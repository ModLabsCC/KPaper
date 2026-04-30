package cc.modlabs.kpaper.inventory

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Test

class InventoryBuilderTest {

    @Test
    fun `build sets provided items into inventory`() {
        val inventory: Inventory = mockk(relaxed = true)
        val title = Component.text("Inv")
        val size = 9

        val stack1: ItemStack = mockk(relaxed = true)
        val stack2: ItemStack = mockk(relaxed = true)

        val ib = InventoryBuilder(size, title, createInventory = { _, _, _ -> inventory })
            .setItem(0, InventoryItem(stack1))
            .setItem(5, stack2)

        val inv = ib.build()

        // Correct inventory returned from Bukkit
        assert(inv === inventory)

        verify(exactly = 1) { inventory.setItem(0, any()) }
        verify(exactly = 1) { inventory.setItem(5, any()) }
        confirmVerified(inventory)
    }

    @Test
    @org.junit.jupiter.api.Disabled("Disabled: calling open() initializes ItemClickListener and the plugin event system; documented behavior only")
    fun `open builds inventory opens for player and registers click mapping`() {
        val inventory: Inventory = mockk(relaxed = true)
        val player: Player = mockk(relaxed = true)
        val title = Component.text("Openable")
        val size = 9

        val stack: ItemStack = mockk(relaxed = true)
        val invItem = InventoryItem(stack)

        val ib = InventoryBuilder(size, title, createInventory = { _, _, _ -> inventory })
            .setItem(3, invItem)

        ib.open(player)

        verify(exactly = 1) { player.openInventory(inventory) }
        // Registration with ItemClickListener would occur here in a real server environment
    }

    @Test
    fun `fill sets every empty slot to provided item`() {
        val inventory: Inventory = mockk(relaxed = true)
        val title = Component.text("Fill")
        val size = 9

        // Inventory initially empty
        every { inventory.getItem(any()) } returns null

        val filler: ItemStack = mockk(relaxed = true)
        val ib = InventoryBuilder(size, title, createInventory = { _, _, _ -> inventory })

        val built = ib.fill(InventoryItem(filler)).build()
        assert(built === inventory)

        // setItem called for all slots
        verify(exactly = size) { inventory.setItem(any(), any()) }
    }
}
