import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.dim
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal

class Renderer (private val t: Terminal) {
    private fun bankStr(entities: Set<Entity>): String =
        if (entities.isEmpty()) dim("-") else entities.joinToString(", ") { it.name }

    fun renderSolution(start: State, moves: List<Move>) {
        t.println((bold + green)("✓ Solved in ${moves.size} moves."))

        var current = start

        t.println(table {
            header {
                row("#", "Left Bank", "Crossing", "Right Bank")
            }
            body {
                row("start", bankStr(current.leftBank),
                    "-", bankStr(current.rightBank))

                for ((i, move) in moves.withIndex()) {
                    val newLeft = if (current.boatOnLeft) current.leftBank - move.members
                                    else current.leftBank + move.members
                    val next = State(newLeft, !current.boatOnLeft)

                    val direction = if (move.from) "->" else "<-"
                    val crossing =  "$direction ${move.members.joinToString(", ") { it.name }}"

                    row(
                        "${i + 1}",
                        bankStr(next.leftBank),
                        yellow(crossing),
                        bankStr(next.rightBank),
                    )

                    current = next
                }
            }
        })
    }
}
