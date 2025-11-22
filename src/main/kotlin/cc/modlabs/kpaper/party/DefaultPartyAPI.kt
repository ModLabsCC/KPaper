package cc.modlabs.kpaper.party

import cc.modlabs.kpaper.inventory.GUI
import cc.modlabs.kpaper.inventory.ItemBuilder
import dev.fruxz.stacked.text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.time.Duration
import java.util.Optional
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * A simple in-memory implementation of [PartyAPI].
 *
 * Note: This implementation is NOT persistent and serves as a default/fallback.
 * Projects can provide their own implementation (e.g. Redis) via [Party.api].
 */
class DefaultPartyAPI : PartyAPI {

    private data class InternalParty(
        val id: String,
        @Volatile var leader: UUID,
        val members: MutableSet<UUID> = CopyOnWriteArraySet(),
        val createdAt: Long = System.currentTimeMillis(),
        @Volatile var maxSize: Int = 8
    )

    private data class InviteKey(val partyId: String, val playerId: UUID)

    private val parties = ConcurrentHashMap<String, InternalParty>()
    private val playerToParty = ConcurrentHashMap<UUID, String>()
    private val invites = ConcurrentHashMap<InviteKey, PartyAPI.PartyInvite>()

    // --------------- Party checks -----------------

    override fun isInParty(playerId: UUID): CompletableFuture<Boolean> =
        CompletableFuture.completedFuture(playerToParty.containsKey(playerId))

    override fun isInParty(player: OfflinePlayer): CompletableFuture<Boolean> =
        isInParty(player.uniqueId)

    override fun isPartyLeader(playerId: UUID): CompletableFuture<Boolean> =
        CompletableFuture.completedFuture(
            playerToParty[playerId]?.let { parties[it]?.leader == playerId } ?: false
        )

    override fun areInSameParty(player1: UUID, player2: UUID): CompletableFuture<Boolean> =
        CompletableFuture.completedFuture(playerToParty[player1] != null && playerToParty[player1] == playerToParty[player2])

    override fun getPartyId(playerId: UUID): CompletableFuture<Optional<String>> =
        CompletableFuture.completedFuture(Optional.ofNullable(playerToParty[playerId]))

    // --------------- Party data -----------------

    override fun getPartyData(partyId: String): CompletableFuture<Optional<PartyAPI.PartyData>> =
        CompletableFuture.completedFuture(Optional.ofNullable(parties[partyId]?.toApi()))

    override fun getPlayerParty(playerId: UUID): CompletableFuture<Optional<PartyAPI.PartyData>> =
        CompletableFuture.completedFuture(
            Optional.ofNullable(playerToParty[playerId]?.let { parties[it]?.toApi() })
        )

    override fun getPartyLeader(playerId: UUID): CompletableFuture<Optional<UUID>> =
        CompletableFuture.completedFuture(
            Optional.ofNullable(playerToParty[playerId]?.let { parties[it]?.leader })
        )

    override fun getPartyMembers(partyId: String): CompletableFuture<Set<UUID>> =
        CompletableFuture.completedFuture(parties[partyId]?.members?.toSet() ?: emptySet())

    override fun getPartyMembersOfPlayer(playerId: UUID): CompletableFuture<Set<UUID>> =
        CompletableFuture.completedFuture(
            playerToParty[playerId]?.let { parties[it]?.members?.toSet() } ?: emptySet()
        )

    override fun getPartyMemberCount(partyId: String): CompletableFuture<Int> =
        CompletableFuture.completedFuture(parties[partyId]?.members?.size ?: 0)

    override fun isPartyFull(partyId: String): CompletableFuture<Boolean> =
        CompletableFuture.completedFuture(parties[partyId]?.let { it.members.size >= it.maxSize } ?: false)

    override fun hasInvite(playerId: UUID, partyId: String): CompletableFuture<Boolean> =
        CompletableFuture.completedFuture(invites[InviteKey(partyId, playerId)]?.let { !it.isExpired() } ?: false)

    override fun getInvite(playerId: UUID, partyId: String): CompletableFuture<Optional<PartyAPI.PartyInvite>> =
        CompletableFuture.completedFuture(
            Optional.ofNullable(invites[InviteKey(partyId, playerId)]?.takeUnless { it.isExpired() })
        )

    // --------------- Invites -----------------

    override fun getAllInvites(playerId: UUID): CompletableFuture<Set<String>> =
        CompletableFuture.completedFuture(
            invites.filter { it.key.playerId == playerId && !it.value.isExpired() }.keys.map { it.partyId }.toSet()
        )

    override fun partyExists(partyId: String): CompletableFuture<Boolean> =
        CompletableFuture.completedFuture(parties.containsKey(partyId))

    override fun getOnlinePartyMembers(partyId: String): CompletableFuture<Set<UUID>> =
        CompletableFuture.completedFuture(
            parties[partyId]?.members?.filter { Bukkit.getPlayer(it) != null }?.toSet() ?: emptySet()
        )

    // --------------- Utilities -----------------

    override fun getOnlinePartyMemberCount(partyId: String): CompletableFuture<Int> =
        CompletableFuture.completedFuture(getOnlineCount(partyId))

    override fun openPartyGUI(player: Player) {
        val partyId = playerToParty[player.uniqueId]
        val party = partyId?.let { parties[it]?.toApi() }
        val inv = PartyGUI.factory.build(player, party)
        player.openInventory(inv)
    }

    // --------------- Internal helpers -----------------

    private fun InternalParty.toApi(): PartyAPI.PartyData = PartyAPI.PartyData(
        partyId = id,
        leader = leader,
        members = members.toSet(),
        createdAt = createdAt,
        maxSize = maxSize
    )

    private fun getOnlineCount(partyId: String): Int =
        parties[partyId]?.members?.count { Bukkit.getPlayer(it) != null } ?: 0

    // --------------- Minimal management API (internal) -----------------
    // Not part of PartyAPI (which is read-only), but useful for tests/examples

    fun createParty(id: String, leader: UUID, maxSize: Int = 8): PartyAPI.PartyData {
        val p = InternalParty(id, leader, mutableSetOf<UUID>().apply { add(leader) }, System.currentTimeMillis(), maxSize)
        parties[id] = p
        playerToParty[leader] = id
        return p.toApi()
    }

    fun addMember(partyId: String, playerId: UUID): Boolean {
        val p = parties[partyId] ?: return false
        if (p.members.size >= p.maxSize) return false
        p.members.add(playerId)
        playerToParty[playerId] = partyId
        return true
    }

    fun removeMember(playerId: UUID) {
        val pid = playerToParty.remove(playerId) ?: return
        val p = parties[pid] ?: return
        p.members.remove(playerId)
        if (playerId == p.leader) {
            // Promote someone else or disband
            val newLeader = p.members.firstOrNull()
            if (newLeader == null) {
                parties.remove(pid)
            } else {
                p.leader = newLeader
            }
        }
    }

    fun invite(playerId: UUID, partyId: String, inviter: UUID, ttl: Duration = Duration.ofSeconds(60)) {
        val invite = PartyAPI.PartyInvite(
            partyId = partyId,
            invitedPlayer = playerId,
            inviter = inviter,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + ttl.toMillis()
        )
        invites[InviteKey(partyId, playerId)] = invite
    }
}
