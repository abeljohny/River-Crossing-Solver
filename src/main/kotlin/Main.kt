import com.github.ajalt.mordant.animation.textAnimation
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.YesNoPrompt
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.prompt
import com.github.ajalt.mordant.terminal.success
import com.github.ajalt.mordant.terminal.warning

var allEntities: Set<Entity> = emptySet()

fun isSafe(bank: Set<Entity>, boatPresent: Boolean, constraints: Set<Constraint>): Boolean {
    for ((members, guardian) in constraints) {
        if (members.all { it in bank }) {
            val guardianPresent = if (guardian == null) boatPresent else guardian in bank
            if (!guardianPresent) return false
        }
    }
    return true
}

fun allowedMoves(sourceBank: Set<Entity>, capacity: Int): List<Set<Entity>> {
    val out = mutableListOf<Set<Entity>>()
    val sourceBankList = sourceBank.toList()
    fun combos(start: Int, current: MutableList<Entity>) {
        if (current.isNotEmpty()) out.add(current.toSet())
        if (current.size == capacity) return
        for (i in start until sourceBankList.size) {
            current.add(sourceBankList[i])
            combos(i + 1, current)
            current.removeAt(current.size - 1)
        }
    }
    combos(0, mutableListOf())
    return out
}

fun solve(entities: Set<Entity>, capacity: Int, constraints: Set<Constraint>): List<Move>? {
    val start = State(entities, boatOnLeft = true)
    val goal = State(emptySet(), boatOnLeft = false)

    val visited = mutableSetOf(start)
    val queue = ArrayDeque<Pair<State, List<Move>>>()
    queue.addLast(start to emptyList())

    while (queue.isNotEmpty()) {
        val (state, path) = queue.removeFirst()
        if (state == goal) return path

        val source = if (state.boatOnLeft) state.leftBank else state.rightBank
        for (subset in allowedMoves(source, capacity)) {
            val newLeft = if (state.boatOnLeft) state.leftBank - subset else state.leftBank + subset
            val newBoatOnLeft = !state.boatOnLeft
            val newState = State(newLeft, newBoatOnLeft)

            if (!isSafe(newState.leftBank, newBoatOnLeft, constraints)) continue
            if (!isSafe(newState.rightBank, !newBoatOnLeft, constraints)) continue

            if (newState !in visited) {
                visited.add(newState)
                queue.addLast(newState to (path + Move(subset, state.boatOnLeft)))
            }
        }
    }
    return null
}

fun promptEntities(fromTerminal: Terminal): Set<Entity> {
    var entities: Set<Entity>?
    while (true) {
        entities = fromTerminal.prompt("Enter entities (comma-separated): ")
            ?.split(",")
            ?.map { Entity(it.trim()) }
            ?.toSet()
        if (entities.isNullOrEmpty()) {
            fromTerminal.danger("Entities cannot be empty. Try again.")
            continue
        }
        break
    }
    return entities
}

fun constraintStr(constraints: Set<Constraint>): String {
    if (constraints.isEmpty()) return "[]"
    val out = StringBuilder()
    out.append("[")
    for ((members) in constraints) {
        if (out.length > 1) out.append(", ")
        out.append(members.joinToString(", ", prefix = "{", postfix = "}") { it.name })
    }
    out.append("]")
    return out.toString()
}

fun promptConflicts(usingEntities: Set<Entity>, fromTerminal: Terminal): Set<Constraint> {
    val byName = usingEntities.associateBy { it.name }
    val names = byName.keys.toList()
    val constraints = mutableSetOf<Constraint>()

    while (true) {
        // 1. pick the group that can't be left alone
        fromTerminal.println("(Entities: ${names.joinToString(", ")})")
        val memberInput = fromTerminal.prompt(
            "Which entities can never be left alone together? (existing rules: ${constraintStr(constraints)}) [Press " +
                    "ENTER to skip]"
        )
        if (memberInput.isNullOrEmpty()) break

        val memberNames = memberInput.split(",").map { it.trim() }
        val unknown = memberNames.filter { it !in byName }
        if (unknown.isNotEmpty()) {
            fromTerminal.danger("Unknown entities: ${unknown.joinToString(", ")}. Try again.")
            continue
        }
        if (memberNames.size < 2) {
            fromTerminal.danger("Need at least 2 entities for a rule. Try again.")
            continue
        }
        val members = memberNames.map { byName.getValue(it) }.toSet()

        // 2. does someone need to supervise them?
        val needsSpecificSupervisor = YesNoPrompt(
            "Does a SPECIFIC entity need to be present to keep them safe?",
            fromTerminal
        ).ask() ?: false

        val guardian: Entity? = if (needsSpecificSupervisor) {
            val guardianName = fromTerminal.prompt("Which entity supervises them? (from $names)") ?: continue
            val g = byName[guardianName.trim()]
            if (g == null) {
                fromTerminal.danger("Unknown entity '$guardianName'. Rule skipped.")
                continue
            }
            if (g in members) {
                fromTerminal.danger("Supervisor can't be one of the entities being supervised. Rule skipped.")
                continue
            }
            g
        } else {
            null
        }
        if (constraints.add(Constraint(members, guardian))) {
            fromTerminal.success(
                "Rule added: {${memberNames.joinToString(", ")}}" + " " +
                        if (guardian != null) "needs ${guardian.name} present" else "needs the boat present"
            )
        } else {
            fromTerminal.warning(
                "Rule exists, skipping..."
            )
        }
        fromTerminal.println()
    }
    return constraints.toSet()
}

fun main() {
    val t = Terminal()
    allEntities = promptEntities(t)
    val constraints = promptConflicts(allEntities, t)
    val capacityPromptVal = t.prompt(
        "What's the capacity of the boat ? (default 2)"
    )
    val capacity = if (capacityPromptVal.isNullOrEmpty()) 2 else capacityPromptVal.toInt()
    val solution = solve(allEntities, capacity, constraints)
    if (solution == null) {
        t.danger("No solution exists for this configuration.")
        return
    }
    Renderer(t).renderSolution(State(allEntities, boatOnLeft = true), solution)
}
