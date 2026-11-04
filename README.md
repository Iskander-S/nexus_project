# Nexus - quest system with ranks

Daily / Weekly / Global quests, XP, levels and ranks E to S. Data is stored in H2 (`~/.nexus/`).

## Run

```bash
mvn javafx:run          # JavaFX GUI
mvn test                # unit tests
```

Run the `Main` class from an IDE. CLI mode: `Main --cli` (program argument).
Requires JDK 17+ and internet access on the first build (Maven downloads JavaFX, H2, HikariCP).

## Architecture

Layered, dependencies point downwards only:

```
ui        JavaFX views, dialogs, toasts            (NexusApp, *View, *Dialog, *Actions)
service   business rules                           (TaskService, EvaluationService, UserService,
                                                    AchievementService, QuestCompletionService, AuthService)
db        repositories (interfaces + H2 impls)     (TaskRepository, UserRepository, AchievementRepository)
model     domain objects                           (Task, DailyTask, WeeklyTask, GlobalQuest, User, Achievement)
config    wiring                                   (AppServices)
```

## Features

### Deadlines and grading
* `Task` has an optional `LocalDate deadline`; `Task.isOverdue(today)` and `Task.DEADLINE_ORDER` (earliest first, quests without a deadline last).
* Every quest type (Daily, Weekly, Global) is graded with `PerformanceGrade` (S, A, B, C, D, E). A score from 0 to 100 is entered on completion; the grade decides the bonus XP.
* The grade is stored with the completed quest (`tasks.grade`) and shown in History.
* `EvaluationService.evaluate(...)` returns an `EvaluationResult` (grade, base XP, bonus XP); `processTaskEvaluation(...)` is kept for compatibility.

### Achievements and leaderboard
* Tables `achievements` and `user_achievements`; model `Achievement` (id, title, description, icon).
* `AchievementRule` is the extension point (Open/Closed): add a rule class and register it in `AchievementService.defaultRules()`.
  Built-in rules: **First Blood** (first completed quest) and **Overachiever** (five quests graded S).
* `QuestCompletionService` completes a quest and then checks achievements; the UI receives one `CompletionResult` (XP, grade, newly unlocked achievements).
* `UserRepository.findTopByXp(limit)` backs the Top-10 leaderboard on the Dashboard (`ORDER BY current_xp DESC, name ASC LIMIT ?`).

### Notifications
* `NotificationService` is the abstraction; `ToastHost` is the JavaFX overlay implementation.
* Every toast fades in, stays and fades out so that it disappears 3 seconds after it was shown (`SequentialTransition` with a `PauseTransition`).
* Kinds: info, success, error, level-up, achievement.

### UI
* Dashboard: Leaderboard card, Achievements card, "Edit profile" button.
* Quests: sorted by deadline; overdue quests have a red border, red title and an "Overdue" chip.
* History: search field backed by a `FilteredList` (title, description, type, grade).
* Profile: change the display name (3-30 characters, unique, case-insensitive) and upload a custom avatar (PNG, JPG or JPEG, up to 5 MB) with a `FileChooser`. The image is copied to `~/.nexus/avatars/`, its path is stored in `users.avatar_path`, and it is shown as a circle-clipped `ImageView` in the sidebar, the dashboard, the leaderboard and the profile dialog. Users without a photo get a placeholder avatar with their initial. The sidebar profile block opens the profile dialog.
* "Logout" sits in the bottom-left corner of the sidebar.

### Typography
The whole application uses `'JetBrains Mono', 'Consolas', 'Courier New', monospace` (set on `.root` and `.nx-dialog`). If JetBrains Mono is not installed on the machine, put `JetBrainsMono-Regular.ttf`, `JetBrainsMono-Bold.ttf` (and optionally the italic files) into `src/main/resources/fonts/`; they are loaded automatically at start-up.

### Language
The complete UI, notifications, dialogs, validation messages and the CLI are in English.

### Dark theme
`dark-theme.css` now uses neutral near-black surfaces (`#121212`, `#1E1E1E`, `#262626`) with deep burgundy accents that match the red accent of the light theme. Overdue items use a brighter red so they stay distinguishable from the accent colour.

## Database changes

`DatabaseManager.initTables()` migrates existing databases automatically:

* `tasks.deadline DATE`, `tasks.grade VARCHAR(2)` (added with `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`)
* `users.avatar_path VARCHAR(500)`
* new tables `achievements` and `user_achievements`
* the achievement catalog is synchronised from the rules at start-up (`AchievementService.syncCatalog()`)

## Tests

`mvn test` runs the JUnit 5 suite in `test/`: domain rules, grading of all quest types, deadline ordering, achievements, leaderboard, profile rename, avatar storage and avatar service, authentication.

## Onboarding

New accounts get four starter quests (two Daily, one Weekly, one Global) with deadlines 1, 2, 7 and 14 days ahead. Passwords are stored as salted SHA-256 hashes (demo grade; swap `PasswordHasher` for PBKDF2/bcrypt/Argon2 in production).
Accounts created by very old versions have no password and cannot log in - register a new account or delete `~/.nexus`.
