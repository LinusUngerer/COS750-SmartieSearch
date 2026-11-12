package cos212.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsTest {

    @TempDir
    Path dir;

    @Test
    void settingsSurviveARestart() {
        Path file = dir.resolve("sub/settings.properties");
        Settings settings = new Settings(file);
        settings.textSizeProperty().set(Settings.TextSize.EXTRA_LARGE);
        settings.paletteProperty().set(Palette.DEUTERANOPIA);
        settings.describeTreesProperty().set(true);
        settings.strongFocusProperty().set(false);

        Settings reloaded = new Settings(file);
        reloaded.load();
        assertEquals(Settings.TextSize.EXTRA_LARGE, reloaded.textSize());
        assertEquals(Palette.DEUTERANOPIA, reloaded.palette());
        assertTrue(reloaded.describeTreesProperty().get());
        assertFalse(reloaded.strongFocusProperty().get());
    }

    @Test
    void oldRedGreenSettingLoadsAsDeuteranopia() throws IOException {
        Path file = dir.resolve("settings.properties");
        Files.writeString(file, "palette=RED_GREEN\n");
        Settings settings = new Settings(file);
        settings.load();
        assertEquals(Palette.DEUTERANOPIA, settings.palette());
    }

    @Test
    void unknownValuesFallBackToDefaults() throws IOException {
        Path file = dir.resolve("settings.properties");
        Files.writeString(file, "textSize=HUGE\npalette=RAINBOW\n");
        Settings settings = new Settings(file);
        settings.load();
        assertEquals(Settings.TextSize.STANDARD, settings.textSize());
        assertEquals(Palette.STANDARD, settings.palette());
        assertTrue(settings.strongFocusProperty().get());
    }

    @Test
    void stylesheetScalesFontsAndMapsColours() {
        String css = ".a { -fx-font-size: 10px; -fx-text-fill: #17805A; -fx-pref-height: 50; -fx-padding: 4 8 4 8; }";
        String out = Stylesheet.transform(css, Settings.TextSize.EXTRA_LARGE, Palette.DEUTERANOPIA, false);
        assertTrue(out.contains("-fx-font-size: 14.0px"), out);
        assertTrue(out.contains("-fx-pref-height: 70.0"), out);
        assertTrue(out.contains("-fx-padding: 4 8 4 8"), out);
        assertTrue(out.contains(Palette.DEUTERANOPIA.map("#17805A")), out);
        assertFalse(out.contains("#17805A"), out);
    }

    @Test
    void strongFocusOutlineIsAddedOnlyWhenOn() {
        assertTrue(Stylesheet.transform("", Settings.TextSize.STANDARD, Palette.STANDARD, true).contains(":focused"));
        assertFalse(Stylesheet.transform("", Settings.TextSize.STANDARD, Palette.STANDARD, false).contains(":focused"));
    }

    @Test
    void treeDescriptionNamesRootChildrenCurrentAndVisited() {
        String text = TreeCanvas.describe(Shape.node(50, Shape.node(30, Shape.leaf(20), null), Shape.leaf(70)),
                30, Set.of(50));
        assertEquals("Tree with 4 nodes. The root is 50. 50 has left child 30 and right child 70."
                + " 30 has only a left child, 20. Current node: 30. Already passed: 50.", text);
        assertEquals("The tree is empty.", TreeCanvas.describe(null, null, Set.of()));
    }
}
