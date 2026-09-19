# River Crossing Solver

A generic, n-entity river crossing puzzle solver with an interactive terminal UI. Define any number of entities, any set of "can't be left alone together" rules, and any boat capacity — the solver finds the minimum number of crossings needed to get everyone across safely.

Works for the classic wolf/goat/cabbage puzzle, missionaries and cannibals, or any custom scenario you define at runtime — no code changes required.

## Features

- **Arbitrary entity count** — not hardcoded to 3 entities like the classic puzzle
- **Arbitrary boat capacity** — 1, 2, or more entities per crossing
- **Flexible conflict rules** — define groups that can't be left alone together, each either requiring a specific supervising entity or just requiring the boat to be present
- **Guaranteed optimal solution** — uses breadth-first search over the state graph, so the returned solution always has the minimum possible number of crossings (or correctly reports that no solution exists)
- **Fully interactive** — entities, rules, and capacity are all entered at runtime via terminal prompts, no code editing needed
- **Readable tabular output** — the full move sequence is rendered as a Mordant table, not an animation

## Requirements

- JDK 17+
- Gradle (or use the included wrapper — no local Gradle install needed)

## Setup

```bash
git clone <your-repo-url>
cd river-crossing
./gradlew build
```

## Usage

```bash
./gradlew run
```

You'll be walked through three prompts:

### 1. Define your entities

```
Enter entities (comma-separated): wolf, goat, cabbage, farmer
```

### 2. Define conflict rules

For each rule, you're asked in plain language:
- **Which entities can never be left alone together?**
- **Does a specific entity need to be present to keep them safe** (vs. just needing the boat there)?

```
Which entities can never be left alone together? wolf, goat
Does a SPECIFIC entity need to be present to keep them safe? yes
Which entity supervises them? farmer
✓ Rule added: {wolf, goat} needs farmer present

Which entities can never be left alone together? goat, cabbage
Does a SPECIFIC entity need to be present to keep them safe? yes
Which entity supervises them? farmer
✓ Rule added: {goat, cabbage} needs farmer present

Which entities can never be left alone together? [press ENTER to skip]
```

Press **ENTER** on an empty rule prompt when you're done adding rules.

If no specific entity is needed (i.e. the rule only requires *the boat* to be present, not a named supervisor), just answer "no" — this covers puzzles like prisoner/guard scenarios where the boat operator alone is enough supervision.

### 3. Set boat capacity

```
What's the capacity of the boat? (default 2)
```

### Output

The solver prints the full solution as a table, showing the state of both banks after every move:

```
River Crossing Solution — 7 move(s)

┏━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━┳━━━━━━━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━┓
┃ #     ┃ Left Bank           ┃ Crossing      ┃ Right Bank          ┃
┡━━━━━━━╇━━━━━━━━━━━━━━━━━━━━━╇━━━━━━━━━━━━━━━╇━━━━━━━━━━━━━━━━━━━━━┩
│ start │ wolf, goat, cabbage,│ -             │ -                   │
│       │ farmer 🚤           │               │                     │
│ 1     │ wolf, cabbage       │ → goat, farmer│ goat, farmer 🚤     │
│ 2     │ wolf, cabbage, farm-│ ← farmer      │ goat                │
│       │ er 🚤               │               │                     │
│ ...   │ ...                 │ ...           │ ...                 │
└───────┴─────────────────────┴───────────────┴─────────────────────┘

✓ Solved in 7 moves.
```

If no valid solution exists for the given entities, rules, and capacity, the tool reports that clearly instead of crashing or looping forever.

## How it works

The puzzle is modeled as a graph search:

- **State** = which entities are on the left bank + which side the boat is on. The right bank is always implied (`all entities - left bank`).
- **Move** = a legal subset of the boat's current side, of size 1 to `capacity`.
- **Safety check** = for every conflict rule, if all of that rule's members end up on the same bank, either the rule's designated guardian must also be on that bank, or (if no specific guardian was set) the boat itself must be there.

The solver runs a **breadth-first search** from the "everyone on the left, boat on the left" state to the "everyone on the right, boat on the right" state, rejecting any transition that leaves either bank unsafe. Because every move costs exactly one crossing, BFS is guaranteed to find a solution with the **minimum possible number of crossings** — there is no shorter valid solution than the one returned.

**Complexity note:** the state space is `2^n × 2`, so this approach is fast for small-to-moderate numbers of entities (fine well past the classic 3-entity puzzles) but grows exponentially. For very large entity counts, this would need a smarter search (e.g. IDA* with an admissible heuristic) rather than plain BFS.

### Two flavors of "safe" — and why move counts differ

When you set up a rule, the *optimal move count can change significantly* depending on which supervision option you pick:

- **A named guardian** (`guardedBy "farmer"`) means only that specific entity can supervise the risky pair. Every crossing that separates the pair must include the guardian — this matches the traditional wolf/goat/cabbage puzzle, where only the farmer can be trusted to prevent the wolf eating the goat, even if the boat happens to be docked there.
- **The default (no `guardedBy`)** means the risky pair is safe as long as the *boat* is docked at their bank — regardless of who's on it. This is a looser rule: any entity can act as the "shuttle" for a return trip, not just a single dedicated guardian.

These are genuinely different problems with different optimal answers. For wolf, goat, cabbage, farmer with boat capacity 2:

| Constraint style | Optimal moves |
|---|---|
| `{wolf,goat}` and `{goat,cabbage}` each `guardedBy "farmer"` | 7 (the textbook answer — farmer must personally escort every dangerous separation) |
| `{wolf,goat}` and `{goat,cabbage}` with no `guardedBy` (boat-only) | 5 (wolf and cabbage can act as their own return escorts) |

Neither is "more correct" — pick whichever matches the real supervision rule you're modeling. If in doubt, use a named guardian when only one entity is actually capable of watching the group; use the default when any entity's presence (via the boat) is enough.

## Project structure

```
river-crossing/
├── gradlew, gradlew.bat          # Gradle wrapper scripts
├── gradle/wrapper/                # Wrapper jar + version config
├── settings.gradle.kts
├── build.gradle.kts
├── src/
│   ├── main/kotlin/
│   │   ├── Main.kt                # Entry point, prompts, wiring
│   │   ├── Solver.kt              # State, Move, Constraint, BFS solve()
│   │   └── Renderer.kt            # Mordant table rendering
│   └── test/kotlin/
│       └── SolverTest.kt
└── .gitignore
```

## Example: classic wolf, goat, cabbage

| Entities | wolf, goat, cabbage, farmer |
|---|---|
| Rules | wolf+goat `guardedBy` farmer; goat+cabbage `guardedBy` farmer |
| Capacity | 2 (farmer plus one item per trip, or farmer alone) |
| Result | 7 moves — the textbook-optimal solution |

Using the same entities and capacity but leaving the rules at their **default** (no named guardian — just "boat present") instead produces a 5-move solution, since wolf and cabbage can each serve as their own return escort instead of requiring farmer on every trip. See [Two flavors of "safe"](#two-flavors-of-safe--and-why-move-counts-differ) above.

## License

MIT (or replace with your project's license)
