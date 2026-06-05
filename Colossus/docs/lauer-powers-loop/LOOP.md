# LOOP — lauer-powers continuous session guide

One page. Tells you how to launch an autonomous AutoNudge loop in this MeshClaw session for **lauer-powers**.

Generated: 2026-05-28T22:52:39Z by `self-nudge-loop/scaffold.sh`.

---

## Definition of Done

The loop retires itself when **all** are true:

1. **Build clean.** `cd /Users/sherrodz/src/colossus-titan/Colossus && ant clean jar 2>&1 | tail -1` contains "BUILD SUCCESSFUL".
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
BOARD=/Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/board

STOP CHECK:
1. If /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/STOP exists: call autonudge_stop(reason="sentinel"), do nothing else.

PICK:
2. kanban-md --dir $BOARD list --status todo --json | pick first by ID
3. kanban-md --dir $BOARD move <id> in-progress
4. If no todo tasks: autonudge_stop(reason="board drained"), stop.

EXECUTE (<=5 tool calls):
5. Do ONE atomic thing for the task.
6. NEVER git push. NEVER destructive ops.
7. For UI tasks: consult docs/lauer-powers-loop/ui-designs.md.

DONE:
8. kanban-md --dir $BOARD move <id> done
9. DM owner via send_message: "🔁 lauer-powers cycle-<n> · #<id>  Done: <1-line>  Next: <next-id>"

One cycle = one task.
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
8. Loop moves cards directly to done.
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