package cc.modlabs.kpaper.skins

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

internal fun canonicalSkinId(input: String, aliases: Map<String, String>): String {
    var current = input.trim().lowercase(Locale.ROOT)
    val visited = hashSetOf<String>()
    while (visited.add(current)) current = aliases[current] ?: return current
    return current
}

internal fun validatedSkinDisplayName(input: String): String {
    val clean = input.trim().replace(Regex("\\s+"), " ")
    require(clean.length in 1..48) { "Der Skin-Name muss 1 bis 48 Zeichen lang sein." }
    require(clean.none(Char::isISOControl) && '<' !in clean && '>' !in clean) {
        "Der Skin-Name enthält unzulässige Zeichen."
    }
    return clean
}

internal fun validatedMineSkinId(input: String): String {
    val clean = input.trim()
    require(clean.matches(Regex("[a-zA-Z0-9_-]{1,128}"))) { "Ungültige MineSkin-ID." }
    return clean
}

internal fun validatedPlayerName(input: String): String {
    val clean = input.trim()
    require(clean.matches(Regex("[A-Za-z0-9_]{1,16}"))) { "Ungültiger Spielername." }
    return clean
}

internal fun validatedSkinId(input: String): String {
    val clean = input.trim().lowercase(Locale.ROOT)
    require(clean.matches(Regex("[a-z0-9_-]{1,128}"))) { "Die Skin-ID darf nur a-z, 0-9, _ und - enthalten." }
    return clean
}

internal fun skinId(input: String): String {
    val ascii = Normalizer.normalize(input, Normalizer.Form.NFKD).replace(Regex("\\p{M}+"), "")
    val id = ascii.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .take(48)
        .trimEnd('_')
    require(id.isNotBlank()) { "Aus diesem Namen kann keine Skin-ID erzeugt werden." }
    return id
}

internal fun legacySkinId(input: String): String {
    val clean = input.trim().lowercase(Locale.ROOT)
    return clean.takeIf { it.matches(Regex("[a-z0-9_-]{1,128}")) } ?: skinId(clean)
}

internal fun humanizeSkinId(id: String) = id.removePrefix("mineskin_")
    .split('_', '-')
    .filter(String::isNotBlank)
    .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

internal fun sha256(value: String) = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(StandardCharsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
