# Roadmap

## Process

1. Pick next todo task from kanban-md board (`kanban-md list` in docs/lauer-powers-loop/board)
2. Run full AI game: `java -cp Colossus.jar net.sf.colossus.tools.PowerGameRunner POWER`
3. Game automatically produces a JSON recording in `~/.colossus/recordings/`
4. Copy the recording to `test-corpus/POWER_N.json`
5. Load it with GameReplayEngine and verify power-specific events fired
6. If ALL mechanics found in recording events → mark task done
7. If a mechanic is MISSING after 5+ games → investigate (bug? AI never triggers it?)
8. If a game HANGS or CRASHES → file a new bug task, fix it (TDD), then retry
9. Never git push. Never destructive ops.
10. Work through ALL tasks regardless of priority.

## What the corpus IS

- A collection of **replayable JSON game recordings** (not debug logs)
- Each recording can be loaded: `GameReplayEngine.load("test-corpus/ADMIN_1.json")`
- Each recording proves specific power mechanics fired during a real game
- Recordings are deterministic (seed captured) — can be replayed identically

## Commands

Project: /Users/sherrodz/src/colossus-titan/Colossus
Board: docs/lauer-powers-loop/board

Build: `ant clean jar`
Run one power: `java -cp Colossus.jar net.sf.colossus.tools.PowerGameRunner POWER`
Recordings: `~/.colossus/recordings/game-YYYYMMDD-HHmmss.json`
Corpus: `test-corpus/<POWER>_<N>.json`
Kanban: `kanban-md list`, `kanban-md move <id> done`
