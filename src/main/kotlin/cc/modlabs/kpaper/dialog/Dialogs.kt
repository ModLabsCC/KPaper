package cc.modlabs.kpaper.dialog

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/** Creates a simple native client notice dialog. */
fun noticeDialog(
    title: Component,
    configure: DialogBase.Builder.() -> Unit = {},
): Dialog {
    val base = DialogBase.builder(title).apply(configure).build()
    return Dialog.create { factory ->
        factory.empty().base(base).type(DialogType.notice())
    }
}

fun Player.openDialog(dialog: Dialog) {
    showDialog(dialog)
}

fun Player.dismissDialog() {
    closeDialog()
}
