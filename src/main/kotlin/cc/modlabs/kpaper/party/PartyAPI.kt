package cc.modlabs.kpaper.party

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.Optional
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Party API for external plugins.
 * Provides read-only access to the party system.
 *
 * Note: In KPaper this is an in-memory implementation by default.
 * Projects can register their own implementation (e.g. Redis) via [Party.api].
 */
interface PartyAPI {

    // ==================== Party checks ====================

    /**
     * Checks whether a player is in a party.
     */
    fun isInParty(playerId: UUID): CompletableFuture<Boolean>

    /**
     * Checks whether a player is in a party.
     */
    fun isInParty(player: OfflinePlayer): CompletableFuture<Boolean> = isInParty(player.uniqueId)

    /**
     * Checks whether a player is the party leader.
     */
    fun isPartyLeader(playerId: UUID): CompletableFuture<Boolean>

    /**
     * Checks whether two players are in the same party.
     */
    fun areInSameParty(player1: UUID, player2: UUID): CompletableFuture<Boolean>

    /**
     * Gets the party ID of a player.
     */
    fun getPartyId(playerId: UUID): CompletableFuture<Optional<String>>

    /**
     * Gets the party ID of a player.
     */
    fun getPartyId(player: OfflinePlayer): CompletableFuture<Optional<String>> = getPartyId(player.uniqueId)

    // ==================== Party data ====================

    /**
     * Gets the full party data by party ID.
     */
    fun getPartyData(partyId: String): CompletableFuture<Optional<PartyData>>

    /**
     * Gets the party data of a player.
     */
    fun getPlayerParty(playerId: UUID): CompletableFuture<Optional<PartyData>>

    /**
     * Gets the party data of a player.
     */
    fun getPlayerParty(player: OfflinePlayer): CompletableFuture<Optional<PartyData>> = getPlayerParty(player.uniqueId)

    /**
     * Gets the party leader of a player’s party.
     */
    fun getPartyLeader(playerId: UUID): CompletableFuture<Optional<UUID>>

    /**
     * Gets all members of a party.
     */
    fun getPartyMembers(partyId: String): CompletableFuture<Set<UUID>>

    /**
     * Gets all members of the party a player is in.
     */
    fun getPartyMembersOfPlayer(playerId: UUID): CompletableFuture<Set<UUID>>

    /**
     * Counts the number of members in a party.
     */
    fun getPartyMemberCount(partyId: String): CompletableFuture<Int>

    /**
     * Checks whether a party is full.
     */
    fun isPartyFull(partyId: String): CompletableFuture<Boolean>

    /**
     * Checks whether a player has a party invite.
     */
    fun hasInvite(playerId: UUID, partyId: String): CompletableFuture<Boolean>

    /**
     * Gets a party invite.
     */
    fun getInvite(playerId: UUID, partyId: String): CompletableFuture<Optional<PartyInvite>>

    // ==================== Party invites ====================

    /**
     * Gets all open party invites of a player.
     */
    fun getAllInvites(playerId: UUID): CompletableFuture<Set<String>>

    /**
     * Checks whether a party exists.
     */
    fun partyExists(partyId: String): CompletableFuture<Boolean>

    /**
     * Gets all online members of a party.
     */
    fun getOnlinePartyMembers(partyId: String): CompletableFuture<Set<UUID>>

    // ==================== Party utilities ====================

    /**
     * Counts the number of online members in a party.
     */
    fun getOnlinePartyMemberCount(partyId: String): CompletableFuture<Int>

    /**
     * Opens the party GUI for a player. Only works if the player is online.
     */
    fun openPartyGUI(player: Player)

    // ==================== Data containers ====================

    data class PartyData(
        val partyId: String,
        val leader: UUID,
        val members: Set<UUID>,
        val createdAt: Long,
        val maxSize: Int
    ) {
        fun isMember(playerId: UUID): Boolean = members.contains(playerId)
        fun isLeader(playerId: UUID): Boolean = leader == playerId
        fun getMemberCount(): Int = members.size
        fun isFull(): Boolean = members.size >= maxSize
    }

    data class PartyInvite(
        val partyId: String,
        val invitedPlayer: UUID,
        val inviter: UUID,
        val createdAt: Long,
        val expiresAt: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    }
}
