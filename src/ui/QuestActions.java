package ui;

import model.Achievement;
import model.Rank;
import model.Task;
import model.User;

public final class QuestActions {
    private final AppContext ctx;

    public QuestActions(AppContext ctx) {
        this.ctx = ctx;
    }

    public void addQuest() {
        new AddQuestDialog(ctx.window()).showAndWait().ifPresent(task ->
                ctx.async(() -> {
                    ctx.tasks().assignTask(task, ctx.user().getId());
                    return task;
                }, saved -> {
                    ctx.toast("Quest added: " + saved.getTitle(), NotificationService.Kind.SUCCESS);
                    ctx.reload();
                }));
    }

    public void completeQuest(Task task) {
        new CompleteQuestDialog(ctx.window(), task).showAndWait()
                .ifPresent(score -> submit(task, score));
    }

    private void submit(Task task, int score) {
        User user = ctx.user();
        int levelBefore = user.getLevel();
        Rank rankBefore = user.getRank();

        ctx.async(() -> ctx.quests().complete(task.getId(), user, score), result -> {
            ctx.toast("Grade " + result.grade() + "  ·  +" + result.xp() + " XP  ·  " + task.getTitle(),
                    NotificationService.Kind.SUCCESS);
            if (user.getRank() != rankBefore) {
                ctx.toast("RANK UP!  You are now " + user.getRank().getDisplayName(),
                        NotificationService.Kind.LEVEL_UP);
            } else if (user.getLevel() > levelBefore) {
                ctx.toast("LEVEL UP!  You reached level " + user.getLevel(),
                        NotificationService.Kind.LEVEL_UP);
            }
            for (Achievement achievement : result.unlocked()) {
                ctx.toast(achievement.icon() + "  Achievement unlocked: " + achievement.title(),
                        NotificationService.Kind.ACHIEVEMENT);
            }
            ctx.reload();
        });
    }

    public void deleteQuest(Task task) {
        boolean confirmed = Dialogs.confirm(ctx.window(), "Delete quest?",
                "“" + task.getTitle() + "” will be removed permanently.", "Delete");
        if (!confirmed) {
            return;
        }
        ctx.async(() -> {
            ctx.tasks().deleteTask(ctx.user().getId(), task.getId());
            return task;
        }, removed -> {
            ctx.toast("Quest deleted", NotificationService.Kind.INFO);
            ctx.reload();
        });
    }
}
