# River Crossing Solver

Generic, n-entity river crossing puzzle solver. uses breadth-first search over the state graph, so the returned 
solution always has the minimum possible number of crossings (or correctly reports that no solution exists). 

## Requirements

- JDK 17+
- Gradle (or use the included wrapper — no local Gradle install needed)

## Setup

```bash
git clone <your-repo-url>
cd river-crossing-solver
./gradlew build
```

## Usage

```bash
./gradlew run
```

## Project structure

```
river-crossing-solver/
├── gradlew, gradlew.bat          # Gradle wrapper scripts
├── gradle/wrapper/                # Wrapper jar + version config
├── settings.gradle.kts
├── build.gradle.kts
├── src/
│   ├── main/kotlin/
│   │   ├── Constraint.kt          # Constraint Data Class
│   │   ├── Entity.kt              # Entity Data Class
│   │   ├── Main.kt                # Entry point, prompts, wiring
│   │   ├── Renderer.kt            # output renderer
│   │   └── State.kt               # State Data Class
│   └── test/kotlin/               # No tests for now
└── .gitignore
```

## License

MIT (or replace with your project's license)
