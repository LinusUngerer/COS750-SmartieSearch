package cos212.ui;

import cos212.assess.Diagnostic;
import cos212.assess.LessonProgress;
import cos212.assess.ProgressTracker;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * The lesson list: welcomes first-time learners with a spotlight on L0 ("Open
 * lesson"),
 * then shows the diagnostic check and the five lessons in order, L0 first.
 */
final class LessonsScreen extends VBox {

    private static final String SPOTLIGHT_STYLE = "-fx-border-color: #003380; -fx-border-width: 2.5; -fx-border-radius: 14; -fx-background-radius: 14;"
            + "-fx-effect: dropshadow(three-pass-box, rgba(0, 51, 128, 0.28), 16, 0.2, 0, 2);";

    private static final String FLASH_BTN_STYLE = "-fx-background-color: #FEF3C7; -fx-border-color: #D97706; -fx-border-width: 2.5;"
            + "-fx-text-fill: #78350F; -fx-font-weight: bold;"
            + "-fx-effect: dropshadow(three-pass-box, rgba(245, 158, 11, 0.65), 10, 0.25, 0, 0);";

    private final ProgressTracker tracker;
    private final Diagnostic diagnostic;
    private final Runnable startDiagnostic;
    private final Runnable viewProgress;
    private final LessonProgress lessons;
    private final Consumer<Objectives.Lesson> openLesson;
    private boolean showWelcome;

    LessonsScreen(ProgressTracker tracker, Diagnostic diagnostic, LessonProgress lessons,
            Runnable startDiagnostic, Runnable viewProgress, Consumer<Objectives.Lesson> openLesson) {
        this.tracker = tracker;
        this.diagnostic = diagnostic;
        this.lessons = lessons;
        this.startDiagnostic = startDiagnostic;
        this.viewProgress = viewProgress;
        this.openLesson = openLesson;
        this.showWelcome = isFirstTimeVisitor();
        setSpacing(16);
        setPadding(new Insets(22, 28, 22, 28));
        refresh();
    }

    private boolean isFirstTimeVisitor() {
        if (diagnostic.answered() > 0 || diagnostic.isComplete()) {
            return false;
        }
        for (Objectives.Lesson lesson : Objectives.LESSONS) {
            if (lessons.position(lesson.id()) > 0) {
                return false;
            }
        }
        return true;
    }

    /** Rebuilds the screen from the current diagnostic and progress. */
    void refresh() {
        HBox head = header();
        VBox welcome = welcomeBanner();
        HBox diag = banner();
        HBox row = lessonRow();
        HBox progress = viewProgressRow();

        if (showWelcome) {
            getChildren().setAll(head, welcome, diag, row, progress);
            for (Node n : new Node[] { head, diag, progress }) {
                n.setDisable(true);
                n.setOpacity(0.35);
            }
        } else {
            getChildren().setAll(head, diag, row, progress);
        }
    }

    private VBox welcomeBanner() {
        Label chip = Ui.css(new Label("Start your BST journey"), "chip", "chip-question");
        Label title = Ui.title("Ready to master Binary Search Trees?");
        Label body = Ui.note(
                "Begin your evaluation and interactive learning path right now—click 'Open lesson' on L0 (What is a BST?) below to jump in!");
        body.setWrapText(true);

        Button startL0 = Ui.primary("Start L0 now");
        startL0.setOnAction(e -> {
            showWelcome = false;
            openLesson.accept(Objectives.LESSONS.get(0));
        });

        Button dismiss = Ui.secondary("Got it, explore screen");
        dismiss.setOnAction(e -> {
            showWelcome = false;
            refresh();
        });

        VBox card = Ui.card(Ui.row(10, chip, title), body, Ui.row(8, startL0, dismiss));
        card.setStyle(SPOTLIGHT_STYLE);
        return card;
    }

    private HBox header() {
        Label subtitle = Ui.css(
                new Label("Five lessons, in order. Take the diagnostic any time to unlock the tree activities."),
                "subtitle");
        VBox copy = new VBox(4, Ui.pageTitle("Lessons"), subtitle);
        HBox.setHgrow(copy, Priority.ALWAYS);

        Button guideBtn = Ui.secondary("Show welcome guide");
        guideBtn.setMinWidth(Region.USE_PREF_SIZE);
        guideBtn.setOnAction(e -> {
            showWelcome = true;
            refresh();
        });
        return Ui.row(12, copy, guideBtn);
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
        for (int i = 0; i < Objectives.LESSONS.size(); i++) {
            Objectives.Lesson lesson = Objectives.LESSONS.get(i);
            boolean isFirstLesson = (i == 0);
            VBox card = lessonCard(lesson, isFirstLesson);
            if (showWelcome && !isFirstLesson) {
                card.setDisable(true);
                card.setOpacity(0.35);
            } else if (showWelcome && isFirstLesson) {
                card.setStyle(SPOTLIGHT_STYLE);
            }
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    private VBox lessonCard(Objectives.Lesson lesson, boolean highlightOpen) {
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
        if (showWelcome && highlightOpen) {
            open.setStyle(FLASH_BTN_STYLE);
        }
        open.setMaxWidth(Double.MAX_VALUE);
        VBox card = Ui.card(id, Ui.title(lesson.title()), Ui.note(lesson.description()), objectives, open);
        card.getStyleClass().add("lesson-card");
        card.setMinWidth(150);
        return card;
    }

    /**
     * The button that opens the lesson, labelled with where the student is in it.
     */
    private Button openButton(Objectives.Lesson lesson) {
        int pages = LessonBank.PAGES.get(lesson.id()).size();
        int position = Math.min(lessons.position(lesson.id()), pages);
        String label = position >= pages ? "Review lesson"
                : position > 0 ? "Continue (page " + (position + 1) + " of " + pages + ")"
                        : "Open lesson";
        Button open = position > 0 && position < pages ? Ui.primary(label) : Ui.secondary(label);
        open.setOnAction(e -> {
            showWelcome = false;
            openLesson.accept(lesson);
        });
        return open;
    }

    private HBox viewProgressRow() {
        Button view = Ui.secondary("View progress");
        view.setOnAction(e -> viewProgress.run());
        return Ui.row(8, view);
    }
}