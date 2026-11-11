package cos212.ui;

import cos212.assess.Diagnostic;
import cos212.assess.ProgressTracker;
import java.util.List;
import java.util.Set;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The diagnostic gate: one question at a time, a submit button, feedback that names the
 * rule, and a result that unlocks the tree activities or routes the student to the lessons
 * for the gaps.
 */
final class DiagnosticScreen extends VBox {

    private final List<DiagnosticBank.Item> items = DiagnosticBank.ITEMS;
    private final ProgressTracker tracker;
    private final Diagnostic diagnostic;
    private final Runnable onChange;
    private final Runnable openTree;
    private final Runnable backToLessons;

    private final TreeCanvas canvas = new TreeCanvas(false);
    private final Label question = Ui.css(new Label(), "card-title");
    private final Label progress = Ui.css(new Label(), "subtitle");
    private final Label status = Ui.css(new Label(), "diagnostic-status");
    private final ToggleGroup group = new ToggleGroup();
    private final VBox choices = new VBox(8);
    private final Button submit = Ui.primary("Submit answer");
    private final Label error = Ui.css(new Label(), "error");
    private final VBox questionArea = new VBox(12);
    private final VBox feedbackArea = new VBox(12);
    private final VBox resultArea = new VBox(12);

    private int index;
    private boolean answered;

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
        submit.setOnAction(e -> submit());
        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setPrefHeight(260);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());

        VBox treePanel = Ui.card(Ui.title("Question tree"),
                Ui.note("Look at the whole tree before choosing."), canvasPane);
        Region answerSpacer = new Region();
        VBox.setVgrow(answerSpacer, Priority.ALWAYS);
        submit.setMaxWidth(Double.MAX_VALUE);
        VBox answerPanel = Ui.card(Ui.title("Pick one answer"), choices, answerSpacer, submit, error);
        answerPanel.setMinWidth(310);
        treePanel.setMinWidth(290);
        HBox questionColumns = new HBox(14, treePanel, answerPanel);
        HBox.setHgrow(treePanel, Priority.ALWAYS);
        HBox.setHgrow(answerPanel, Priority.ALWAYS);
        treePanel.prefWidthProperty().bind(questionColumns.widthProperty().multiply(0.58).subtract(8));
        answerPanel.prefWidthProperty().bind(questionColumns.widthProperty().multiply(0.42).subtract(6));
        questionColumns.setMinHeight(350);
        VBox.setVgrow(questionColumns, Priority.ALWAYS);
        questionArea.getChildren().setAll(question, questionColumns);

        Button back = Ui.secondary("Back to lessons");
        back.setOnAction(e -> backToLessons.run());
        getChildren().setAll(header(), progress, questionArea, feedbackArea, resultArea, back);
        restart();
    }

    private HBox header() {
        VBox title = new VBox(4, Ui.pageTitle("Diagnostic Gate"),
                Ui.note("Check your understanding of the BST ordering rule."));
        VBox statusCard = Ui.card(Ui.note("Status"), status);
        statusCard.getStyleClass().add("diagnostic-status-card");
        HBox.setHgrow(title, Priority.ALWAYS);
        return Ui.row(14, title, statusCard);
    }

    /** Starts the diagnostic from the first item, with no answers recorded. */
    void restart() {
        diagnostic.reset();
        index = 0;
        answered = false;
        questionArea.setVisible(true);
        questionArea.setManaged(true);
        feedbackArea.getChildren().clear();
        resultArea.getChildren().clear();
        showItem();
        updateStatus();
        onChange.run();
    }

    private void showItem() {
        DiagnosticBank.Item item = items.get(index);
        question.setText(item.question());
        canvas.show(item.tree(), null, Set.of());
        group.getToggles().clear();
        choices.getChildren().clear();
        for (int i = 0; i < item.choices().size(); i++) {
            ToggleButton choice = Ui.css(new ToggleButton(
                    String.valueOf((char) ('A' + i)) + ". " + item.choices().get(i)), "choice");
            choice.setToggleGroup(group);
            choice.setUserData(i);
            choice.setMaxWidth(Double.MAX_VALUE);
            choice.setWrapText(true);
            choices.getChildren().add(choice);
        }
        choices.setDisable(false);
        submit.setDisable(false);
        error.setText("");
        feedbackArea.getChildren().clear();
        answered = false;
        updateProgress();
    }

    private void submit() {
        if (answered) {
            return;
        }
        Toggle selected = group.getSelectedToggle();
        if (selected == null) {
            error.setText("Choose one answer first.");
            return;
        }
        DiagnosticBank.Item item = items.get(index);
        boolean correct = (Integer) selected.getUserData() == item.correct();
        answered = true;
        diagnostic.recordAnswer(correct, item.lesson());
        tracker.record(item.objective(), correct, 0);
        choices.setDisable(true);
        submit.setDisable(true);
        error.setText("");
        feedbackArea.getChildren().setAll(feedbackCard(correct, item));
        updateProgress();
        updateStatus();
        onChange.run();
    }

    private VBox feedbackCard(boolean correct, DiagnosticBank.Item item) {
        Button next = Ui.primary(index < items.size() - 1 ? "Next question" : "See result");
        next.setOnAction(e -> next());
        Label body = Ui.css(new Label(correct ? item.correctFeedback() : item.wrongFeedback()), "feedback-body");
        body.setWrapText(true);
        VBox card = Ui.card(Ui.titleRow(correct ? "Correct" : "Not quite"), body, Ui.row(8, next));
        card.getStyleClass().add(correct ? "feedback-right" : "feedback-wrong");
        return card;
    }

    private void next() {
        if (!answered) {
            return;
        }
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
        feedbackArea.getChildren().clear();
        resultArea.getChildren().clear();
        String score = "You got " + diagnostic.correct() + " of " + Diagnostic.ITEM_COUNT + ".";
        if (diagnostic.passed()) {
            Button open = Ui.primary("Open tree activity");
            open.setOnAction(e -> openTree.run());
            resultArea.getChildren().add(Ui.card(Ui.titleRow("Gate passed"),
                    Ui.css(new Label(score + " The tree activities are now unlocked."), "result-text"), open));
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
        progress.setText("Question " + (index + 1) + " of " + items.size() + "    "
                + dots.toString().trim() + "    No timer. Choose one answer, then submit.");
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
