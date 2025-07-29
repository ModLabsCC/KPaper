package cc.modlabs.kpaper.util

const val ANSI_RESET = "\u001B[0m"
const val ANSI_BLACK = "\u001B[30m"
const val ANSI_RED = "\u001B[31m"
const val ANSI_GREEN = "\u001B[32m"
const val ANSI_YELLOW = "\u001B[33m"
const val ANSI_BLUE = "\u001B[34m"
const val ANSI_PURPLE = "\u001B[35m"
const val ANSI_CYAN = "\u001B[36m"
const val ANSI_WHITE = "\u001B[37m"


fun printColor(text: String, color: String) {
    print("$color$text$ANSI_RESET")
}

fun printlnColor(text: String, color: String) {
    println("$color$text$ANSI_RESET")
}

// Quick shortcuts
fun printlnRed(text: String) = printlnColor(text, ANSI_RED)
fun printlnGreen(text: String) = printlnColor(text, ANSI_GREEN)
fun printlnYellow(text: String) = printlnColor(text, ANSI_YELLOW)
fun printlnBlue(text: String) = printlnColor(text, ANSI_BLUE)
fun printlnCyan(text: String) = printlnColor(text, ANSI_CYAN)
fun printlnPurple(text: String) = printlnColor(text, ANSI_PURPLE)


// --- SECTION TITLES AND DIVIDERS ---

fun sectionTitle(title: String, color: String = ANSI_CYAN) {
    val line = "═".repeat(title.length + 4)
    printlnColor("╔$line╗", color)
    printlnColor("║  $title  ║", color)
    printlnColor("╚$line╝", color)
}

fun divider(length: Int = 50, color: String = ANSI_WHITE) {
    printlnColor("─".repeat(length), color)
}


// --- PROGRESS BAR ---

fun progressBar(progress: Double, length: Int = 30, color: String = ANSI_GREEN) {
    require(progress in 0.0..1.0) { "Progress must be between 0.0 and 1.0" }
    val filled = (progress * length).toInt()
    val bar = "█".repeat(filled) + "░".repeat(length - filled)
    printColor("\r[$bar] ${(progress * 100).toInt()}%", color)
    if (progress >= 1.0) println()
}


// --- USER INPUT (with prompt color) ---

fun readLinePrompt(prompt: String, color: String = ANSI_YELLOW): String? {
    printColor(prompt, color)
    return readLine()
}