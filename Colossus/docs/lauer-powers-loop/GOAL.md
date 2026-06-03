# GOAL — lauer-powers

Generated: 2026-05-28T22:52:39Z by `goal-loop/scaffold.sh`.

---

## The goal

> Implement all 23 Lauer Powers in Colossus per docs/lauer-powers-implementation-plan.md — Phase 1 through Phase 6, all tasks checked off, ant builds clean, ant test passes, AI-vs-AI games with powers complete without crashes.

Loop retires when all Definition-of-Done criteria in `LOOP.md` are true.

---

## Issue-discovery sources

Every cycle, if the todo column is empty, the agent must run these and
`kanban-md create` a card for each **new** finding (skip findings already
on the board by title match):

- **Tree grep.** `grep -rnE '(TODO|FIXME|XXX|TBD)' /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop` scoped to the
  project tree. Obfuscate the markers in any card body you write (e.g.
  `T${""}ODO`) so DoD-4 zero-markers stays satisfiable.
- **Failed tests** (if applicable). Last ToD / CI / pytest run in the
  project. Each failure = one card.
- **User-reported issues** in chat (if any are tagged with this project's
  name).

Add/remove sources to fit the goal. Keep this list short — each source adds
cycle cost.

## Cycle protocol

Re-read `LOOP.md §"Operating invariants"` every cycle. One cycle = one
atomic step. Never more than 5 tool calls per cycle.

Card lifecycle: **todo → in-progress → review**. Never move to done yourself
— human approves.

## Persistence rule (CRITICAL — re-read every cycle)

The loop stops in **exactly two cases**, nothing else:

1. **Goal achieved** — all DoD criteria in `LOOP.md` check green.
2. **Unrecoverable infrastructure error** — host/tooling itself broken
   (disk full, network down, auth provider out, kanban-md missing from
   PATH, kernel OOM). Log one-line diagnosis and stop.

Everything else is a problem to solve, not a stop condition:

- Test/build/lint failures → fix them, that's the job.
- "I don't know how" → read code, grep, log, probe, add a research card.
- A card is blocked → split it, unblock the dep, or pick another card.
- A tool erred → read the error, fix the invocation, retry.
- Tried twice, both failed → try a third angle; never stop on "tried twice".
- Cycle budget feels tight → the service enforces `max_cycles`, not you.

When in doubt: keep going, find another angle, create a new card, and tick.
Only write `STOP` or call `autonudge_stop` for the two cases above.

## Kill switches

- `touch /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/STOP` — agent halts at next cycle entry.
- UI 🔁 → Stop loop — service stops scheduling.
- `autonudge_stop(reason="...")` — in-loop self-halt MCP tool.
