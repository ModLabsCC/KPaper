package cc.modlabs.kpaper.party

import io.mockk.*
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import redis.clients.jedis.JedisPooled
import java.util.*

class PartyGUITest {

    private lateinit var prevFactory: PartyGUIFactory

    @BeforeEach
    fun setUp() {
        prevFactory = PartyGUI.factory
    }

    @AfterEach
    fun tearDown() {
        PartyGUI.factory = prevFactory
        unmockkAll()
    }

    @Test
    fun `DefaultPartyAPI openPartyGUI calls Player openInventory`() {
        val api = DefaultPartyAPI()
        val player: Player = mockk(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()

        // Provide test factory returning a mocked inventory
        val inv: Inventory = mockk(relaxed = true)
        PartyGUI.factory = object : PartyGUIFactory {
            override fun build(player: Player, party: PartyAPI.PartyData?): Inventory {
                return inv
            }
        }

        api.openPartyGUI(player)
        verify(exactly = 1) { player.openInventory(inv) }
    }

    @Test
    fun `RedisPartyAPI openPartyGUI uses Redis to build and opens`() {
        val jedis: JedisPooled = mockk(relaxed = true)
        val api = RedisPartyAPI(jedis)
        val player: Player = mockk(relaxed = true)

        val pid = "pid-123"
        val leader = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val member = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val partyJson = "{" +
                "\"partyId\":\"$pid\",\"leader\":\"$leader\"," +
                "\"members\":[\"$leader\",\"$member\"],\"createdAt\":1,\"maxSize\":5}"

        val playerId = UUID.randomUUID()
        every { player.uniqueId } returns playerId
        every { jedis.get("player_party:$playerId") } returns pid
        every { jedis.get("party:$pid") } returns partyJson

        val inv: Inventory = mockk(relaxed = true)
        // Capture that party data is passed (not null)
        PartyGUI.factory = object : PartyGUIFactory {
            override fun build(player: Player, party: PartyAPI.PartyData?): Inventory {
                assertNotNull(party)
                return inv
            }
        }

        api.openPartyGUI(player)
        verify(exactly = 1) { player.openInventory(inv) }
        api.close()
    }
}
