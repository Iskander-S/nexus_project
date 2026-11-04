package ui;

import exception.AuthException;
import model.User;

public final class ProfileActions {
    private final AppContext ctx;

    public ProfileActions(AppContext ctx) {
        this.ctx = ctx;
    }

    public void editProfile() {
        new ProfileDialog(ctx.window(), ctx.user()).showAndWait().ifPresent(this::apply);
    }

    private void apply(ProfileEdit edit) {
        ctx.async(() -> {
            User user = ctx.user();
            try {
                if (!edit.name().equals(user.getName())) {
                    ctx.users().renameUser(user, edit.name());
                }
                if (edit.avatarFile() != null) {
                    ctx.avatars().changeAvatar(user, edit.avatarFile());
                } else if (edit.removeAvatar()) {
                    ctx.avatars().removeAvatar(user);
                }
            } catch (AuthException e) {
                throw new IllegalArgumentException(describe(e), e);
            }
            return user.getName();
        }, saved -> {
            ctx.toast("Profile updated: " + saved, NotificationService.Kind.SUCCESS);
            ctx.reload();
        });
    }

    static String describe(AuthException error) {
        return switch (error.getReason()) {
            case INVALID_USERNAME -> "Name must be 3-30 characters: letters, digits, '_', '.', '-'.";
            case USER_EXISTS -> "This name is already taken.";
            default -> error.getMessage();
        };
    }
}
