# Nexus — квест-система с рангами

Задачи (Daily / Weekly / Global), XP, уровни и ранги E → S. Данные хранятся в H2 (`~/.nexus/`).

## Запуск

```bash
mvn javafx:run          # графический интерфейс (JavaFX)
mvn test                # тесты
```

Из IDE запускайте класс `Main`. Консольный режим: `Main --cli` (аргумент программы).
Нужны JDK 21+ и интернет при первой сборке (Maven скачает JavaFX, H2, HikariCP).

GUI и CLI используют одну БД и одного игрока (`user_001`).

## Что исправлено

| Где | Проблема | Исправление |
|---|---|---|
| `Main.java` | `class пше Main` — проект не компилировался | исправлено |
| `pom.xml` | старые Maven пропускают JUnit 5 тесты без явной версии surefire | `maven-surefire-plugin 3.2.5` |
| `DatabaseManager` | пул создавался в `static {}`: при занятой БД — `ExceptionInInitializerError`, а затем `NoClassDefFoundError` в `finally` скрывал настоящую причину | ленивая инициализация, понятное сообщение об ошибке, безопасный `shutdown()` |
| `TaskService` | можно создать квест с пустым названием / без пользователя | валидация в `assignTask` |
| `TaskRepository.deleteById` | удаляет по одному id, не проверяя владельца | новый `TaskService.deleteTask(userId, taskId)` с проверкой владельца |

## Что добавлено

* **JavaFX-интерфейс** (`src/ui/NexusApp` и экраны): Dashboard (профиль, прогресс до уровня и ранга, статистика), Quests (карточки, фильтр по типу, выполнение и удаление), History.
* Диалог создания квеста; для Global-квестов — диалог оценки со слайдером и предпросмотром оценки (S…E) и XP.
* Уведомления, «LEVEL UP / RANK UP», горячая клавиша **Ctrl+N**, тёмная тема (`src/main/resources/css/nexus.css`).
* Операции с БД выполняются в фоне — окно не подвисает.
* При первом запуске спрашивается имя игрока.
* `Rank.next()`, `User.getXpIntoLevel()`, `UserService.findUser()`, `TaskService.getTasks()/getCompletedTasks()`.
* `config.AppServices` — единая сборка зависимостей для GUI и CLI.
* Новые тесты: пустое название, удаление чужой задачи, список выполненных, цепочка рангов.
