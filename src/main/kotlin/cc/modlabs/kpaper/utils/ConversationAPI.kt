package cc.modlabs.kpaper.utils

import cc.modlabs.kpaper.main.PluginInstance
import net.kyori.adventure.text.Component
import org.bukkit.conversations.ConversationContext
import org.bukkit.conversations.ConversationFactory
import org.bukkit.conversations.Prompt
import org.bukkit.conversations.StringPrompt
import org.bukkit.entity.Player

class ConversationAPI(private val player: Player) {

    private val questions = mutableListOf<Question>()
    private val inputs = mutableMapOf<String, String>()

    // Define a question class to hold the question and its validation
    data class Question(
        val identifier: String,
        val prompt: Component,
        val validator: (String) -> Boolean
    )

    // DSL Entry point
    fun conversation(init: ConversationBuilder.() -> Unit) {
        val builder = ConversationBuilder()
        builder.init()
        builder.build()
    }

    // DSL builder class
    inner class ConversationBuilder {
        private val questionList = mutableListOf<Question>()
        private lateinit var onDone: (Map<String, String>) -> Unit

        // Add a question to the conversation
        fun question(identifier: String, prompt: Component, validator: (String) -> Boolean) {
            questionList.add(Question(identifier, prompt, validator))
        }

        // Define the done function to finalize the conversation
        fun done(onComplete: (Map<String, String>) -> Unit) {
            onDone = onComplete
        }

        // Build and start the conversation
        fun build() {
            questions.addAll(questionList)
            startBukkitConversation()
        }

        // Start the Bukkit conversation
        private fun startBukkitConversation() {
            val conversationFactory = ConversationFactory(PluginInstance)
                .withModality(true)
                .withFirstPrompt(createPrompt(0))
                .withEscapeSequence("cancel")
                .thatExcludesNonPlayersWithMessage("Only players can use this!")

            val conversation = conversationFactory.buildConversation(player)
            conversation.begin()
        }

        // Create a prompt for each question
        private fun createPrompt(index: Int): Prompt {
            return object : StringPrompt() {
                override fun getPromptText(context: ConversationContext): String {
                    return questions[index].prompt.toLegacy()
                }

                override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
                    val userInput = input ?: ""
                    val question = questions[index]

                    // Validate the input using true/false logic
                    val isValid = question.validator(userInput)
                    if (isValid) {
                        inputs[question.identifier] = userInput
                    } else {
                        return createPrompt(index) // Retry the current prompt on failure
                    }

                    // Proceed to the next question or end the conversation
                    return if (index + 1 < questions.size) {
                        createPrompt(index + 1)
                    } else {
                        onDone(inputs) // Call the done function with all inputs
                        null // End conversation
                    }
                }
            }
        }
    }
}