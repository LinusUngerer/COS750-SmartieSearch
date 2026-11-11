package cos212.ui;

import cos212.assess.ProgressTracker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The progress screen: one row per objective with its bar, mastery status and counts,
 * and the standard for the bars written out in words.
 */
final class ProgressScreen extends VBox {

    private final ProgressTracker tracker;
    private final Runnable backToLessons;

    ProgressScreen(ProgressTracker tracker, Runnable backToLessons) {
        this.tracker = tracker;
        this.backToLessons = backToLessons;
        setSpacing(12);
        setPadding(new Insets(22, 28, 22, 28));
        refresh();
    }

    /** Rebuilds the rows from the current progress. */
    void refresh() {
        VBox rows = new VBox(8);
        for (Objectives.Objective objective : Objectives.ALL) {
            rows.getChildren().add(row(objective));
        }
        Button back = Ui.secondary("Back to lessons");
        back.setOnAction(e -> backToLessons.run());
        VBox meaning = new VBox(4, Ui.title("What the bars mean"),
                Ui.css(new Label("Practised: at least 5 items answered. Secure: at least 4 of your last 5 first"
                        + " answers correct, with no hints."), "apricot-text"));
        meaning.getStyleClass().add("apricot-card");
        getChildren().setAll(header(), rows, meaning, Ui.row(8, back));
    }

    private HBox header() {
        Label subtitle = Ui.css(new Label("What you have practised so far, and how accurately."), "subtitle");
        HBox header = Ui.row(10, new VBox(4, Ui.pageTitle("Progress"), subtitle), Ui.tag("FR10"));
        header.setAlignment(Pos.TOP_LEFT);
        return header;
    }

    private HBox row(Objectives.Objective objective) {
        String id = objective.id();
        VBox name = new VBox(2, Ui.css(new Label(id), "mono-small"),
                Ui.css(new Label(objective.title()), "card-title"), Ui.note(objective.description()));
        name.setPrefWidth(300);

        ProgressBar bar = Ui.bar(tracker.progressPercent(id) / 100.0);
        HBox.setHgrow(bar, Priority.ALWAYS);

        HBox chip = new HBox(Ui.chip(tracker.status(id)));
        chip.setPrefWidth(95);

        HBox row = Ui.row(16, name, bar, chip,
                stat("Attempts", tracker.attempts(id)),
                stat("First-time correct", tracker.correctFirst(id)),
                stat("Hints used", tracker.hints(id)));
        row.getStyleClass().add("card");
        return row;
    }

    private static VBox stat(String label, int value) {
        VBox box = new VBox(2, Ui.note(label), Ui.css(new Label(String.valueOf(value)), "stat-value"));
        box.setPrefWidth(110);
        return box;
    }
}
