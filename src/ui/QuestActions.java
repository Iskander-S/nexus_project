package ui;

import model.GlobalQuest;
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
                    ctx.toast("Quest added: " + saved.getTitle(), ToastHost.Kind.SUCCESS);
                    ctx.reload();
                }));
    }

    public void completeQuest(Task task) {
        if (task instanceof GlobalQuest quest) {
            new CompleteQuestDialog(ctx.window(), quest).showAndWait()
                    .ifPresent(score -> submit(task, score));
        } else {
            submit(task, null);
        }
    }

    private void submit(Task task, Integer score) {
        User user = ctx.user();
        int levelBefore = user.getLevel();
        Rank rankBefore = user.getRank();

        ctx.async(() -> ctx.tasks().completeTask(task.getId(), user, score), xp -> {
            ctx.toast("+" + xp + " XP  ·  " + task.getTitle(), ToastHost.Kind.SUCCESS);
            if (user.getRank() != rankBefore) {
                ctx.toast("RANK UP!  You are now " + user.getRank().getDisplayName(), ToastHost.Kind.LEVEL_UP);
            } else if (user.getLevel() > levelBefore) {
                ctx.toast("LEVEL UP!  You reached level " + user.getLevel(), ToastHost.Kind.LEVEL_UP);
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
            ctx.toast("Quest deleted", ToastHost.Kind.INFO);
            ctx.reload();
        });
    }
}
