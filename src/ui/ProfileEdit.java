package ui;

import java.nio.file.Path;

public record ProfileEdit(String name, Path avatarFile, boolean removeAvatar) {
}
