package cc.modlabs.kpaper.command

import cc.modlabs.kpaper.main.KPlugin
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents

fun KPlugin.registerCommand(command: CommandBuilder) {
    lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
        event.registrar().register(command.register(), command.description, command.aliases)
    }
}

fun KPlugin.registerCommands(vararg commands: CommandBuilder) {
    commands.forEach(::registerCommand)
}
