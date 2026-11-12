package cos212.ui;

import cos212.assess.Diagnostic;
import cos212.assess.ProgressTracker;
import java.util.List;
import java.util.Set;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The diagnostic gate: one question at a time. The student picks an option card and submits;
 * the chosen card turns green or red, feedback that names the rule appears directly under it,
 * and the same button then moves on to the next question. The result unlocks the tree
 * activities or routes the student to the lessons for the gaps.
 */
final class DiagnosticScreen extends VBox {

    private final List<DiagnosticBank.Item> items = DiagnosticBank.ITEMS;
    private final ProgressTracker tracker;
    private final Diagnostic diagnostic;
    private final Runnable onChange;
    private final Runnable openTree;
    private final Runnable backToLessons;

    private final TreeCanvas canvas = new TreeCanvas(false);
    private final Label questionChip = Ui.css(new Label(), "chip", "chip-question");
    private final Label lessonChip = Ui.css(new Label(), "chip");
    private final Label question = Ui.css(new Label(), "question-text");
    private final Label progress = Ui.css(new Label(), "subtitle");
    private final Label status = Ui.css(new Label(), "diagnostic-status");
    private final Label error = Ui.css(new Label(), "error");
    private final QuestionView answers = new QuestionView(() -> error.setText(""));
    private final Button action = Ui.css(new Button("Submit answer"), "btn-primary", "btn-wide");
    private final Label correctCount = Ui.css(new Label(), "stats-text");
    private final Label incorrectCount = Ui.css(new Label(), "stats-text");
    private final Label remainingCount = Ui.css(new Label(), "stats-text");
    private final VBox questionArea = new VBox(14);
    private final VBox resultArea = new VBox(12);

    private int index;

    DiagnosticScreen(ProgressTracker tracker, Diagnostic diagnostic, Runnable onChange, Runnable openTree,
            Runnable backToLessons) {
        this.tracker = tracker;
        this.diagnostic = diagnostic;
        this.onChange = onChange;
        this.openTree = openTree;
        this.backToLessons = backToLessons;
        setSpacing(14);
        setPadding(new Insets(22, 24, 22, 24));

        question.setWrapText(true);
        progress.setWrapText(true);
        action.setOnAction(e -> {
            if (answers.graded()) {
                next();
            } else {
                submit();
            }
        });
        action.setMaxWidth(Double.MAX_VALUE);
        action.setDefaultButton(true);

        VBox questionCard = Ui.card(Ui.row(8, questionChip, lessonChip), question);
        questionCard.getStyleClass().add("question-card");

        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setMinHeight(220);
        canvasPane.setPrefHeight(300);
        VBox treePanel = Ui.card(Ui.title("Question tree"),
                Ui.note("Look at the whole tree before choosing."), canvasPane, Ui.treeDescription(canvas));
        VBox.setVgrow(canvasPane, Priority.ALWAYS);

        VBox answerColumn = new VBox(12, answers, action, statsBar(), error);
        questionArea.getChildren().setAll(questionCard, new TwoColumns(treePanel, answerColumn));

        Button back = Ui.secondary("Back to lessons");
        back.setOnAction(e -> backToLessons.run());
        getChildren().setAll(header(), progress, questionArea, resultArea, back);
        resume();
    }

    private HBox header() {
        VBox title = new VBox(4, Ui.pageTitle("Diagnostic Gate"),
                Ui.note("Check your understanding of the BST ordering rule."));
        VBox statusCard = Ui.card(Ui.note("Status"), status);
        statusCard.getStyleClass().add("diagnostic-status-card");
        HBox.setHgrow(title, Priority.ALWAYS);
        return Ui.row(14, title, statusCard);
    }

    private HBox statsBar() {
        HBox bar = Ui.row(14,
                stat("stats-dot-right", correctCount), divider(),
                stat("stats-dot-wrong", incorrectCount), divider(),
                stat("stats-dot-left", remainingCount));
        bar.setAlignment(Pos.CENTER);
        bar.getStyleClass().add("stats-bar");
        return bar;
    }

    private static HBox stat(String dotClass, Label text) {
        return Ui.row(6, Ui.css(new Label("●"), "stats-dot", dotClass), text);
    }

    private static Label divider() {
        return Ui.css(new Label("|"), "stats-divider");
    }

    /**
     * Shows the diagnostic where the student left off: the first unanswered item, or the
     * result if every item has been answered. Saved answers are kept.
     */
    void resume() {
        resultArea.getChildren().clear();
        if (diagnostic.isComplete()) {
            index = items.size() - 1;
            showResult();
        } else {
            index = diagnostic.answered();
            questionArea.setVisible(true);
            questionArea.setManaged(true);
            showItem();
        }
        updateStatus();
    }

    /** Picks answer {@code choice} (0 for A) from the keyboard. */
    void chooseOption(int choice) {
        answers.choose(choice);
    }

    /** Starts the diagnostic from the first item, with no answers recorded. */
    void restart() {
        diagnostic.reset();
        resume();
        onChange.run();
    }

    private void showItem() {
        DiagnosticBank.Item item = items.get(index);
        questionChip.setText("Question " + (index + 1) + " of " + items.size());
        lessonChip.setText("Lesson " + item.lesson());
        question.setText(item.question());
        canvas.show(item.tree(), null, Set.of());
        answers.show(item.choices());
        action.setText("Submit answer");
        error.setText("");
        updateProgress();
        updateStats();
    }

    private void submit() {
        if (answers.selected() < 0) {
            error.setText("Choose one answer first.");
            return;
        }
        DiagnosticBank.Item item = items.get(index);
        boolean correct = answers.selected() == item.correct();
        diagnostic.recordAnswer(correct, item.lesson());
        tracker.record(item.objective(), correct, 0);
        answers.grade(correct, correct ? "Great job!" : "Not quite",
                correct ? item.correctFeedback() : item.wrongFeedback(),
                correct ? null : "Revisit lesson " + item.lesson());

        action.setText(index < items.size() - 1 ? "Next question  →" : "See result  →");
        error.setText("");
        updateStats();
        updateStatus();
        onChange.run();
        action.requestFocus();
    }

    private void next() {
        if (index < items.size() - 1) {
            index++;
            showItem();
        } else {
            showResult();
        }
    }

    private void showResult() {
        questionArea.setVisible(false);
        questionArea.setManaged(false);
        progress.setText("Diagnostic complete. The pass mark is " + Diagnostic.PASS_MARK + " of "
                + Diagnostic.ITEM_COUNT + ".");
        resultArea.getChildren().clear();
        String score = "You got " + diagnostic.correct() + " of " + Diagnostic.ITEM_COUNT + ".";
        if (diagnostic.passed()) {
            Button open = Ui.primary("Open tree activity");
            open.setOnAction(e -> openTree.run());
            Button retake = Ui.secondary("Retake the diagnostic");
            retake.setOnAction(e -> restart());
            resultArea.getChildren().add(Ui.card(Ui.titleRow("Gate passed"),
                    Ui.css(new Label(score + " The tree activities are now unlocked."), "result-text"),
                    Ui.row(8, open, retake)));
        } else {
            Button retry = Ui.secondary("Retry the diagnostic");
            retry.setOnAction(e -> restart());
            String missed = String.join(", ", diagnostic.missedLessons());
            resultArea.getChildren().add(Ui.card(Ui.titleRow("Result when the gate is failed"),
                    Ui.css(new Label(score + " The ordering rule is not secure yet."), "result-text"),
                    Ui.note("Go to " + missed + " before you retry."),
                    Ui.row(8, retry)));
        }
        onChange.run();
    }

    private void updateProgress() {
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            dots.append(i <= index ? "● " : "○ ");
        }
        progress.setText(dots.toString().trim() + "    No timer. Choose one answer, then submit.");
    }

    private void updateStats() {
        int right = diagnostic.correct();
        int wrong = diagnostic.answered() - right;
        correctCount.setText(right + " Correct");
        incorrectCount.setText(wrong + " Incorrect");
        remainingCount.setText((Diagnostic.ITEM_COUNT - diagnostic.answered()) + " Remaining");
    }

    private void updateStatus() {
        status.setText(diagnostic.passed() ? "Passed"
                : diagnostic.isComplete() ? "Not passed"
                : diagnostic.answered() > 0 ? "In progress"
                : "Not started");
        status.getStyleClass().removeAll("status-passed", "status-progress", "status-unattempted");
        status.getStyleClass().add(diagnostic.passed() ? "status-passed"
                : diagnostic.answered() > 0 ? "status-progress" : "status-unattempted");
    }
}
