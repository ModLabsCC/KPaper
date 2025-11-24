package cc.modlabs.kpaper.visuals.display

/**
 * Flags that can be applied to a Text Display entity.
 * These flags control various display behaviors such as shadow, transparency, background, and text alignment.
 */
enum class TextDisplayFlags(val bitValue: Int) {
    /**
     * Shows the text with a shadow effect.
     */
    HAS_SHADOW(0x01),

    /**
     * Makes the text see-through (transparent).
     */
    IS_SEE_THROUGH(0x02),

    /**
     * Uses the default background color instead of a custom one.
     */
    USE_DEFAULT_BACKGROUND_COLOR(0x04),

    /**
     * Centers the text alignment.
     */
    ALIGNMENT_CENTER(0x00),

    /**
     * Aligns the text to the left.
     */
    ALIGNMENT_LEFT(0x08),

    /**
     * Aligns the text to the right.
     */
    ALIGNMENT_RIGHT(0x10);

    companion object {
        /**
         * Calculates a bitmask from a list of flags.
         * Only one alignment flag can be used at a time.
         *
         * @param flags The list of flags to combine
         * @return The combined bitmask value
         * @throws IllegalArgumentException if multiple alignment flags are provided
         */
        fun calculateBitMask(flags: List<TextDisplayFlags>): Int {
            var mask = 0

            // Only one alignment may be selected
            val alignmentFlags = flags.filter {
                it == ALIGNMENT_CENTER || it == ALIGNMENT_LEFT || it == ALIGNMENT_RIGHT
            }

            if (alignmentFlags.size > 1) {
                throw IllegalArgumentException("Nur eine Ausrichtung kann verwendet werden")
            }

            // Add all flags to the mask
            for (flag in flags) {
                mask = mask or flag.bitValue
            }

            return mask
        }

        /**
         * Creates a flag list from a bitmask.
         *
         * @param mask The bitmask to decode
         * @return A list of flags represented by the bitmask
         */
        fun fromBitMask(mask: Int): List<TextDisplayFlags> {
            val result = mutableListOf<TextDisplayFlags>()

            // Check other flags
            if (mask and HAS_SHADOW.bitValue != 0) result.add(HAS_SHADOW)
            if (mask and IS_SEE_THROUGH.bitValue != 0) result.add(IS_SEE_THROUGH)
            if (mask and USE_DEFAULT_BACKGROUND_COLOR.bitValue != 0) result.add(USE_DEFAULT_BACKGROUND_COLOR)

            // Determine the alignment
            when {
                mask and ALIGNMENT_LEFT.bitValue != 0 -> result.add(ALIGNMENT_LEFT)
                mask and ALIGNMENT_RIGHT.bitValue != 0 -> result.add(ALIGNMENT_RIGHT)
                else -> result.add(ALIGNMENT_CENTER) // Default is CENTER
            }

            return result
        }
    }
}

