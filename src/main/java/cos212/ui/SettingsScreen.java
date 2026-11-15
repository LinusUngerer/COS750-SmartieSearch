package cos212.ui;

import static cos212.ui.Shape.leaf;
import static cos212.ui.Shape.node;

import java.util.Set;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Accessibility settings: text size, colour palette, tree descriptions for screen readers and
 * low vision, and keyboard use. Every change applies straight away and is saved. Colours are
 * picked from a fixed set of palettes; there is no custom colour option, so every palette the
 * student can choose has been checked (see {@link Palette}).
 */
final class SettingsScreen extends VBox {

    private final Settings settings;

    SettingsScreen(Settings settings) {
        this.settings = settings;
        setSpacing(16);
        setPadding(new Insets(22, 28, 22, 28));
        Label subtitle = Ui.css(new Label("Accessibility options. Changes apply straight away and are saved"
                + " for next time."), "subtitle");
        subtitle.setWrapText(true);
        Button reset = Ui.secondary("Reset to defaults");
        reset.setOnAction(e -> settings.reset());
        getChildren().setAll(new VBox(4, Ui.pageTitle("Settings"), subtitle),
                textSizeCard(), paletteCard(), screenReaderCard(), keyboardCard(), Ui.row(8, reset));
    }

    /**
     * Text size, laid out like the Windows setting: a small A, a slider, a large A and an
     * Apply button. The slider stops at each size in {@link Settings.TextSize}; the preview
     * changes as the slider moves, and the app changes when Apply is pressed.
     */
    private VBox textSizeCard() {
        Settings.TextSize[] sizes = Settings.TextSize.values();
        Slider slider = Ui.css(new Slider(0, sizes.length - 1, settings.textSize().ordinal()), "text-size-slider");
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);
        slider.setBlockIncrement(1);
        slider.setPrefWidth(200);
        slider.setMinWidth(140);
        Label small = Ui.css(new Label("A"), "slider-a-small");
        Label large = Ui.css(new Label("A"), "slider-a-large");
        small.setAccessibleText("Smaller");
        large.setAccessibleText("Larger");
        Label percent = Ui.css(new Label(), "slider-value");
        percent.setMinWidth(Region.USE_PREF_SIZE);
        Button apply = Ui.primary("Apply");
        apply.setMinWidth(Region.USE_PREF_SIZE);
        Label preview = Ui.css(new Label("Text size preview: every value in a node's left subtree is smaller than"
                + " the node."), "text-size-preview");
        preview.setWrapText(true);
        preview.setMaxWidth(Double.MAX_VALUE);

        Runnable update = () -> {
            Settings.TextSize chosen = sizes[(int) Math.round(slider.getValue())];
            percent.setText(Math.round(chosen.scale * 100) + "%");
            apply.setDisable(chosen == settings.textSize());
            slider.setAccessibleText("Text size, " + chosen.label + ", " + percent.getText());
            preview.setStyle("-fx-font-size: " + 14 * chosen.scale + "px;");
        };
        slider.valueProperty().addListener((obs, oldValue, newValue) -> update.run());
        apply.setOnAction(e -> {
            settings.textSizeProperty().set(sizes[(int) Math.round(slider.getValue())]);
            update.run();
        });
        settings.textSizeProperty().addListener((obs, oldValue, newValue) -> {
            slider.setValue(newValue.ordinal());
            update.run();
        });
        update.run();

        Label icon = Ui.css(new Label("AA"), "setting-icon");
        VBox words = new VBox(2, Ui.title("Text size"), Ui.note("Text size that appears throughout SmartieSearch"));
        HBox.setHgrow(words, Priority.ALWAYS);
        HBox control = Ui.row(10, small, slider, large, percent, apply);
        control.setMinWidth(Region.USE_PREF_SIZE);
        HBox row = Ui.row(16, icon, words, control);
        return Ui.card(row, preview);
    }

    /**
     * Colour filters, laid out like the Windows colour-filter list: a reset link, then one
     * radio button per palette. Students pick from this fixed set; there are no custom colours.
     */
    private VBox paletteCard() {
        ToggleGroup group = new ToggleGroup();
        VBox choices = new VBox(4);
        for (Palette palette : Palette.values()) {
            RadioButton choice = Ui.css(new RadioButton(palette.label), "filter-option");
            choice.setToggleGroup(group);
            choice.setUserData(palette);
            choice.setSelected(palette == settings.palette());
            choice.setAccessibleHelp(palette.description);
            choice.setTooltip(new Tooltip(palette.description));
            choices.getChildren().add(choice);
        }
        HBox swatches = Ui.row(8);
        Label described = Ui.note("");
        Runnable showSample = () -> {
            Palette palette = settings.palette();
            swatches.getChildren().setAll(Ui.css(new Label("Preview"), "result-text"),
                    swatch(palette, "Selected", "#EEF3FB", "#021B6E", "#021B6E"),
                    swatch(palette, "✓ Correct", "#D6F2E4", "#17805A", "#17694A"),
                    swatch(palette, "✕ Not quite", "#FBDADD", "#C8323D", "#B42318"));
            described.setText(palette.description);
        };
        group.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                settings.paletteProperty().set((Palette) newValue.getUserData());
            }
        });
        settings.paletteProperty().addListener((obs, oldValue, newValue) -> {
            group.getToggles().stream().filter(t -> t.getUserData() == newValue).findFirst()
                    .ifPresent(t -> t.setSelected(true));
            showSample.run();
        });
        showSample.run();

        Button reset = Ui.css(new Button("Reset colours to default"), "link-row");
        reset.setMaxWidth(Double.MAX_VALUE);
        reset.setOnAction(e -> settings.paletteProperty().set(Palette.STANDARD));
        Region divider = Ui.css(new Region(), "settings-divider");
        Label check = Ui.note("Colour is never the only signal: answers also carry a tick or cross and a word, and"
                + " the current tree node has a thick outline. Every filter keeps text contrast at WCAG AA"
                + " (4.5:1) or better.");
        return Ui.card(Ui.title("Colour filters"),
                Ui.note("The colour-blindness filters simulate how the app looks with that type of colour vision."),
                reset, divider, choices, swatches, described, check);
    }

    /** A sample of how one state looks in a palette, drawn with that palette's colours. */
    private static Label swatch(Palette palette, String text, String background, String border, String textColour) {
        Label sample = Ui.css(new Label(text), "swatch");
        sample.setStyle("-fx-background-color: " + palette.map(background) + "; -fx-border-color: "
                + palette.map(border) + "; -fx-text-fill: " + palette.map(textColour) + ";");
        sample.setAccessibleText(text.replace("✓ ", "").replace("✕ ", "") + " colour sample");
        return sample;
    }

    private VBox screenReaderCard() {
        CheckBox describe = Ui.css(new CheckBox("Describe trees in text"), "check-box");
        describe.selectedProperty().bindBidirectional(settings.describeTreesProperty());
        String example = TreeCanvas.describe(node(50, node(30, leaf(20), leaf(40)), leaf(70)), 30, Set.of(50));
        return Ui.card(Ui.title("Screen readers and tree descriptions"),
                Ui.note("Every tree drawing has a text description that screen readers read out. Turn this on to"
                        + " also show it under each tree, which helps with low vision and lets you check what a"
                        + " screen reader will hear."),
                describe,
                Ui.css(new Label("Example: " + example), "tree-description"),
                Ui.note("Testing status: not yet tested with a screen reader. The answer cards, feedback and tree"
                        + " drawings have screen-reader labels, and they need checking with Narrator (Windows,"
                        + " Ctrl+Win+Enter) and NVDA. The checklist is in evaluation/accessibility-study.md."));
    }

    private VBox keyboardCard() {
        CheckBox focus = Ui.css(new CheckBox("Strong focus outline"), "check-box");
        focus.selectedProperty().bindBidirectional(settings.strongFocusProperty());
        GridPane keys = new GridPane();
        keys.setHgap(14);
        keys.setVgap(8);
        String[][] rows = {
            {"Tab / Shift+Tab", "Move to the next or previous control"},
            {"Space or Enter", "Press the focused button or choose the focused answer"},
            {"A, B, C, D", "Choose an answer on a question"},
            {"Enter", "Submit the answer, then go to the next question or page"},
            {"Ctrl+1 to Ctrl+5", "Lessons, Diagnostic Gate, Tree Activity, My Progress, Settings"},
            {"← / →", "Previous or next step of a trace in the tree activity"},
            {"Esc", "Reset the trace in the tree activity"},
        };
        for (int i = 0; i < rows.length; i++) {
            keys.add(Ui.css(new Label(rows[i][0]), "shortcut-key"), 0, i);
            Label what = Ui.css(new Label(rows[i][1]), "result-text");
            what.setWrapText(true);
            keys.add(what, 1, i);
        }
        return Ui.card(Ui.title("Keyboard"),
                Ui.note("Everything in the app can be done without a mouse. The strong focus outline draws a thick"
                        + " amber border around whatever the keyboard is on."),
                focus, keys);
    }
}
