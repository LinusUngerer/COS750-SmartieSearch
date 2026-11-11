package cos212.ui;

import cos212.assess.ProgressTracker;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
        getStyleClass().add("progress-page");
        setSpacing(12);
        setPadding(new Insets(22, 28, 22, 28));
        refresh();
    }

    /** Rebuilds the rows from the current progress. */
    void refresh() {
        VBox rows = new VBox(9);
        for (Objectives.Objective objective : Objectives.ALL) {
            rows.getChildren().add(row(objective));
        }
        Button back = Ui.secondary("Back to lessons");
        back.setOnAction(e -> backToLessons.run());
        HBox backRow = Ui.row(8, back);
        backRow.setAlignment(Pos.CENTER_RIGHT);
        backRow.setPadding(new Insets(6, 0, 0, 0));
        Region rule = new Region();
        rule.getStyleClass().add("progress-rule");
        rule.setMinHeight(1);
        rule.setPrefHeight(1);
        rule.setMaxHeight(1);
        getChildren().setAll(header(), rule, masteryGuide(), objectivesHeader(), rows, backRow);
    }

    private HBox header() {
        Label subtitle = Ui.css(new Label("See what you have practised so far, and where to focus next."),
                "subtitle");
        long secure = Objectives.ALL.stream()
                .filter(objective -> tracker.status(objective.id()) == ProgressTracker.Status.SECURE)
                .count();
        int average = (int) Math.round(Objectives.ALL.stream()
                .mapToInt(objective -> tracker.progressPercent(objective.id())).average().orElse(0));
        VBox secureStat = new VBox(1, Ui.css(new Label(String.valueOf(secure)), "summary-value"),
                Ui.note("of " + Objectives.ALL.size() + " secure"));
        VBox practisedStat = new VBox(1, Ui.css(new Label(average + "%"), "summary-value"),
                Ui.note("practised"));
        Separator divider = new Separator(Orientation.VERTICAL);
        divider.getStyleClass().add("summary-divider");
        HBox summary = Ui.row(16, secureStat, divider, practisedStat);
        summary.getStyleClass().add("progress-summary");
        summary.setMinWidth(Region.USE_PREF_SIZE);

        Label dot = Ui.css(new Label("\u25CF"), "progress-dot");
        HBox titleRow = Ui.row(10, dot, Ui.css(Ui.pageTitle("Progress"), "progress-title"));
        VBox copy = new VBox(10, titleRow, subtitle);
        HBox.setHgrow(copy, Priority.ALWAYS);
        HBox header = Ui.row(14, copy, summary);
        header.getStyleClass().add("progress-header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private HBox masteryGuide() {
        Label icon = Ui.css(new Label("i"), "info-icon");
        icon.setAlignment(Pos.CENTER);
        VBox copy = new VBox(3, Ui.css(new Label("How mastery works"), "guide-title"),
                Ui.css(new Label("Practised means you have answered at least "
                        + ProgressTracker.PRACTISED_AFTER + " items. Secure means "
                        + ProgressTracker.SECURE_CORRECT + " of your last "
                        + ProgressTracker.SECURE_WINDOW + " first answers were correct, without hints."),
                        "guide-text"));
        ((Label) copy.getChildren().get(1)).setWrapText(true);
        HBox.setHgrow(copy, Priority.ALWAYS);

        Button next = Ui.css(new Button(nextLabel()), "btn-cta");
        next.setOnAction(e -> backToLessons.run());
        VBox recommended = new VBox(4, Ui.css(new Label("RECOMMENDED NEXT"), "caption"), next);
        recommended.setAlignment(Pos.CENTER);

        HBox guide = Ui.row(14, icon, copy, recommended);
        guide.getStyleClass().add("mastery-guide");
        return guide;
    }

    /** The label for the first lesson that still has an objective that is not secure. */
    private String nextLabel() {
        for (Objectives.Lesson lesson : Objectives.LESSONS) {
            boolean open = lesson.objectives().stream()
                    .anyMatch(id -> tracker.status(id) != ProgressTracker.Status.SECURE);
            if (open) {
                return "Continue: " + lesson.id() + " " + lesson.title();
            }
        }
        return "Review the lessons";
    }

    private VBox objectivesHeader() {
        HBox legend = Ui.row(14, legendItem("Secure", "legend-secure"),
                legendItem("Practised", "legend-practised"), legendItem("Untried", "legend-untried"));
        Label note = Ui.css(new Label("Progress is based on your first answer to each question."), "note");
        HBox.setHgrow(note, Priority.ALWAYS);
        note.setMaxWidth(Double.MAX_VALUE);
        HBox noteRow = Ui.row(12, note, legend);
        VBox header = new VBox(2, Ui.title("Learning objectives"), noteRow);
        header.setPadding(new Insets(8, 0, 0, 0));
        return header;
    }

    private static HBox legendItem(String text, String dotClass) {
        String mark = dotClass.equals("legend-untried") ? "\u25CB" : "\u25CF";
        return Ui.row(5, Ui.css(new Label(mark), "legend-dot", dotClass), Ui.css(new Label(text), "note"));
    }

    private HBox row(Objectives.Objective objective) {
        String id = objective.id();
        VBox name = new VBox(2, Ui.css(new Label(id), "mono-small"),
                Ui.css(new Label(objective.title()), "objective-title"), Ui.note(objective.description()));
        name.setPrefWidth(260);
        name.setMinWidth(220);

        int percent = tracker.progressPercent(id);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox barLabels = new HBox(Ui.css(new Label("Progress"), "stat-label"), gap,
                Ui.css(new Label(percent + "%"), "percent-label"));
        ProgressBar bar = Ui.bar(percent / 100.0);
        VBox progress = new VBox(5, barLabels, bar);
        HBox.setHgrow(progress, Priority.ALWAYS);

        Label chipLabel = Ui.chip(tracker.status(id));
        chipLabel.setText(capitalise(chipLabel.getText()));
        HBox chip = new HBox(chipLabel);
        chip.setAlignment(Pos.CENTER);
        chip.setPrefWidth(90);

        HBox row = Ui.row(16, name, progress, chip,
                stat("Attempts", tracker.attempts(id), 70),
                stat("First-time correct", tracker.correctFirst(id), 110),
                stat("Hints used", tracker.hints(id), 80));
        row.getStyleClass().addAll("card", "objective-row");
        return row;
    }

    private static String capitalise(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static VBox stat(String label, int value, double width) {
        VBox box = new VBox(3, Ui.css(new Label(label), "stat-label"),
                Ui.css(new Label(String.valueOf(value)), "stat-value"));
        box.setPrefWidth(width);
        box.setMinWidth(width);
        return box;
    }
}