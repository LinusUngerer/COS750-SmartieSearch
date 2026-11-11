package cos212.ui;

import cos212.assess.Diagnostic;
import cos212.assess.ProgressTracker;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * The lesson list: the diagnostic check, then the four lessons in order. Lessons stay
 * locked until the diagnostic is passed. Lesson content is added in increment I4, so the
 * Open lesson buttons are shown but disabled.
 */
final class LessonsScreen extends VBox {

    private final ProgressTracker tracker;
    private final Diagnostic diagnostic;
    private final Runnable startDiagnostic;
    private final Runnable viewProgress;

    LessonsScreen(ProgressTracker tracker, Diagnostic diagnostic, Runnable startDiagnostic,
            Runnable viewProgress) {
        this.tracker = tracker;
        this.diagnostic = diagnostic;
        this.startDiagnostic = startDiagnostic;
        this.viewProgress = viewProgress;
        setSpacing(16);
        setPadding(new Insets(22, 28, 22, 28));
        refresh();
    }

    /** Rebuilds the screen from the current diagnostic and progress. */
    void refresh() {
        boolean unlocked = diagnostic.unlocked();
        getChildren().setAll(header(), banner(), lessonRow(unlocked), viewProgressRow());
    }

    private VBox header() {
        Label subtitle = Ui.css(new Label("Four lessons, in order. Start with the diagnostic."), "subtitle");
        return new VBox(4, Ui.pageTitle("Lessons"), subtitle);
    }

    private HBox banner() {
        String status = diagnostic.unlocked() ? "passed"
                : diagnostic.isComplete() ? "not passed"
                : diagnostic.answered() > 0 ? "in progress"
                : "not attempted";
        VBox text = new VBox(6, Ui.titleRow("Diagnostic check"),
                Ui.note("5 questions on the ordering rule. 4 correct unlocks the tree activities. Status: "
                        + status + "."));
        HBox.setHgrow(text, Priority.ALWAYS);
        Button start = Ui.primary("Start diagnostic");
        start.setOnAction(e -> startDiagnostic.run());
        HBox banner = Ui.row(16, text, start);
        banner.getStyleClass().add("banner-diag");
        return banner;
    }

    private HBox lessonRow(boolean unlocked) {
        HBox row = new HBox(16);
        for (Objectives.Lesson lesson : Objectives.LESSONS) {
            VBox card = lessonCard(lesson, unlocked);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    private VBox lessonCard(Objectives.Lesson lesson, boolean unlocked) {
        Label id = Ui.css(new Label(lesson.id()), "chip");
        VBox objectives = new VBox(10);
        VBox.setVgrow(objectives, Priority.ALWAYS);
        for (String objectiveId : lesson.objectives()) {
            ProgressTracker.Status status = tracker.status(objectiveId);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox head = Ui.row(6, Ui.css(new Label(objectiveId), "mono-small"), spacer, Ui.chip(status));
            objectives.getChildren().add(new VBox(4, head,
                    Ui.bar(tracker.progressPercent(objectiveId) / 100.0)));
        }
        Label lock = unlocked ? Ui.note("") : Ui.css(new Label("Locked until the diagnostic is passed"), "lock");
        lock.setWrapText(true);
        Button open = Ui.off("Open lesson");
        open.setDisable(true);
        open.setMaxWidth(Double.MAX_VALUE);
        open.setTooltip(new Tooltip("Lesson content is added in increment I4."));
        VBox card = Ui.card(id, Ui.title(lesson.title()), Ui.note(lesson.description()), objectives, lock, open);
        card.getStyleClass().add("lesson-card");
        card.setMinWidth(150);
        return card;
    }

    private HBox viewProgressRow() {
        Button view = Ui.secondary("View progress");
        view.setOnAction(e -> viewProgress.run());
        return Ui.row(8, view);
    }
}
