package cos212.ui;

import cos212.assess.Diagnostic;
import cos212.assess.LessonProgress;
import cos212.assess.ProgressTracker;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * The lesson list: the diagnostic check, then the five lessons in order, L0 first. The lessons are
 * open from the start; only the tree activity waits for the diagnostic. Each lesson's button
 * opens it at the page the student reached last time.
 */
final class LessonsScreen extends VBox {

    private final ProgressTracker tracker;
    private final Diagnostic diagnostic;
    private final Runnable startDiagnostic;
    private final Runnable viewProgress;
    private final LessonProgress lessons;
    private final Consumer<Objectives.Lesson> openLesson;

    LessonsScreen(ProgressTracker tracker, Diagnostic diagnostic, LessonProgress lessons,
            Runnable startDiagnostic, Runnable viewProgress, Consumer<Objectives.Lesson> openLesson) {
        this.tracker = tracker;
        this.diagnostic = diagnostic;
        this.lessons = lessons;
        this.startDiagnostic = startDiagnostic;
        this.viewProgress = viewProgress;
        this.openLesson = openLesson;
        setSpacing(16);
        setPadding(new Insets(22, 28, 22, 28));
        refresh();
    }

    /** Rebuilds the screen from the current diagnostic and progress. */
    void refresh() {
        getChildren().setAll(header(), banner(), lessonRow(), viewProgressRow());
    }

    private VBox header() {
        Label subtitle = Ui.css(new Label("Five lessons, in order. Take the diagnostic any time to unlock the tree activities."), "subtitle");
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
        Button start = Ui.primary(diagnostic.isComplete() ? "View diagnostic result"
                : diagnostic.answered() > 0 ? "Continue diagnostic (question " + (diagnostic.answered() + 1) + ")"
                : "Start diagnostic");
        start.setOnAction(e -> startDiagnostic.run());
        HBox banner = Ui.row(16, text, start);
        banner.getStyleClass().add("banner-diag");
        return banner;
    }

    private HBox lessonRow() {
        HBox row = new HBox(16);
        for (Objectives.Lesson lesson : Objectives.LESSONS) {
            VBox card = lessonCard(lesson);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    private VBox lessonCard(Objectives.Lesson lesson) {
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
        Button open = openButton(lesson);
        open.setMaxWidth(Double.MAX_VALUE);
        VBox card = Ui.card(id, Ui.title(lesson.title()), Ui.note(lesson.description()), objectives, open);
        card.getStyleClass().add("lesson-card");
        card.setMinWidth(150);
        return card;
    }

    /** The button that opens the lesson, labelled with where the student is in it. */
    private Button openButton(Objectives.Lesson lesson) {
        int pages = LessonBank.PAGES.get(lesson.id()).size();
        int position = Math.min(lessons.position(lesson.id()), pages);
        String label = position >= pages ? "Review lesson"
                : position > 0 ? "Continue (page " + (position + 1) + " of " + pages + ")"
                : "Open lesson";
        Button open = position > 0 && position < pages ? Ui.primary(label) : Ui.secondary(label);
        open.setOnAction(e -> openLesson.accept(lesson));
        return open;
    }

    private HBox viewProgressRow() {
        Button view = Ui.secondary("View progress");
        view.setOnAction(e -> viewProgress.run());
        return Ui.row(8, view);
    }
}
