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
class DefaultPartyAPI(
    private val onlinePlayerLookup: OnlinePlayerLookup = OnlinePlayerLookup { Bukkit.getPlayer(it) },
) : PartyAPI {

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
    private val mutationLock = Any()

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
        CompletableFuture.completedFuture(activeInvite(InviteKey(partyId, playerId)) != null)

    override fun getInvite(playerId: UUID, partyId: String): CompletableFuture<Optional<PartyAPI.PartyInvite>> =
        CompletableFuture.completedFuture(
            Optional.ofNullable(activeInvite(InviteKey(partyId, playerId)))
        )

    // --------------- Invites -----------------

    override fun getAllInvites(playerId: UUID): CompletableFuture<Set<String>> =
        CompletableFuture.completedFuture(
            run {
                invites.entries.removeIf { it.value.isExpired() }
                invites.keys.filter { it.playerId == playerId }.map { it.partyId }.toSet()
            }
        )

    override fun partyExists(partyId: String): CompletableFuture<Boolean> =
        CompletableFuture.completedFuture(parties.containsKey(partyId))

    override fun getOnlinePartyMembers(partyId: String): CompletableFuture<Set<UUID>> =
        CompletableFuture.completedFuture(
            parties[partyId]?.members?.filter { onlinePlayerLookup(it) != null }?.toSet() ?: emptySet()
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
        parties[partyId]?.members?.count { onlinePlayerLookup(it) != null } ?: 0

    private fun activeInvite(key: InviteKey): PartyAPI.PartyInvite? {
        val invite = invites[key] ?: return null
        if (invite.isExpired()) {
            invites.remove(key, invite)
            return null
        }
        return invite
    }

    // --------------- Minimal management API (internal) -----------------
    // Not part of PartyAPI (which is read-only), but useful for tests/examples

    fun createParty(id: String, leader: UUID, maxSize: Int = 8): PartyAPI.PartyData {
        require(id.isNotBlank()) { "Party id cannot be blank" }
        require(maxSize > 0) { "Party maxSize must be positive" }
        return synchronized(mutationLock) {
            parties.remove(id)?.members?.forEach { member -> playerToParty.remove(member, id) }
            playerToParty[leader]?.let { previous -> removeMemberLocked(leader, previous) }
            val p = InternalParty(id, leader, CopyOnWriteArraySet<UUID>().apply { add(leader) }, System.currentTimeMillis(), maxSize)
            parties[id] = p
            playerToParty[leader] = id
            p.toApi()
        }
    }

    fun addMember(partyId: String, playerId: UUID): Boolean {
        return synchronized(mutationLock) {
            val p = parties[partyId] ?: return@synchronized false
            val currentParty = playerToParty[playerId]
            if (currentParty != null) return@synchronized currentParty == partyId
            if (p.members.size >= p.maxSize) return@synchronized false
            if (!p.members.add(playerId)) return@synchronized false
            playerToParty[playerId] = partyId
            true
        }
    }

    fun removeMember(playerId: UUID) {
        synchronized(mutationLock) {
            val pid = playerToParty[playerId] ?: return
            removeMemberLocked(playerId, pid)
        }
    }

    private fun removeMemberLocked(playerId: UUID, pid: String) {
        playerToParty.remove(playerId, pid)
        val p = parties[pid] ?: return
        p.members.remove(playerId)
        if (playerId == p.leader) {
            // Promote someone else or disband
            val newLeader = p.members.firstOrNull()
            if (newLeader == null) {
                parties.remove(pid)
                p.members.forEach { member -> playerToParty.remove(member, pid) }
                invites.keys.removeIf { it.partyId == pid }
            } else {
                p.leader = newLeader
            }
        }
    }

    fun invite(playerId: UUID, partyId: String, inviter: UUID, ttl: Duration = Duration.ofSeconds(60)) {
        require(!ttl.isNegative && !ttl.isZero) { "Invite TTL must be positive" }
        val party = parties[partyId] ?: throw IllegalArgumentException("Unknown party: $partyId")
        require(party.members.contains(inviter)) { "Inviter must be a member of the party" }
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
