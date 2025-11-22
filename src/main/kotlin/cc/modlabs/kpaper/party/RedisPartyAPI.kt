package cc.modlabs.kpaper.party

import cc.modlabs.kpaper.inventory.GUI
import cc.modlabs.kpaper.inventory.ItemBuilder
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.params.ScanParams
import redis.clients.jedis.resps.ScanResult
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.Optional
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Redis-backed implementation of [PartyAPI].
 *
 * Key format (ephemeral):
 * - party:{partyId} -> JSON {partyId, leader, members:Set<UUID>, createdAt, maxSize}
 * - player_party:{uuid} -> partyId (String/UUID)
 * - party_invite:{partyId}:{uuid} -> JSON {partyId, invitedPlayer, inviter, createdAt, expiresAt} (TTL: ~60s)
 *
 * This implementation performs read-only operations using Redis.
 * Note: [openPartyGUI] does synchronous reads from Redis to build the inventory.
 */
class RedisPartyAPI(
    private val jedis: JedisPooled
) : PartyAPI, AutoCloseable {

    constructor(redisUri: String) : this(JedisPooled(redisUri))

    private val gson = Gson()

    // --------------- Party checks -----------------

    override fun isInParty(playerId: UUID): CompletableFuture<Boolean> =
        CompletableFuture.supplyAsync { jedis.get(playerPartyKey(playerId)) != null }

    override fun isInParty(player: OfflinePlayer): CompletableFuture<Boolean> =
        isInParty(player.uniqueId)

    override fun isPartyLeader(playerId: UUID): CompletableFuture<Boolean> =
        CompletableFuture.supplyAsync {
            val partyId = jedis.get(playerPartyKey(playerId)) ?: return@supplyAsync false
            val pdata = getPartyDataSync(partyId) ?: return@supplyAsync false
            pdata.leader == playerId
        }

    override fun areInSameParty(player1: UUID, player2: UUID): CompletableFuture<Boolean> =
        CompletableFuture.supplyAsync {
            val p1 = jedis.get(playerPartyKey(player1))
            val p2 = jedis.get(playerPartyKey(player2))
            p1 != null && p1 == p2
        }

    override fun getPartyId(playerId: UUID): CompletableFuture<Optional<String>> =
        CompletableFuture.supplyAsync { Optional.ofNullable(jedis.get(playerPartyKey(playerId))) }

    // --------------- Party data -----------------

    override fun getPartyData(partyId: String): CompletableFuture<Optional<PartyAPI.PartyData>> =
        CompletableFuture.supplyAsync {
            Optional.ofNullable(getPartyDataSync(partyId))
        }

    override fun getPlayerParty(playerId: UUID): CompletableFuture<Optional<PartyAPI.PartyData>> =
        CompletableFuture.supplyAsync {
            val pid = jedis.get(playerPartyKey(playerId)) ?: return@supplyAsync Optional.empty()
            Optional.ofNullable(getPartyDataSync(pid))
        }

    override fun getPartyLeader(playerId: UUID): CompletableFuture<Optional<UUID>> =
        CompletableFuture.supplyAsync {
            val pid = jedis.get(playerPartyKey(playerId)) ?: return@supplyAsync Optional.empty()
            Optional.ofNullable(getPartyDataSync(pid)?.leader)
        }

    override fun getPartyMembers(partyId: String): CompletableFuture<Set<UUID>> =
        CompletableFuture.supplyAsync {
            getPartyDataSync(partyId)?.members ?: emptySet()
        }

    override fun getPartyMembersOfPlayer(playerId: UUID): CompletableFuture<Set<UUID>> =
        CompletableFuture.supplyAsync {
            val pid = jedis.get(playerPartyKey(playerId)) ?: return@supplyAsync emptySet()
            getPartyDataSync(pid)?.members ?: emptySet()
        }

    override fun getPartyMemberCount(partyId: String): CompletableFuture<Int> =
        CompletableFuture.supplyAsync { getPartyDataSync(partyId)?.members?.size ?: 0 }

    override fun isPartyFull(partyId: String): CompletableFuture<Boolean> =
        CompletableFuture.supplyAsync { getPartyDataSync(partyId)?.isFull() ?: false }

    override fun hasInvite(playerId: UUID, partyId: String): CompletableFuture<Boolean> =
        CompletableFuture.supplyAsync {
            val json = jedis.get(inviteKey(partyId, playerId)) ?: return@supplyAsync false
            val inv = parseInvite(json) ?: return@supplyAsync false
            !inv.isExpired()
        }

    override fun getInvite(playerId: UUID, partyId: String): CompletableFuture<Optional<PartyAPI.PartyInvite>> =
        CompletableFuture.supplyAsync {
            val json = jedis.get(inviteKey(partyId, playerId)) ?: return@supplyAsync Optional.empty()
            val inv = parseInvite(json)?.takeUnless { it.isExpired() }
            Optional.ofNullable(inv)
        }

    // --------------- Invites -----------------

    override fun getAllInvites(playerId: UUID): CompletableFuture<Set<String>> =
        CompletableFuture.supplyAsync {
            val pattern = "party_invite:*:${playerId}"
            var cursor = ScanParams.SCAN_POINTER_START
            val params = ScanParams().match(pattern).count(500)
            val out = mutableSetOf<String>()
            do {
                val res: ScanResult<String> = jedis.scan(cursor, params)
                cursor = res.cursor
                for (k in res.result) {
                    val json = jedis.get(k) ?: continue
                    val inv = parseInvite(json) ?: continue
                    if (!inv.isExpired()) out.add(inv.partyId)
                }
            } while (cursor != ScanParams.SCAN_POINTER_START)
            out
        }

    override fun partyExists(partyId: String): CompletableFuture<Boolean> =
        CompletableFuture.supplyAsync { jedis.get(partyKey(partyId)) != null }

    override fun getOnlinePartyMembers(partyId: String): CompletableFuture<Set<UUID>> =
        CompletableFuture.supplyAsync {
            val members = getPartyDataSync(partyId)?.members ?: return@supplyAsync emptySet()
            members.filter { Bukkit.getPlayer(it) != null }.toSet()
        }

    // --------------- Utilities -----------------

    override fun getOnlinePartyMemberCount(partyId: String): CompletableFuture<Int> =
        CompletableFuture.supplyAsync {
            getPartyDataSync(partyId)?.members?.count { Bukkit.getPlayer(it) != null } ?: 0
        }

    override fun openPartyGUI(player: Player) {
        val pid = jedis.get(playerPartyKey(player.uniqueId))
        val pdata = pid?.let { getPartyDataSync(it) }
        val inv = PartyGUI.factory.build(player, pdata)
        player.openInventory(inv)
    }

    // --------------- Internal helpers -----------------

    private fun partyKey(partyId: String) = "party:$partyId"
    private fun playerPartyKey(playerId: UUID) = "player_party:$playerId"
    private fun inviteKey(partyId: String, playerId: UUID) = "party_invite:$partyId:$playerId"

    private fun getPartyDataSync(partyId: String): PartyAPI.PartyData? {
        val json = jedis.get(partyKey(partyId)) ?: return null
        return parseParty(json)
    }

    private fun parseParty(json: String): PartyAPI.PartyData? {
        return try {
            val dto = gson.fromJson(json, PartyJson::class.java)
            PartyAPI.PartyData(
                partyId = dto.partyId,
                leader = UUID.fromString(dto.leader),
                members = dto.members.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }.toSet(),
                createdAt = dto.createdAt,
                maxSize = dto.maxSize
            )
        } catch (e: JsonSyntaxException) {
            // Malformed JSON, ignore
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseInvite(json: String): PartyAPI.PartyInvite? {
        return try {
            val dto = gson.fromJson(json, InviteJson::class.java)
            PartyAPI.PartyInvite(
                partyId = dto.partyId,
                invitedPlayer = UUID.fromString(dto.invitedPlayer),
                inviter = UUID.fromString(dto.inviter),
                createdAt = dto.createdAt,
                expiresAt = dto.expiresAt
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun close() {
        jedis.close()
    }

    private data class PartyJson(
        @SerializedName("partyId") val partyId: String,
        @SerializedName("leader") val leader: String,
        @SerializedName("members") val members: List<String> = emptyList(),
        @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
        @SerializedName("maxSize") val maxSize: Int = 8
    )

    private data class InviteJson(
        @SerializedName("partyId") val partyId: String,
        @SerializedName("invitedPlayer") val invitedPlayer: String,
        @SerializedName("inviter") val inviter: String,
        @SerializedName("createdAt") val createdAt: Long,
        @SerializedName("expiresAt") val expiresAt: Long
    )
}
