package cc.modlabs.kpaper.npc

import cc.modlabs.kpaper.event.listen
import dev.fruxz.stacked.text
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentHashMap
import io.papermc.paper.event.player.AsyncChatEvent

/**
 * Represents a conversation that an NPC can have with a player.
 * Uses Paper's AsyncChatEvent for handling player input instead of the deprecated Conversation API.
 */
class NPCConversation(
    private val npc: NPC,
    private val player: Player
) {
    private val prompts = mutableListOf<ConversationPrompt>()
    private val inputs = mutableMapOf<String, String>()
    private var onComplete: ((NPC, Player, Map<String, String>) -> Unit)? = null
    private var onCancel: ((NPC, Player) -> Unit)? = null
    private var escapeSequence: String = "cancel"
    private var timeout: Int = 30 // seconds
    private var timeoutTask: BukkitTask? = null
    private var currentPromptIndex: Int = 0
    private var isActive: Boolean = false

    /**
     * Represents a single prompt in the conversation.
     */
    data class ConversationPrompt(
        val id: String,
        val message: Component,
        val validator: ((String) -> Boolean)? = null,
        val onInput: ((NPC, Player, String) -> Unit)? = null
    )

    /**
     * DSL builder for creating conversations.
     */
    class ConversationBuilder(
        private val conversation: NPCConversation
    ) {
        /**
         * Adds a prompt to the conversation.
         *
         * @param id Unique identifier for this prompt.
         * @param message The message to display to the player.
         * @param validator Optional validator function. Returns true if input is valid.
         * @param onInput Optional callback when player provides input.
         */
        fun prompt(
            id: String,
            message: Component,
            validator: ((String) -> Boolean)? = null,
            onInput: ((NPC, Player, String) -> Unit)? = null
        ) {
            conversation.prompts.add(
                ConversationPrompt(id, message, validator, onInput)
            )
        }

        /**
         * Adds a simple text prompt (no validation).
         */
        fun prompt(id: String, message: String) {
            prompt(id, text(message))
        }

        /**
         * Sets the escape sequence to cancel the conversation.
         * Default is "cancel".
         */
        fun escapeSequence(sequence: String) {
            conversation.escapeSequence = sequence
        }

        /**
         * Sets the conversation timeout in seconds.
         * Default is 30 seconds.
         */
        fun timeout(seconds: Int) {
            conversation.timeout = seconds
        }

        /**
         * Sets the callback when the conversation completes successfully.
         */
        fun onComplete(handler: (NPC, Player, Map<String, String>) -> Unit) {
            conversation.onComplete = handler
        }

        /**
         * Sets the callback when the conversation is cancelled.
         */
        fun onCancel(handler: (NPC, Player) -> Unit) {
            conversation.onCancel = handler
        }
    }

    /**
     * Starts the conversation with the player.
     */
    fun start() {
        if (prompts.isEmpty()) {
            player.sendMessage(text("This NPC has nothing to say."))
            return
        }

        // Register this conversation as active
        activeConversations[player.uniqueId] = this
        isActive = true
        currentPromptIndex = 0

        // Show first prompt
        showPrompt(0)

        // Set up timeout
        scheduleTimeout()
    }

    /**
     * Shows a prompt to the player.
     */
    private fun showPrompt(index: Int) {
        if (index >= prompts.size) {
            endConversation(true)
            return
        }

        val prompt = prompts[index]
        player.sendMessage(prompt.message)
    }

    /**
     * Handles player input for the current prompt.
     */
    private fun handleInput(input: String) {
        if (!isActive || currentPromptIndex >= prompts.size) {
            return
        }

        val prompt = prompts[currentPromptIndex]
        val userInput = input.trim()

        // Check if cancelled
        if (userInput.equals(escapeSequence, ignoreCase = true)) {
            endConversation(false)
            return
        }

        // Validate input if validator is provided
        val isValid = prompt.validator?.invoke(userInput) ?: true
        if (!isValid) {
            // Retry the current prompt on validation failure
            player.sendMessage(text("&cInvalid input. Please try again."))
            showPrompt(currentPromptIndex)
            return
        }

        // Store input
        inputs[prompt.id] = userInput

        // Call onInput callback if provided
        prompt.onInput?.invoke(npc, player, userInput)

        // Move to next prompt
        currentPromptIndex++
        if (currentPromptIndex < prompts.size) {
            showPrompt(currentPromptIndex)
            scheduleTimeout() // Reset timeout for next prompt
        } else {
            // All prompts completed
            endConversation(true)
        }
    }

    /**
     * Ends the conversation.
     */
    private fun endConversation(completed: Boolean) {
        isActive = false
        activeConversations.remove(player.uniqueId)
        timeoutTask?.cancel()
        timeoutTask = null

        if (completed) {
            onComplete?.invoke(npc, player, inputs.toMap())
        } else {
            onCancel?.invoke(npc, player)
        }
    }

    /**
     * Schedules a timeout for the current prompt.
     */
    private fun scheduleTimeout() {
        timeoutTask?.cancel()
        timeoutTask = cc.modlabs.kpaper.extensions.timer(timeout * 20L, "NPCConversationTimeout") {
            if (isActive) {
                player.sendMessage(text("&cConversation timed out."))
                endConversation(false)
            }
        }
    }

    /**
     * Creates a conversation using the DSL builder.
     */
    fun conversation(init: ConversationBuilder.() -> Unit) {
        val builder = ConversationBuilder(this)
        builder.init()
    }

    companion object {
        // Track active conversations per player
        private val activeConversations = ConcurrentHashMap<java.util.UUID, NPCConversation>()

        /**
         * Initializes the chat event listener for conversations.
         * This should be called once when the plugin loads.
         */
        fun initialize() {
            listen<AsyncChatEvent> { event ->
                val player = event.player
                val conversation = activeConversations[player.uniqueId] ?: return@listen

                // Cancel the chat event so the message doesn't appear in chat
                event.isCancelled = true

                // Get the message content
                val message = event.message()
                val messageText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(message)

                // Handle the input
                conversation.handleInput(messageText)
            }
        }
    }
}
