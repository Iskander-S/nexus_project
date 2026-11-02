# Nexus - a quest system with ranks

Tasks (Daily / Weekly / Global), XP, levels and ranks E → S. Data is stored in H2 (`~/.nexus/`).

## Running

```bash
mvn javafx:run          # graphical interface (JavaFX)
mvn test                # tests
```

From the IDE, run the `Main` class. Console mode: `Main --cli` (program argument).
We need JDK 21+ and the Internet at the first build (Maven downloads JavaFX, H2, HikariCP).

GUI and CLI use one database and one player ('user_001`).

## What's fixed

| Where | Is the Problem | Fix |
|---|---|---|
| `Main.java ` | `class or Main` — the project was not compiled | fixed |
| `pom.xml | old Maven versions skip JUnit 5 tests without an explicit surefire version | `maven-surefire-plugin 3.2.5` |
| `DatabaseManager` | the pool was created in `static {}`: when the database was busy, an `ExceptionInInitializerError` occurred, and then a `NoClassDefFoundError` in `finally` hid the real cause | lazy initialization, clear error message, safe `shutdown()` |
| `TaskService` | you can create a quest with an empty name / without a user | validation in `assignTask` |
| `TaskRepository.deleteById` | deletes by id, without checking the owner | new `TaskService.deleteTask(userId, taskId)` with owner check |

## What’s added

* **JavaFX interface** (`src/ui/NexusApp` and screens): Dashboard (profile, progress to level and rank, statistics), Quests (cards, filter by type, completion and deletion), History.
* Quest creation dialog; for Global quests — a rating dialog with a slider and preview of the rating (S…E) and XP.
* Notifications, “LEVEL UP / RANK UP”, hotkey **Ctrl+N**, dark theme (`src/main/resources/css/nexus.css`).
* Database operations are performed in the background — the window doesn’t freeze.
* On the first launch, the player’s name is requested.
* `Rank.next()`, `User.getXpIntoLevel()`, `UserService.findUser()`, `TaskService.getTasks()/getCompletedTasks()`.
* `config.AppServices` — a unified dependency build for GUI and CLI.
* New tests: empty name, deleting someone else’s task, list of completed tasks, rank chain.
