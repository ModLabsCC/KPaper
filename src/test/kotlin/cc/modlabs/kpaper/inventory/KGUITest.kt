package cc.modlabs.kpaper.inventory

import io.mockk.mockk
import io.mockk.verify
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.junit.jupiter.api.Test

class KGUITest {

    private class TestGui(
        override val title: String = "Test",
        override val size: Int = 9,
        private val inv: Inventory
    ) : KGUI() {
        override fun build(player: Player): Inventory = inv
    }

    @Test
    fun `open calls player openInventory with built inventory`() {
        val player: Player = mockk(relaxed = true)
        val inventory: Inventory = mockk(relaxed = true)

        val gui = TestGui(inv = inventory)
        gui.open(player)

        verify(exactly = 1) { player.openInventory(inventory) }
    }
}
