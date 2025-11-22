package cc.modlabs.kpaper.party

import cc.modlabs.kpaper.inventory.GUI
import cc.modlabs.kpaper.inventory.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

/**
 * Pluggable factory to build the Party GUI inventory.
 *
 * Library users can replace [PartyGUI.factory] with their own implementation
 * to fully control layout, texts and styling.
 */
interface PartyGUIFactory {
    /**
     * Build an inventory for the given player. If [party] is null, the player is not in a party.
     */
    fun build(player: Player, party: PartyAPI.PartyData?): Inventory
}

/**
 * Entry point for Party GUI customization.
 * Assign your own [factory] implementation at runtime:
 *
 * PartyGUI.factory = MyPartyGUI()
 */
object PartyGUI {
    @Volatile
    var factory: PartyGUIFactory = DefaultPartyGUIFactory()
}

/**
 * Default GUI implementation used by KPaper out of the box.
 * Mirrors the previous built-in layout and texts.
 */
class DefaultPartyGUIFactory : PartyGUIFactory {
    override fun build(player: Player, party: PartyAPI.PartyData?): Inventory {
        val title = when (party) {
            null -> "No Party"
            else -> "Party ${party.partyId}"
        }
        return GUI(title, 27) {
            if (party == null) {
                val item = ItemBuilder(Material.BARRIER) {
                    display("No Party")
                    lore("You are currently not in a party.")
                }.build()
                item(13, item)
            } else {
                val leaderName = Bukkit.getOfflinePlayer(party.leader).name ?: party.leader.toString()
                val leaderItem = ItemBuilder(Material.NETHER_STAR) {
                    display("Leader: $leaderName")
                    lore("Members: ${party.members.size}/${party.maxSize}")
                }.build()
                item(10, leaderItem)

                var slot = 12
                party.members.sortedBy { if (it == party.leader) 0 else 1 }.forEach { uuid ->
                    val name = Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString()
                    val online = Bukkit.getPlayer(uuid) != null
                    val it = ItemBuilder(if (online) Material.PLAYER_HEAD else Material.SKELETON_SKULL) {
                        display(name + if (uuid == party.leader) " §7(Leader)" else "")
                        lore(
                            if (online) "§aOnline" else "§cOffline"
                        )
                    }.build()
                    if (slot < 27) item(slot++, it)
                }
            }
        }
    }
}
