package cos212.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * The accessibility settings: text size, colour palette, text descriptions of trees and the
 * strong keyboard focus outline. They are saved to {@code .smartiesearch/settings.properties}
 * in the user's home directory (or the file named by {@value #PATH_PROPERTY}) whenever one
 * changes, and loaded when the app starts. Loading and saving never throw: a missing or
 * damaged file gives the defaults.
 */
final class Settings {

    /** The system property that overrides the default location of the settings file. */
    static final String PATH_PROPERTY = "smartiesearch.settings";

    /**
     * The text sizes a student can pick, in order from smallest to largest; they are the stops
     * on the Settings slider. The scale multiplies every font size in the app.
     */
    enum TextSize {
        SMALL("Small", 0.9),
        STANDARD("Standard", 1.0),
        SLIGHTLY_LARGE("Slightly large", 1.1),
        LARGE("Large", 1.2),
        VERY_LARGE("Very large", 1.3),
        EXTRA_LARGE("Extra large", 1.4);

        final String label;
        final double scale;

        TextSize(String label, double scale) {
            this.label = label;
            this.scale = scale;
        }
    }

    private static final Settings CURRENT = new Settings(defaultPath());

    private final Path file;
    private final ObjectProperty<TextSize> textSize = new SimpleObjectProperty<>(TextSize.STANDARD);
    private final ObjectProperty<Palette> palette = new SimpleObjectProperty<>(Palette.STANDARD);
    private final BooleanProperty describeTrees = new SimpleBooleanProperty(false);
    private final BooleanProperty strongFocus = new SimpleBooleanProperty(true);
    private boolean loading;

    Settings(Path file) {
        this.file = file;
        textSize.addListener((obs, oldValue, newValue) -> save());
        palette.addListener((obs, oldValue, newValue) -> save());
        describeTrees.addListener((obs, oldValue, newValue) -> save());
        strongFocus.addListener((obs, oldValue, newValue) -> save());
    }

    /** @return the settings the running app uses */
    static Settings current() {
        return CURRENT;
    }

    private static Path defaultPath() {
        String override = System.getProperty(PATH_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return Path.of(System.getProperty("user.home"), ".smartiesearch", "settings.properties");
    }

    ObjectProperty<TextSize> textSizeProperty() {
        return textSize;
    }

    ObjectProperty<Palette> paletteProperty() {
        return palette;
    }

    /** Whether a text description is shown under every tree drawing. */
    BooleanProperty describeTreesProperty() {
        return describeTrees;
    }

    /** Whether the control with keyboard focus gets a thick, high-visibility outline. */
    BooleanProperty strongFocusProperty() {
        return strongFocus;
    }

    TextSize textSize() {
        return textSize.get();
    }

    Palette palette() {
        return palette.get();
    }

    /** Puts every setting back to its default. */
    void reset() {
        textSize.set(TextSize.STANDARD);
        palette.set(Palette.STANDARD);
        describeTrees.set(false);
        strongFocus.set(true);
    }

    /** Reads the settings file, if there is one. Unknown or damaged values keep their defaults. */
    void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        Properties saved = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            saved.load(in);
        } catch (IOException e) {
            System.err.println("Could not read " + file + " (" + e.getMessage() + "). Using default settings.");
            return;
        }
        loading = true;
        try {
            textSize.set(parse(TextSize.class, saved.getProperty("textSize"), TextSize.STANDARD));
            palette.set(parsePalette(saved.getProperty("palette")));
            describeTrees.set(Boolean.parseBoolean(saved.getProperty("describeTrees", "false")));
            strongFocus.set(Boolean.parseBoolean(saved.getProperty("strongFocus", "true")));
        } finally {
            loading = false;
        }
    }

    private static Palette parsePalette(String name) {
        try {
            return name == null ? Palette.STANDARD : Palette.fromName(name);
        } catch (IllegalArgumentException e) {
            return Palette.STANDARD;
        }
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name, E fallback) {
        if (name == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private void save() {
        if (loading) {
            return;
        }
        Properties out = new Properties();
        out.setProperty("textSize", textSize.get().name());
        out.setProperty("palette", palette.get().name());
        out.setProperty("describeTrees", Boolean.toString(describeTrees.get()));
        out.setProperty("strongFocus", Boolean.toString(strongFocus.get()));
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (OutputStream stream = Files.newOutputStream(file)) {
                out.store(stream, "SmartieSearch accessibility settings");
            }
        } catch (IOException e) {
            System.err.println("Could not save settings to " + file + ": " + e.getMessage());
        }
    }
}
