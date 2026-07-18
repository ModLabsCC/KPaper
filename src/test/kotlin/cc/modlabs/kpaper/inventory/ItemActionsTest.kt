package cc.modlabs.kpaper.inventory

import io.mockk.every
import io.mockk.mockk
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ItemActionsTest {
    @Test
    fun `dispatch ignores items without metadata`() {
        val item = mockk<ItemStack>()
        every { item.itemMeta } returns null

        assertFalse(ItemActions.dispatch(item, mockk<InventoryClickEvent>()))
    }
}
