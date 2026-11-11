package cos212.ui;

import cos212.assess.ProgressTracker.Status;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Builders for the controls, so every screen uses the classes defined in app.css. */
final class Ui {

    private Ui() {
    }

    static <T extends Node> T css(T node, String... classes) {
        node.getStyleClass().addAll(classes);
        return node;
    }

    static Label pageTitle(String text) {
        return css(new Label(text), "page-title");
    }

    static Label title(String text) {
        return css(new Label(text), "card-title");
    }

    static Label note(String text) {
        Label label = css(new Label(text), "note");
        label.setWrapText(true);
        return label;
    }

    static Label tag(String text) {
        Label label = css(new Label(text), "tag");
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    static Label mono(String text) {
        return css(new Label(text), "mono");
    }

    static HBox row(double spacing, Node... children) {
        HBox box = new HBox(spacing, children);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** A title with an optional requirement tag beside it. */
    static HBox titleRow(String text, String tagText) {
        HBox box = row(8, title(text));
        if (tagText != null) {
            box.getChildren().add(tag(tagText));
        }
        return box;
    }

    /** A wrapping note followed by requirement tags. */
    static HBox noteWithTags(String text, String... tags) {
        Label label = note(text);
        HBox box = row(6, label);
        HBox.setHgrow(label, Priority.ALWAYS);
        for (String tagText : tags) {
            box.getChildren().add(tag(tagText));
        }
        return box;
    }

    static VBox card(Node... children) {
        return css(new VBox(8, children), "card");
    }

    static Button primary(String text) {
        return css(new Button(text), "btn-primary");
    }

    static Button secondary(String text) {
        return css(new Button(text), "btn-secondary");
    }

    static Button off(String text) {
        return css(new Button(text), "btn-off");
    }

    static Button tab(String text) {
        return css(new Button(text), "tab");
    }

    /** A progress bar for a fraction from 0 to 1. */
    static ProgressBar bar(double fraction) {
        ProgressBar bar = css(new ProgressBar(fraction), "wf-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        return bar;
    }

    /** The status chip for an objective, with the wording used in the wireframes. */
    static Label chip(Status status) {
        String text = switch (status) {
            case UNTRIED -> "untried";
            case STARTED -> "started";
            case PRACTISED -> "practised";
            case SECURE -> "secure";
        };
        return css(new Label(text), "chip", "chip-" + status.name().toLowerCase());
    }
}
