package cos212.ui;

import cos212.assess.LessonProgress;
import cos212.assess.ProgressTracker;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * One lesson, a page at a time, in the order Predict, Show, Try, Check. Explanation pages
 * walk through a worked example on the tree; question pages use the same answer cards as the
 * diagnostic. Try and Check answers are recorded in the progress record, and the page the
 * student reaches is saved, so opening the lesson again carries on from there.
 */
final class LessonScreen extends VBox {

    private final ProgressTracker tracker;
    private final LessonProgress lessons;
    private final Runnable onChange;
    private final Runnable backToLessons;
    private final Consumer<Objectives.Lesson> openLesson;

    private final TreeCanvas canvas = new TreeCanvas(false);
    private final Label lessonChip = Ui.css(new Label(), "chip", "chip-question");
    private final Label lessonTitle = Ui.css(new Label(), "page-title");
    private final HBox stages = Ui.row(8);
    private final Label pageCount = Ui.css(new Label(), "subtitle");
    private final Label pageTitle = Ui.css(new Label(), "card-title");
    private final Label pageText = Ui.css(new Label(), "question-text");
    private final Label error = Ui.css(new Label(), "error");
    private final QuestionView answers = new QuestionView(() -> error.setText(""));
    private final Button hintButton = Ui.secondary("Show hint");
    private final Label hintText = Ui.css(new Label(), "hint-text");
    private final Button action = Ui.css(new Button(), "btn-primary", "btn-wide");
    private final VBox rightColumn = new VBox(12);
    private final VBox pageArea = new VBox(14);
    private final VBox doneArea = new VBox(12);

    private Objectives.Lesson lesson;
    private List<LessonBank.Page> pages;
    private int index;
    private int hintsUsed;

    LessonScreen(ProgressTracker tracker, LessonProgress lessons, Runnable onChange, Runnable backToLessons,
            Consumer<Objectives.Lesson> openLesson) {
        this.tracker = tracker;
        this.lessons = lessons;
        this.onChange = onChange;
        this.backToLessons = backToLessons;
        this.openLesson = openLesson;
        setSpacing(14);
        setPadding(new Insets(22, 24, 22, 24));

        pageText.setWrapText(true);
        hintText.setWrapText(true);
        action.setMaxWidth(Double.MAX_VALUE);
        action.setOnAction(e -> act());
        hintButton.setOnAction(e -> showHint());

        VBox textCard = Ui.card(pageTitle, pageText);
        textCard.getStyleClass().add("question-card");

        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setMinHeight(240);
        canvasPane.setPrefHeight(340);
        VBox treePanel = Ui.card(Ui.title("Tree"), canvasPane);
        VBox.setVgrow(canvasPane, Priority.ALWAYS);
        pageArea.getChildren().setAll(textCard, new TwoColumns(treePanel, rightColumn));

        VBox heading = new VBox(6, Ui.row(10, lessonChip, lessonTitle), Ui.row(14, stages, pageCount));
        Button back = Ui.secondary("Back to lessons");
        back.setOnAction(e -> backToLessons.run());
        getChildren().setAll(heading, pageArea, doneArea, back);
    }

    /** Opens the lesson at the page the student reached last time. */
    void open(Objectives.Lesson lesson) {
        this.lesson = lesson;
        this.pages = LessonBank.PAGES.get(lesson.id());
        lessonChip.setText(lesson.id());
        lessonTitle.setText(lesson.title());
        index = Math.min(lessons.position(lesson.id()), pages.size());
        if (index >= pages.size()) {
            showDone();
        } else {
            showPage();
        }
    }

    private void showPage() {
        LessonBank.Page page = pages.get(index);
        doneArea.getChildren().clear();
        pageArea.setVisible(true);
        pageArea.setManaged(true);
        updateStages(page.stage());
        pageCount.setText("Page " + (index + 1) + " of " + pages.size());
        pageTitle.setText(page.stage().label + ": " + page.title());
        pageText.setText(page.text());
        canvas.setDepthShown("L4".equals(lesson.id()));
        canvas.show(page.tree(), page.current(), page.visited());
        error.setText("");
        hintsUsed = 0;

        if (page.isQuestion()) {
            if (page.fillAnswers() != null) {
                answers.showFillIn(page.fillAnswers().size());
            } else {
                answers.show(page.choices());
            }
            action.setText("Submit answer");
            rightColumn.getChildren().setAll(answers);
            if (page.hint() != null) {
                hintText.setText("");
                hintText.setVisible(false);
                hintText.setManaged(false);
                hintButton.setDisable(false);
                rightColumn.getChildren().add(new VBox(8, hintButton, hintText));
            }
            rightColumn.getChildren().addAll(action, error);
        } else {
            action.setText(index < pages.size() - 1 ? "Next  →" : "Finish lesson  →");
            Label note = Ui.note("Follow the highlighted path on the tree: the thick outline is the node being"
                    + " compared, and dashed nodes have already been passed.");
            rightColumn.getChildren().setAll(Ui.card(note), action);
        }
    }

    private void updateStages(LessonBank.Stage current) {
        stages.getChildren().clear();
        for (LessonBank.Stage stage : LessonBank.Stage.values()) {
            Label chip = Ui.css(new Label(stage.label), "stage-chip");
            if (stage == current) {
                chip.getStyleClass().add("stage-current");
            } else if (stage.ordinal() < current.ordinal()) {
                chip.getStyleClass().add("stage-done");
            }
            stages.getChildren().add(chip);
        }
    }

    private void act() {
        LessonBank.Page page = pages.get(index);
        if (page.isQuestion() && !answers.graded()) {
            submit(page);
        } else {
            advance();
        }
    }

    private void submit(LessonBank.Page page) {
        if (!answers.hasAnswer()) {
            error.setText(page.fillAnswers() == null ? "Choose one answer first." : "Complete every blank first.");
            return;
        }
        boolean correct = page.fillAnswers() == null
                ? answers.selected() == page.correct()
                : answers.matches(page.fillAnswers());
        String heading = correct ? "Great job!"
                : page.stage() == LessonBank.Stage.PREDICT ? "Not quite, and that's fine" : "Not quite";
        answers.grade(correct, heading, correct ? page.rightFeedback() : page.wrongFeedback(), null);
        hintButton.setDisable(true);
        error.setText("");
        lessons.setPosition(lesson.id(), index + 1);
        if (page.objective() != null) {
            tracker.record(page.objective(), correct, hintsUsed);
        }
        onChange.run();
        action.setText(index < pages.size() - 1 ? "Next  →" : "Finish lesson  →");
        action.requestFocus();
    }

    private void showHint() {
        LessonBank.Page page = pages.get(index);
        hintsUsed = 1;
        hintText.setText("Hint: " + page.hint());
        hintText.setVisible(true);
        hintText.setManaged(true);
        hintButton.setDisable(true);
    }

    private void advance() {
        index++;
        lessons.setPosition(lesson.id(), index);
        if (index >= pages.size()) {
            lessons.complete(lesson.id());
            onChange.run();
            showDone();
        } else {
            onChange.run();
            showPage();
        }
    }

    private void showDone() {
        pageArea.setVisible(false);
        pageArea.setManaged(false);
        stages.getChildren().clear();
        pageCount.setText("Lesson complete");

        VBox objectives = new VBox(8);
        for (String id : lesson.objectives()) {
            HBox row = Ui.row(10, Ui.css(new Label(Objectives.find(id).title()), "objective-title"),
                    Ui.chip(tracker.status(id)),
                    Ui.note(tracker.correctFirst(id) + " of " + tracker.attempts(id) + " correct first time"));
            objectives.getChildren().add(row);
        }

        Button review = Ui.secondary("Review from the start");
        review.setOnAction(e -> {
            lessons.setPosition(lesson.id(), 0);
            onChange.run();
            open(lesson);
        });
        HBox buttons = Ui.row(8);
        int position = Objectives.LESSONS.indexOf(lesson);
        if (position < Objectives.LESSONS.size() - 1) {
            Objectives.Lesson nextLesson = Objectives.LESSONS.get(position + 1);
            Button next = Ui.primary("Next lesson: " + nextLesson.id() + " " + nextLesson.title() + "  →");
            next.setOnAction(e -> openLesson.accept(nextLesson));
            buttons.getChildren().add(next);
        } else {
            Button done = Ui.primary("Back to lessons");
            done.setOnAction(e -> backToLessons.run());
            buttons.getChildren().add(done);
        }
        buttons.getChildren().add(review);

        VBox card = Ui.card(Ui.titleRow("You finished " + lesson.id() + ": " + lesson.title()),
                Ui.note("Your answers to the Try and Check questions are in your progress record."),
                objectives, buttons);
        card.setAlignment(Pos.TOP_LEFT);
        doneArea.getChildren().setAll(card);
    }
}
