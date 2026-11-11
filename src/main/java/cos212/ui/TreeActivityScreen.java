package cos212.ui;

import cos212.assess.ProgressTracker;
import cos212.bst.BinarySearchTree;
import cos212.trace.TraceStep;
import cos212.trace.TraceStep.Comparison;
import cos212.trace.TraceStep.Move;
import cos212.trace.Traces;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The tree activity. The tree and trace share the main row; build, search, traversal,
 * prediction and trace controls sit below it. Predictions and hints are recorded.
 */
final class TreeActivityScreen extends BorderPane {

    private static final String INVALID_VALUE = "Invalid value: Only whole numbers are allowed"
            + " (for example 45 or -3). The tree was not changed.";
    private static final int HINT_LIMIT = 3;

    private final BinarySearchTree<Integer> tree = new BinarySearchTree<>();
    private final ProgressTracker tracker;
    private final TreeCanvas canvas = new TreeCanvas(true);
    private final VBox traceList = new VBox(6);
    private final Label result = Ui.css(new Label("Result: no trace yet. Insert a value to start."), "result-bar");
    private final Label stepCaption = Ui.note("");
    private final Label traverseResult = Ui.mono("");
    private final TextField insertField = new TextField();
    private final TextField searchField = new TextField();
    private final Button left = Ui.secondary("Left");
    private final Button right = Ui.secondary("Right");
    private final Button hint = Ui.secondary("Hint (0 of 3)");
    private final Button previous = Ui.secondary("Previous");
    private final Button next = Ui.primary("Next");
    private final Button reset = Ui.secondary("Reset");
    private final Label hintText = Ui.note("");
    private final Label predictFeedback = Ui.note("");

    private List<TraceStep<Integer>> steps = List.of();
    private int stepIndex = -1;
    private boolean lastWasInsert;
    private int hintsShown;
    private final Set<Integer> answeredSteps = new HashSet<>();

    TreeActivityScreen(ProgressTracker tracker) {
        this.tracker = tracker;
        setPadding(new Insets(12));
        setTop(heading());
        setCenter(mainArea());
        setBottom(bottomArea());
        left.setOnAction(e -> predict(Move.LEFT));
        right.setOnAction(e -> predict(Move.RIGHT));
        hint.setOnAction(e -> showHint());
        previous.setOnAction(e -> move(-1));
        next.setOnAction(e -> move(1));
        reset.setOnAction(e -> reset());
        refresh();
    }

    /** Handles the arrow keys and Escape, as the trace controls do. */
    void handleKey(KeyCode code) {
        switch (code) {
            case RIGHT -> move(1);
            case LEFT -> move(-1);
            case ESCAPE -> reset();
            default -> {
            }
        }
    }

    private VBox heading() {
        VBox copy = new VBox(4, Ui.pageTitle("Tree Activity"),
                Ui.note("Create your own BST and follow along with the insertion, deletion, and traversal paths."));
        VBox header = Ui.card(copy);
        BorderPane.setMargin(header, new Insets(0, 0, 10, 0));
        return header;
    }

    private HBox mainArea() {
        StackPane pane = new StackPane(canvas);
        pane.setPrefHeight(320);
        canvas.widthProperty().bind(pane.widthProperty());
        canvas.heightProperty().bind(pane.heightProperty());
        VBox tree = Ui.card(Ui.note("Tree canvas. Current node: thick outline and the word current. Visited: dashed."),
                pane);
        VBox trace = Ui.card(Ui.titleRow("Trace"), traceList);
        VBox.setVgrow(pane, Priority.ALWAYS);
        VBox.setVgrow(traceList, Priority.ALWAYS);
        HBox.setHgrow(tree, Priority.ALWAYS);
        tree.setPrefWidth(0);
        trace.setPrefWidth(280);
        VBox.setVgrow(tree, Priority.ALWAYS);
        VBox.setVgrow(trace, Priority.ALWAYS);
        HBox main = new HBox(12, tree, trace);
        VBox.setVgrow(main, Priority.ALWAYS);
        return main;
    }

    private VBox bottomArea() {
        Button insert = Ui.primary("Insert");
        insert.setOnAction(e -> insert());
        insertField.setPromptText("Value");
        insertField.setOnAction(e -> insert());
        insertField.setPrefWidth(130);
        insertField.setMaxWidth(Double.MAX_VALUE);
        Button empty = Ui.secondary("New tree");
        empty.setOnAction(e -> clearTree());
        HBox buildInput = Ui.row(8, insertField, insert);
        VBox build = new VBox(8, Ui.note("Insert value"), buildInput, empty);
        build.setMinWidth(210);

        Button search = Ui.primary("Search");
        search.setOnAction(e -> search());
        searchField.setPromptText("Value");
        searchField.setOnAction(e -> search());
        searchField.setPrefWidth(130);
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox searchInput = Ui.row(8, searchField, search);
        VBox searchControls = new VBox(8, Ui.note("Search value"), searchInput);
        searchControls.setMinWidth(210);
        Button inOrder = Ui.secondary("In-order");
        inOrder.setOnAction(e -> inOrder());
        traverseResult.setWrapText(true);
        VBox controls = Ui.card(Ui.titleRow("Build, search and traverse"),
                Ui.row(10, build, searchControls),
                Ui.note("Duplicates leave the tree unchanged."),
                inOrder, traverseResult);
        HBox answers = new HBox(8, left, right, hint);
        HBox stepButtons = new HBox(8, previous, next, reset);
        VBox predict = Ui.card(Ui.titleRow("Predict the next step"),
                Ui.note("Which subtree next?"), answers, hintText, predictFeedback, stepButtons, stepCaption);
        controls.setMinWidth(440);
        controls.setPrefWidth(470);
        predict.setMinWidth(340);
        predict.setPrefWidth(370);
        FlowPane controlsRow = new FlowPane(12, 12, controls, predict);
        controlsRow.setPrefWrapLength(900);
        result.setMaxWidth(Double.MAX_VALUE);
        VBox bottom = new VBox(10, result, controlsRow);
        BorderPane.setMargin(bottom, new Insets(10, 0, 0, 0));
        return bottom;
    }

    private void insert() {
        Integer value = parseValue(insertField);
        if (value == null) {
            return;
        }
        startTrace(Traces.insert(tree, value), true);
        boolean added = tree.insert(value);
        setResult(added
                ? value + " added to the tree after " + comparisons() + " comparisons."
                : value + " is already in the tree, so nothing changed.", false);
        insertField.clear();
        refresh();
    }

    private void search() {
        Integer value = parseValue(searchField);
        if (value == null) {
            return;
        }
        startTrace(Traces.search(tree, value), false);
        setResult(tree.contains(value)
                ? value + " found after " + comparisons() + " comparisons."
                : value + " not found after " + comparisons() + " comparisons.", false);
        refresh();
    }

    private void clearTree() {
        tree.clear();
        clearTrace();
        setResult("The tree is now empty.", false);
        refresh();
    }

    private void inOrder() {
        traverseResult.setText(tree.size() == 0
                ? "The tree is empty."
                : tree.inOrder().toString().replace("[", "").replace("]", ""));
    }

    private void startTrace(List<TraceStep<Integer>> newSteps, boolean insert) {
        steps = newSteps;
        stepIndex = 0;
        lastWasInsert = insert;
        hintsShown = 0;
        answeredSteps.clear();
        hintText.setText("");
        predictFeedback.setText("");
    }

    private void clearTrace() {
        steps = List.of();
        stepIndex = -1;
        hintsShown = 0;
        answeredSteps.clear();
        hintText.setText("");
        predictFeedback.setText("");
    }

    private void move(int delta) {
        if (steps.isEmpty()) {
            return;
        }
        int target = Math.max(0, Math.min(steps.size() - 1, stepIndex + delta));
        if (target != stepIndex) {
            stepIndex = target;
            hintsShown = 0;
            hintText.setText("");
            predictFeedback.setText("");
        }
        refresh();
    }

    private void reset() {
        clearTrace();
        setResult("Trace cleared. The tree is unchanged.", false);
        refresh();
    }

    /** Checks the student's choice of subtree for the current step and records the first answer. */
    private void predict(Move chosen) {
        TraceStep<Integer> step = currentStep();
        if (!isSubtreeMove(step)) {
            return;
        }
        boolean correct = step.move() == chosen;
        if (answeredSteps.add(stepIndex)) {
            tracker.record(lastWasInsert ? "trace-insert" : "trace-search", correct, hintsShown);
        }
        String direction = step.move() == Move.LEFT ? "left" : "right";
        predictFeedback.setText((correct ? "Correct. " : "Not quite. ")
                + "The search goes " + direction + " because " + step.rule());
        refresh();
    }

    private void showHint() {
        TraceStep<Integer> step = currentStep();
        if (!isSubtreeMove(step) || hintsShown >= HINT_LIMIT) {
            return;
        }
        hintsShown++;
        hintText.setText(switch (hintsShown) {
            case 1 -> "Hint 1: " + step.rule();
            case 2 -> "Hint 2: the target is "
                    + (step.comparison() == Comparison.LESS ? "smaller" : "larger")
                    + " than " + step.value() + ".";
            default -> "Hint 3: the answer is " + (step.move() == Move.LEFT ? "left" : "right") + ".";
        });
        refresh();
    }

    private TraceStep<Integer> currentStep() {
        return stepIndex >= 0 && stepIndex < steps.size() ? steps.get(stepIndex) : null;
    }

    private static boolean isSubtreeMove(TraceStep<Integer> step) {
        return step != null && (step.move() == Move.LEFT || step.move() == Move.RIGHT);
    }

    private Integer parseValue(TextField field) {
        try {
            return Integer.valueOf(field.getText().trim());
        } catch (NumberFormatException e) {
            setResult(INVALID_VALUE, true);
            return null;
        }
    }

    private void setResult(String text, boolean error) {
        result.setText("Result: " + text);
        if (error) {
            result.getStyleClass().add("result-error");
        } else {
            result.getStyleClass().remove("result-error");
        }
    }

    /** The number of steps that compare the target with a stored value. */
    private int comparisons() {
        return (int) steps.stream().filter(step -> step.comparison() != null).count();
    }

    private void refresh() {
        TraceStep<Integer> current = currentStep();
        Set<Integer> visited = new HashSet<>();
        for (int i = 0; i < stepIndex; i++) {
            if (steps.get(i).value() != null) {
                visited.add(steps.get(i).value());
            }
        }
        canvas.show(tree.rootView(), current == null ? null : current.value(), visited);

        traceList.getChildren().clear();
        if (steps.isEmpty()) {
            traceList.getChildren().add(Ui.note("No trace yet."));
        }
        for (int i = 0; i < steps.size(); i++) {
            Label line = new Label((i + 1) + ". " + steps.get(i).explanation());
            line.setWrapText(true);
            line.setMaxWidth(250);
            line.getStyleClass().add("trace-step");
            if (i == stepIndex) {
                line.getStyleClass().add("trace-step-current");
            }
            traceList.getChildren().add(line);
        }
        stepCaption.setText(steps.isEmpty() ? ""
                : "Step " + (stepIndex + 1) + " of " + steps.size()
                        + ". Previous and Next move through the recorded trace and do not change the tree.");
        updateControls();
    }

    private void updateControls() {
        boolean subtree = isSubtreeMove(currentStep());
        left.setDisable(!subtree);
        right.setDisable(!subtree);
        hint.setDisable(!subtree || hintsShown >= HINT_LIMIT);
        hint.setText("Hint (" + hintsShown + " of " + HINT_LIMIT + ")");
        previous.setDisable(stepIndex <= 0);
        next.setDisable(steps.isEmpty() || stepIndex >= steps.size() - 1);
        reset.setDisable(steps.isEmpty());
    }
}
