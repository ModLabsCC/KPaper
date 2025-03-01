package cc.modlabs.kpaper.scoreboard

import com.google.common.base.Preconditions
import com.google.common.base.Splitter
import dev.fruxz.stacked.text
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import java.util.*


class SimpleScoreboard(private var title: String?) {
    private val scoreboard: Scoreboard

    private val scores: MutableMap<String, Int>
    private val teams: MutableList<Team>

    init {
        this.scoreboard = Bukkit.getScoreboardManager().newScoreboard
        this.scores = mutableMapOf()
        this.teams = mutableListOf()
    }

    fun blankLine() {
        add(" ")
    }

    @JvmOverloads
    fun add(text: String, score: Int? = null) {
        var textData = text
        Preconditions.checkArgument(textData.length < 48, "text cannot be over 48 characters in length")
        textData = fixDuplicates(textData)
        scores[textData] = score!!
    }

    private fun fixDuplicates(text: String): String {
        var textData = text
        while (scores.containsKey(textData)) textData += "§r"
        if (textData.length > 48) textData = textData.substring(0, 47)
        return textData
    }

    private fun createTeam(text: String): Map.Entry<Team?, String> {
        val result: String
        if (text.length <= 16) return AbstractMap.SimpleEntry<Team?, String>(null, text)
        val team = scoreboard.registerNewTeam("text-" + scoreboard.teams.size)
        val iterator: Iterator<String> = Splitter.fixedLength(16).split(text).iterator()
        team.prefix(text(iterator.next()))
        result = iterator.next()
        if (text.length > 32) team.suffix(text(iterator.next()))
        teams.add(team)
        return AbstractMap.SimpleEntry(team, result)
    }

    fun build() {
        val obj =
            scoreboard.registerNewObjective(((if (title!!.length > 16) title!!.substring(0, 15) else title)!!), Criteria.DUMMY, text(title!!))
        obj.displayName(text(title!!))
        obj.displaySlot = DisplaySlot.SIDEBAR

        var index = scores.size

        for ((key, value) in scores) {
            val team = createTeam(key)
            val player = Bukkit.getOfflinePlayer(team.value)
            if (team.key != null) team.key!!.addPlayer(player)
            obj.getScore(player).score = value
            index -= 1
        }
    }

    fun reset() {
        title = null
        scores.clear()
        for (t in teams) t.unregister()
        teams.clear()
    }

    fun send(vararg players: Player) {
        for (p in players) p.scoreboard = scoreboard
    }
}