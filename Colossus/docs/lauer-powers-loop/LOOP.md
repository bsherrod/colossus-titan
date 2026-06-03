# LOOP — lauer-powers continuous session guide

One page. Tells you how to launch an autonomous AutoNudge loop in this MeshClaw session for **lauer-powers**.

Generated: 2026-05-28T22:52:39Z by `self-nudge-loop/scaffold.sh`.

---

## Definition of Done

The loop retires itself when **all** are true:

1. **Build clean.** `cd /Users/sherrodz/src/colossus-titan/Colossus && ant 2>&1 | tail -1` contains "BUILD SUCCESSFUL".
2. **Tests pass.** `cd /Users/sherrodz/src/colossus-titan/Colossus && ant test 2>&1 | tail -1` contains "BUILD SUCCESSFUL".
3. **All plan tasks checked.** `grep -c '^\- \[ \]' /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-implementation-plan.md` returns `0`.
4. **Zero stale markers.** `grep -rn "TODO\|FIXME\|XXX\|TBD" /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop` returns empty.
5. **Board drained.** `kanban-md --dir /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/board list --status todo --json | python3 -c "import json,sys;print(len(json.load(sys.stdin)))"` returns `0` AND same for `in-progress` and `review`.

When all five are true, agent posts the DoD checklist with ticks, calls `autonudge_stop(reason="DoD met")`, stays silent.

---

## Start the loop (UI)

1. `ls /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/STOP` → must say "No such file".
2. Click 🔁 Repeat in the composer toolbar.
3. Popover — **all four fields** (no blanks):
   - Nudge: paste from §"Ready-to-paste nudge" below
   - Idle seconds: `60`
   - Max cycles: `30`
   - Stop sentinel path: `/Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/STOP`
4. **Start loop**.

## Start the loop (REST)

```bash
# 1. Get a local user token (loopback only; requires X-Local-Secret header).
SECRET=$(cat ~/.meshclaw/.local_secret)
TOKEN=$(curl -sf -H "X-Local-Secret: $SECRET" \
  "http://127.0.0.1:7777/api/token/local?ttl=1h" \
  | python3 -c 'import json,sys;print(json.load(sys.stdin)["token"])')

# 2. Arm the loop — ?token= query param (NOT Authorization: Bearer).
curl -X POST "http://127.0.0.1:7777/api/autonudge?token=$TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "slot_key": "<slot>",
    "message": "<paste nudge>",
    "idle_secs": 60,
    "max_cycles": 30,
    "stop_sentinel_path": "/Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/STOP"
  }'
```

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/autonudge` | list all loops |
| GET | `/api/autonudge/slot/{slot_key}` | loop bound to a slot |
| POST | `/api/autonudge` | start / replace a loop |
| PATCH | `/api/autonudge/{loop_id}` | edit nudge / idle / active |
| DELETE | `/api/autonudge/{loop_id}` | stop + remove |

---

## Ready-to-paste nudge

```
Continue the lauer-powers loop.
Definition of Done: /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/LOOP.md §Definition of Done.

STOP / EXIT CHECKS (every cycle, in order, before anything else):
1. If /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/STOP exists: call autonudge_stop(reason="sentinel"), post "Loop halted by sentinel.", do nothing else.
2. If the Definition-of-Done criteria in LOOP.md are all met: call autonudge_stop(reason="DoD met"), post the DoD checklist with ticks, stop.

BOARD (skip this block if no kanban-md board in use):
   BOARD=/Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/board
3. kanban-md --dir $BOARD list --status todo --json
4. kanban-md --dir $BOARD pick --assignee loop-<cycle_n>
5. If pick returns nothing and a dep-met backlog card exists: kanban-md --dir $BOARD move <id> --status todo ; then pick.
6. If everything blocked AND you already posted a blocker this arming: autonudge_stop(reason="all blocked"), stop.

EXECUTE (<=5 tool calls per cycle, hard cap):
7. Read the one spec file for the claimed task.
8. Do ONE atomic thing: draft a fix, write a test, run a test, invoke one SOP. Never all at once.
9. NEVER git push. NEVER destructive ops. NEVER reply to real production tickets — sandbox / read-only only.
10. Midway auth: http.cookiejar.MozillaCookieJar(path).load() + urllib opener, OR curl -b ~/.midway/cookie -f -s. NEVER read ~/.midway/cookie as text. NEVER echo cookie contents in any error — scrub exceptions to type(e).__name__.

RECORD:
11. Append progress to the claimed card (kanban-md edit --add-body) or to a Cycle Log section in LOOP.md.
12. DM the owner a tick via send_message (owner DM is default — no channel arg). Template:
    "🔁 lauer-powers cycle-<n> · <task-id>  Done: <1-line>  Next: <1-line>  Status: <col>"
    One DM per productive cycle. Skip halted / no-op cycles.
13. If task complete: handoff to Review (NOT Done — human approves Done).

STAY SILENT in the chat panel unless: DoD met, hard blocker needs user decision, or STOP sentinel tripped.
(send_message DMs are expected every productive cycle — that is the progress channel, not the chat.)
One cycle = one step.
```

---

## Kill switches

| Method | Speed | Notes |
|---|---|---|
| Agent self-halt via `autonudge_stop` | next cycle | Cleanest. Works unattended. |
| UI 🔁 → Stop loop | instant | Fastest manual. |
| `touch /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/STOP` | <=60s | Agent halts and calls autonudge_stop; service honors sentinel. |
| `max_cycles: 30` reached | bounded | Loop deactivates (not removed). Re-arm via PATCH/UI. |
| REST DELETE `/api/autonudge/{loop_id}` | instant | Clean removal. |

---

## Operating invariants (never violate)

1. Never `git push`. Humans push.
2. Never run destructive ops.
3. Never read credential files as text (`~/.midway/cookie`, `~/.aws/*`, `~/.ssh/*`).
4. Never echo credential content in errors — scrub to `type(e).__name__`.
5. If using kanban-md, `kanban-md` is the only board writer.
6. Test execution lives in a sandbox/DevSpace, not the local workspace.
7. One cycle, one step.
8. Human approval required for Done. Loop only moves cards to Review.
9. `max_cycles: 30` cap every arming.
10. `stop_sentinel_path` never blank at loop start.

---

## Troubleshooting

| Symptom | Check |
|---|---|
| UI icon dim | `echo $MESHCLAW_AUTONUDGE` should be `1`. |
| Nudge never fires | `cat ~/.meshclaw/autonudge.json` — loop `active:true`? Right slot_key? |
| Loop fires but agent does nothing | STOP sentinel present? `ls /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/STOP`. |
| `code=-15 SIGTERM` after ~100 cycles | Context-window overflow. Re-arm with `max_cycles: 30`. |

Skill reference: `~/.meshclaw/skills/self-nudge-loop/SKILL.md` §"Hardened Loop Pattern".