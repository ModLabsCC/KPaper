package cc.modlabs.kpaper.party

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

class DefaultPartyAPITest {

    private lateinit var api: DefaultPartyAPI

    @BeforeEach
    fun setup() {
        api = DefaultPartyAPI()
        mockkStatic(Bukkit::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `create party and basic membership queries`() {
        val leader = UUID.randomUUID()
        val member = UUID.randomUUID()

        val party = api.createParty("p1", leader, maxSize = 2)
        assertEquals("p1", party.partyId)
        assertTrue(api.isInParty(leader).join())
        assertTrue(api.isPartyLeader(leader).join())

        assertTrue(api.addMember("p1", member))
        assertTrue(api.isInParty(member).join())
        assertTrue(api.areInSameParty(leader, member).join())

        val mid = api.getPartyId(member).join()
        assertTrue(mid.isPresent)
        assertEquals("p1", mid.get())

        val pdata = api.getPlayerParty(member).join()
        assertTrue(pdata.isPresent)
        assertEquals(2, pdata.get().members.size)
    }

    @Test
    fun `party full check prevents adding more members`() {
        val leader = UUID.randomUUID()
        val m1 = UUID.randomUUID()
        val m2 = UUID.randomUUID()
        api.createParty("full", leader, maxSize = 2)
        assertTrue(api.addMember("full", m1))
        assertFalse(api.addMember("full", m2), "Should not add beyond max size")
        assertTrue(api.isPartyFull("full").join())
        assertEquals(2, api.getPartyMemberCount("full").join())
    }

    @Test
    fun `remove leader promotes or disbands`() {
        val leader = UUID.randomUUID()
        val m1 = UUID.randomUUID()
        val p = api.createParty("pp", leader, maxSize = 5)
        api.addMember("pp", m1)

        // Remove current leader -> m1 should become leader
        api.removeMember(leader)
        val newLeader = api.getPartyLeader(m1).join()
        assertTrue(newLeader.isPresent)
        assertEquals(m1, newLeader.get())

        // Now remove the last member -> party should disband
        api.removeMember(m1)
        assertFalse(api.partyExists("pp").join())
    }

    @Test
    fun `invites can be created, fetched and expire`() {
        val leader = UUID.randomUUID()
        val invited = UUID.randomUUID()
        api.createParty("i1", leader)
        api.invite(invited, "i1", inviter = leader, ttl = Duration.ofMillis(50))

        assertTrue(api.hasInvite(invited, "i1").join())
        val inv = api.getInvite(invited, "i1").join()
        assertTrue(inv.isPresent)
        assertEquals("i1", inv.get().partyId)

        // Wait for expiration
        Thread.sleep(60)
        assertFalse(api.hasInvite(invited, "i1").join())
        val inv2 = api.getInvite(invited, "i1").join()
        assertFalse(inv2.isPresent)
    }

    @Test
    fun `online members and counts query Bukkit`() {
        val leader = UUID.randomUUID()
        val onlineMember = UUID.randomUUID()
        val offlineMember = UUID.randomUUID()
        api.createParty("o1", leader, maxSize = 5)
        api.addMember("o1", onlineMember)
        api.addMember("o1", offlineMember)

        // Mock Bukkit.getPlayer: return non-null for onlineMember only
        val mockPlayer: Player = mockk(relaxed = true)
        every { Bukkit.getPlayer(onlineMember) } returns mockPlayer
        every { Bukkit.getPlayer(leader) } returns mockPlayer
        every { Bukkit.getPlayer(offlineMember) } returns null

        val online = api.getOnlinePartyMembers("o1").join()
        assertTrue(leader in online)
        assertTrue(onlineMember in online)
        assertFalse(offlineMember in online)
        assertEquals(2, api.getOnlinePartyMemberCount("o1").join())
    }
}
