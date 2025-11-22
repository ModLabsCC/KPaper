package cc.modlabs.kpaper.party

import io.mockk.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.params.ScanParams
import redis.clients.jedis.resps.ScanResult
import java.util.*
import java.util.concurrent.CompletableFuture

class RedisPartyAPITest {

    private lateinit var jedis: JedisPooled
    private lateinit var api: RedisPartyAPI

    @BeforeEach
    fun setup() {
        jedis = mockk(relaxed = true)
        api = RedisPartyAPI(jedis)
        mockkStatic(Bukkit::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        api.close()
    }

    @Test
    fun `party existence and retrieval`() {
        every { jedis.get("party:alpha") } returns "{\"partyId\":\"alpha\",\"leader\":\"00000000-0000-0000-0000-000000000001\",\"members\":[\"00000000-0000-0000-0000-000000000001\",\"00000000-0000-0000-0000-000000000002\"],\"createdAt\":1,\"maxSize\":5}"

        assertTrue(api.partyExists("alpha").join())

        val pdataOpt = api.getPartyData("alpha").join()
        assertTrue(pdataOpt.isPresent)
        val pdata = pdataOpt.get()
        assertEquals("alpha", pdata.partyId)
        assertEquals(2, pdata.members.size)
        assertEquals(5, pdata.maxSize)
    }

    @Test
    fun `isInParty, same party and leader`() {
        val p1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val p2 = UUID.fromString("00000000-0000-0000-0000-000000000002")
        every { jedis.get("player_party:$p1") } returns "alpha"
        every { jedis.get("player_party:$p2") } returns "alpha"
        every { jedis.get("party:alpha") } returns "{\"partyId\":\"alpha\",\"leader\":\"$p1\",\"members\":[\"$p1\",\"$p2\"],\"createdAt\":1,\"maxSize\":8}"

        assertTrue(api.isInParty(p1).join())
        assertTrue(api.isPartyLeader(p1).join())
        assertTrue(api.areInSameParty(p1, p2).join())

        val leaderOpt = api.getPartyLeader(p2).join()
        assertTrue(leaderOpt.isPresent)
        assertEquals(p1, leaderOpt.get())
    }

    @Test
    fun `online members with Bukkit lookup`() {
        val p1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val p2 = UUID.fromString("00000000-0000-0000-0000-000000000002")
        every { jedis.get("party:alpha") } returns "{\"partyId\":\"alpha\",\"leader\":\"$p1\",\"members\":[\"$p1\",\"$p2\"],\"createdAt\":1,\"maxSize\":8}"

        val mockPlayer: Player = mockk(relaxed = true)
        every { Bukkit.getPlayer(p1) } returns mockPlayer
        every { Bukkit.getPlayer(p2) } returns null

        val set = api.getOnlinePartyMembers("alpha").join()
        assertEquals(setOf(p1), set)
        assertEquals(1, api.getOnlinePartyMemberCount("alpha").join())
    }

    @Test
    fun `invites list and retrieval via scan and get`() {
        val invited = UUID.fromString("00000000-0000-0000-0000-000000000010")
        val invKey = "party_invite:alpha:$invited"
        val now = System.currentTimeMillis()
        val json = "{\"partyId\":\"alpha\",\"invitedPlayer\":\"$invited\",\"inviter\":\"00000000-0000-0000-0000-000000000001\",\"createdAt\":$now,\"expiresAt\":${now + 10000}}"

        // getInvite
        every { jedis.get(invKey) } returns json
        val inv = api.getInvite(invited, "alpha").join()
        assertTrue(inv.isPresent)
        assertEquals("alpha", inv.get().partyId)

        // hasInvite
        assertTrue(api.hasInvite(invited, "alpha").join())

        // Scan for all invites
        val paramsSlot = slot<ScanParams>()
        every { jedis.scan(any<String>(), capture(paramsSlot)) } answers {
            val cursor = firstArg<String>()
            // Return single page then finish
            if (cursor == ScanParams.SCAN_POINTER_START) {
                ScanResult<String>("0", listOf(invKey))
            } else ScanResult<String>(ScanParams.SCAN_POINTER_START, emptyList())
        }
        every { jedis.get(invKey) } returns json

        val all = api.getAllInvites(invited).join()
        assertEquals(setOf("alpha"), all)
    }
}
