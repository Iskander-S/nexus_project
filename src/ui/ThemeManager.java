package ui;

import javafx.scene.Scene;
import javafx.scene.text.Font;
import java.io.InputStream;

import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

public final class ThemeManager {
    private ThemeManager() {
    }

    public enum Theme {
        DARK("/css/dark-theme.css"),
        LIGHT("/css/light-theme.css");

        private final String resource;

        Theme(String resource) {
            this.resource = resource;
        }

        String url() {
            return Objects.requireNonNull(ThemeManager.class.getResource(resource), resource + " not found on classpath")
                    .toExternalForm();
        }

        Theme other() {
            return this == DARK ? LIGHT : DARK;
        }
    }

    private static final String[] BUNDLED_FONTS = {
            "/fonts/JetBrainsMono-Regular.ttf",
            "/fonts/JetBrainsMono-Bold.ttf",
            "/fonts/JetBrainsMono-Italic.ttf",
            "/fonts/JetBrainsMono-BoldItalic.ttf"
    };

    static {
        loadBundledFonts();
    }

    private static void loadBundledFonts() {
        for (String resource : BUNDLED_FONTS) {
            try (InputStream stream = ThemeManager.class.getResourceAsStream(resource)) {
                if (stream != null) {
                    Font.loadFont(stream, 14);
                }
            } catch (java.io.IOException | RuntimeException ignored) {
            }
        }
    }

    private static final String PREF_KEY = "theme";
    private static Theme current = loadSaved();

    public static Theme current() {
        return current;
    }

    public static List<String> stylesheets() {
        String base = Objects.requireNonNull(ThemeManager.class.getResource("/css/base.css"), "base.css not found")
                .toExternalForm();
        return List.of(base, current.url());
    }

    public static void apply(Scene scene) {
        scene.getStylesheets().setAll(stylesheets());
    }

    public static Theme toggle(Scene scene) {
        current = current.other();
        save(current);
        apply(scene);
        return current;
    }

    private static Theme loadSaved() {
        try {
            return Theme.valueOf(Preferences.userNodeForPackage(ThemeManager.class).get(PREF_KEY, Theme.DARK.name()));
        } catch (RuntimeException e) {
            return Theme.DARK;
        }
    }

    private static void save(Theme theme) {
        try {
            Preferences.userNodeForPackage(ThemeManager.class).put(PREF_KEY, theme.name());
        } catch (RuntimeException ignored) {
        }
    }
}
