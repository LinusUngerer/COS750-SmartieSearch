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
        getChildren().setAll(header(), masteryGuide(), objectivesHeader(), rows, Ui.row(8, back));
    }

    private HBox header() {
        Label subtitle = Ui.css(new Label("See what you have practised so far, and where to focus next."),
                "subtitle");
        long secure = Objectives.ALL.stream()
                .filter(objective -> tracker.status(objective.id()) == ProgressTracker.Status.SECURE)
                .count();
        int average = (int) Math.round(Objectives.ALL.stream()
                .mapToInt(objective -> tracker.progressPercent(objective.id())).average().orElse(0));
        VBox summary = Ui.card(
                Ui.css(new Label(secure + " of " + Objectives.ALL.size() + " secure"), "summary-value"),
                Ui.note(average + "% practised"));
        summary.getStyleClass().add("progress-summary");
        HBox header = Ui.row(14, new VBox(4, Ui.pageTitle("Progress"), subtitle), summary);
        HBox.setHgrow(header.getChildren().get(0), Priority.ALWAYS);
        header.setAlignment(Pos.TOP_LEFT);
        return header;
    }

    private HBox masteryGuide() {
        VBox copy = new VBox(4, Ui.title("How mastery works"),
                Ui.css(new Label("Practised means at least 5 answers. Secure means at least 4 of your last 5"
                        + " first answers were correct, with no hints."), "apricot-text"));
        Button lessons = Ui.primary("Continue learning");
        lessons.setOnAction(e -> backToLessons.run());
        HBox.setHgrow(copy, Priority.ALWAYS);
        HBox guide = Ui.row(16, copy, lessons);
        guide.getStyleClass().add("mastery-guide");
        return guide;
    }

    private HBox objectivesHeader() {
        HBox legend = Ui.row(10, Ui.chip(ProgressTracker.Status.SECURE),
                Ui.chip(ProgressTracker.Status.PRACTISED), Ui.chip(ProgressTracker.Status.UNTRIED));
        HBox header = Ui.row(12, Ui.title("Learning objectives"), legend);
        HBox.setHgrow(header.getChildren().get(0), Priority.ALWAYS);
        return header;
    }

    private HBox row(Objectives.Objective objective) {
        String id = objective.id();
        VBox name = new VBox(2, Ui.css(new Label(id), "mono-small"),
                Ui.css(new Label(objective.title()), "card-title"), Ui.note(objective.description()));
        name.setPrefWidth(245);

        ProgressBar bar = Ui.bar(tracker.progressPercent(id) / 100.0);
        HBox.setHgrow(bar, Priority.ALWAYS);

        HBox chip = new HBox(Ui.chip(tracker.status(id)));
        chip.setPrefWidth(82);

        HBox row = Ui.row(12, name, bar, chip,
                stat("Attempts", tracker.attempts(id)),
                stat("First-time correct", tracker.correctFirst(id)),
                stat("Hints used", tracker.hints(id)));
        row.getStyleClass().add("card");
        return row;
    }

    private static VBox stat(String label, int value) {
        VBox box = new VBox(2, Ui.note(label), Ui.css(new Label(String.valueOf(value)), "stat-value"));
        box.setPrefWidth(78);
        return box;
    }
}
