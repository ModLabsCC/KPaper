package cc.modlabs.kpaper.messages

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.ComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

var adventureSerializer = LegacyComponentSerializer
	.builder().extractUrls().hexColors().build()

var plainAdventureSerializer: ComponentSerializer<Component, TextComponent, String> =
	PlainTextComponentSerializer.plainText()

var miniMessageSerializer: MiniMessage =
	MiniMessage.miniMessage()

var strictMiniMessageSerializer: MiniMessage =
	MiniMessage.builder().strict(true).build()

val ComponentLike.asString: String
	get() = adventureSerializer.serialize(asComponent())

val ComponentLike.asPlainString: String
	get() = plainAdventureSerializer.serialize(asComponent())

val String.asComponent: TextComponent
	get() = adventureSerializer.deserializeOr(this, Component.text("FAILED", NamedTextColor.RED))!!


val String.asComponents: List<TextComponent>
	get() = this.lines().asComponents

val Iterable<String>.asComponents: List<TextComponent>
	get() = map { it.asComponent }

val ComponentLike.asStyledString: String
	get() = strictMiniMessageSerializer.serialize(asComponent())

fun ComponentLike.asStyledString(
    serializer: MiniMessage = strictMiniMessageSerializer,
) = serializer.serialize(asComponent())


val String.asStyledComponent: TextComponent
	get() = Component.text().append(miniMessageSerializer.deserializeOr(this, Component.empty())!!).build()


fun String.asStyledComponent(
    serializer: MiniMessage = miniMessageSerializer,
    tagResolver: TagResolver = TagResolver.standard(),
) = Component.text().append(serializer.deserialize(this, tagResolver)).build()

fun text(
	content: String,
	serializer: MiniMessage = miniMessageSerializer,
	tagResolver: TagResolver = TagResolver.standard(),
) = content.asStyledComponent(serializer = serializer, tagResolver = tagResolver)
