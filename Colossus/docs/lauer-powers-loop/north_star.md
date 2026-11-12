# North Star

Fully implement all 22 Lauer Powers so they work correctly in real gameplay
— both for human players and AI. Every mechanic of every power should function
as described in lauerpowers.html.

## Current Work

Write client-side unit tests using saved-game XML (Colossus notation).
Each test loads a forced game state via LoadGameWithBattleTest, gets the real
Client via getClientForName(), and asserts specific boolean/list results.

## Tasks

Board: /Users/sherrodz/src/colossus-titan/Colossus/docs/lauer-powers-loop/board
List:  kanban-md list --status todo
Show:  kanban-md show <ID>
Done:  kanban-md move <ID> done
