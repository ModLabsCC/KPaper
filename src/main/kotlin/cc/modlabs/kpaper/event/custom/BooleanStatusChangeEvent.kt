package cc.modlabs.kpaper.event.custom

import cc.modlabs.kpaper.event.KEvent

abstract class BooleanStatusChangeEvent(var newValue: Boolean, isAsync: Boolean = false) : KEvent(isAsync)