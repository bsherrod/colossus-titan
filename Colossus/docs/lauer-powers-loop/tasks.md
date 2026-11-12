# Tasks

Managed via kanban-md. Board: `/Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/board`

## Pick next task
```bash
kanban-md --dir /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/board pick --claim loop-agent --move in-progress --status todo
```

## Complete task
```bash
kanban-md --dir /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/board edit <id> --release
kanban-md --dir /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/board move <id> done
```

## Policy
- TDD: write failing test FIRST, then implement
- Test BOTH server logic AND client UI eligibility (see task #456)
- One task per cycle
- Source code: `/Users/sherrodz/src/colossus-titan/Colossus`
- Test files: `core/src/test/java/net/sf/colossus/server/` (unit), `core/src/functest/java/net/sf/colossus/server/` (functest)
