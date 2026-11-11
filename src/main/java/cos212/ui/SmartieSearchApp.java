package cos212.ui;

import cos212.bst.BinarySearchTree;
import cos212.trace.TraceStep;
import cos212.trace.Traces;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * The COS212 Smartie Search window, laid out as in the team's wireframes: a navy top bar
 * with the three screens, the controls for Build, Search and Traverse beside the tree,
 * the trace and the next-step panel on the right, and the result along the bottom.
 * Built as a landscape window, with a minimum size of 1024 by 680 pixels.
 *
 * <p>Controls that belong to later increments are shown but disabled: In-order (I3),
 * the subtree choices and the hint (I4), and the other screens (I4 and later).
 */
public final class SmartieSearchApp extends Application {

    private static final String INVALID_VALUE = "Invalid value: Only whole numbers are allowed"
            + " (for example 45 or -3). The tree was not changed.";
    private static final String CARD_STYLE = "-fx-background-color: white; -fx-background-radius: 8;"
            + " -fx-border-color: #d9cbb8; -fx-border-radius: 8; -fx-padding: 12;";
    private static final String NOTE_STYLE = "-fx-font-size: 11px; -fx-text-fill: #555555;";
    private static final String ERROR_STYLE = "-fx-text-fill: #b42318; -fx-font-weight: bold;";
    private static final String RESULT_BORDER = "-fx-border-color: #0b3a8a; -fx-border-width: 0 0 0 4;";

    private final BinarySearchTree<Integer> tree = new BinarySearchTree<>();
    private final TreeCanvas canvas = new TreeCanvas();
    private final VBox tracePanel = new VBox(8);
    private final Label result = new Label("Result: no trace yet. Insert a value to start.");
    private final Label stepCaption = new Label();
    private final TextField insertField = new TextField();
    private final TextField searchField = new TextField();

    private List<TraceStep<Integer>> steps = List.of();
    private int stepIndex = -1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #FBF4EB;");
        root.setTop(topBar());
        root.setLeft(leftColumn());
        root.setCenter(centre());
        root.setRight(rightColumn());
        root.setBottom(resultBar());

        Scene scene = new Scene(root, 1280, 800);
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case RIGHT -> move(1);
                case LEFT -> move(-1);
                case ESCAPE -> reset();
                default -> {
                }
            }
        });

        stage.setTitle("COS212 Smartie Search");
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
        refresh();
    }

    /** The navy top bar with the app name and the three screens, as in the team's wireframes. */
    private HBox topBar() {
        Label title = new Label("COS212 BST tutor");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        Button lessons = tabButton("Lessons", false);
        Button activity = tabButton("Tree activity", true);
        Button progress = tabButton("Progress", false);
        HBox bar = new HBox(28, title, space, lessons, activity, progress);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 20, 0, 20));
        bar.setPrefHeight(52);
        bar.setStyle("-fx-background-color: #021B6E;");
        return bar;
    }

    private static Button tabButton(String text, boolean selected) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-padding: 4 2 4 2;"
                + (selected ? " -fx-border-color: transparent transparent #00AEEE transparent;"
                        + " -fx-border-width: 0 0 3 0;" : ""));
        button.setDisable(!selected);
        button.setTooltip(new javafx.scene.control.Tooltip(
                selected ? text : text + " is added in a later increment."));
        return button;
    }

    private VBox centre() {
        StackPane canvasPane = new StackPane(canvas);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        VBox.setVgrow(canvasPane, Priority.ALWAYS);
        Label heading = new Label("Tree canvas");
        heading.setStyle("-fx-font-weight: bold;");
        VBox box = new VBox(8, heading, canvasPane);
        box.setStyle(CARD_STYLE);
        box.setPadding(new Insets(12));
        VBox.setVgrow(box, Priority.ALWAYS);
        VBox wrapper = new VBox(box);
        wrapper.setPadding(new Insets(12, 6, 12, 6));
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    private VBox leftColumn() {
        insertField.setPromptText("Value");
        insertField.setOnAction(e -> insert());
        Button insert = new Button("Insert");
        insert.setOnAction(e -> insert());
        Button empty = new Button("Now empty tree");
        empty.setOnAction(e -> clearTree());
        Label note = new Label("Duplicate shows a message and leaves the tree unchanged.");
        note.setWrapText(true);
        note.setStyle(NOTE_STYLE);
        VBox build = card("Build", new Label("Value"), insertField, insert, note, empty);

        searchField.setPromptText("Value");
        searchField.setOnAction(e -> search());
        Button search = new Button("Search");
        search.setOnAction(e -> search());
        VBox searchCard = card("Search", new Label("Value"), searchField, search);

        Button inOrder = new Button("In-order");
        inOrder.setDisable(true);
        inOrder.setTooltip(new javafx.scene.control.Tooltip("Added in increment I3."));
        Label traverseNote = new Label("Shows every value in sorted order. Added in increment I3.");
        traverseNote.setWrapText(true);
        traverseNote.setStyle(NOTE_STYLE);
        VBox traverse = card("Traverse", inOrder, traverseNote);

        VBox column = new VBox(12, build, searchCard, traverse);
        column.setPadding(new Insets(12, 6, 12, 12));
        column.setPrefWidth(230);
        return column;
    }

    private VBox rightColumn() {
        Label traceHeading = new Label("Trace");
        traceHeading.setStyle("-fx-font-weight: bold;");
        tracePanel.getChildren().add(traceHeading);
        VBox trace = new VBox(8, tracePanel);
        trace.setStyle(CARD_STYLE);
        VBox.setVgrow(trace, Priority.ALWAYS);

        Label predictHeading = new Label("Predict the next step");
        predictHeading.setStyle("-fx-font-weight: bold;");
        Label question = new Label("Which subtree next? Available in increment I4.");
        question.setWrapText(true);
        question.setStyle(NOTE_STYLE);
        Button left = new Button("Left");
        Button right = new Button("Right");
        Button hint = new Button("Hint");
        left.setDisable(true);
        right.setDisable(true);
        hint.setDisable(true);
        HBox answers = new HBox(6, left, right, hint);
        Button previous = new Button("Previous");
        previous.setOnAction(e -> move(-1));
        Button next = new Button("Next");
        next.setOnAction(e -> move(1));
        Button reset = new Button("Reset");
        reset.setOnAction(e -> reset());
        HBox stepButtons = new HBox(6, previous, next, reset);
        stepCaption.setWrapText(true);
        stepCaption.setStyle(NOTE_STYLE);
        VBox predict = card("", predictHeading, question, answers, stepButtons, stepCaption);

        VBox column = new VBox(12, trace, predict);
        column.setPadding(new Insets(12, 12, 12, 6));
        column.setPrefWidth(300);
        return column;
    }

    private HBox resultBar() {
        result.setPadding(new Insets(8, 12, 8, 12));
        result.setStyle(RESULT_BORDER);
        result.setMaxWidth(Double.MAX_VALUE);
        HBox bar = new HBox(result);
        HBox.setHgrow(result, Priority.ALWAYS);
        bar.setPadding(new Insets(0, 12, 12, 12));
        return bar;
    }

    /** A white card with a bold heading (if any) and the given children. */
    private static VBox card(String heading, javafx.scene.Node... children) {
        VBox box = new VBox(8);
        if (!heading.isEmpty()) {
            Label title = new Label(heading);
            title.setStyle("-fx-font-weight: bold;");
            box.getChildren().add(title);
        }
        box.getChildren().addAll(children);
        box.setStyle(CARD_STYLE);
        return box;
    }

    private void insert() {
        Integer value = parseValue(insertField);
        if (value == null) {
            return;
        }
        steps = Traces.insert(tree, value);
        boolean added = tree.insert(value);
        stepIndex = 0;
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
        steps = Traces.search(tree, value);
        stepIndex = 0;
        setResult(tree.contains(value)
                ? value + " found after " + comparisons() + " comparisons."
                : value + " not found after " + comparisons() + " comparisons.", false);
        refresh();
    }

    private void clearTree() {
        tree.clear();
        steps = List.of();
        stepIndex = -1;
        setResult("The tree is now empty.", false);
        refresh();
    }

    private void move(int delta) {
        if (steps.isEmpty()) {
            return;
        }
        stepIndex = Math.max(0, Math.min(steps.size() - 1, stepIndex + delta));
        refresh();
    }

    private void reset() {
        steps = List.of();
        stepIndex = -1;
        setResult("Trace cleared. The tree is unchanged.", false);
        refresh();
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
        result.setStyle(error ? ERROR_STYLE + RESULT_BORDER : RESULT_BORDER);
    }

    /** The number of steps that compare the target with a stored value. */
    private int comparisons() {
        return (int) steps.stream().filter(step -> step.comparison() != null).count();
    }

    private void refresh() {
        Integer current = stepIndex >= 0 ? steps.get(stepIndex).value() : null;
        Set<Integer> visited = new HashSet<>();
        for (int i = 0; i < stepIndex; i++) {
            if (steps.get(i).value() != null) {
                visited.add(steps.get(i).value());
            }
        }
        canvas.show(tree.rootView(), current, visited);

        tracePanel.getChildren().remove(1, tracePanel.getChildren().size());
        if (steps.isEmpty()) {
            tracePanel.getChildren().add(new Label("No trace yet."));
            stepCaption.setText("");
            return;
        }
        for (int i = 0; i < steps.size(); i++) {
            TraceStep<Integer> step = steps.get(i);
            Label explanation = new Label((i + 1) + ". " + step.explanation());
            explanation.setWrapText(true);
            explanation.setMaxWidth(260);
            Label rule = new Label("Rule: " + step.rule());
            rule.setWrapText(true);
            rule.setMaxWidth(260);
            rule.setStyle(NOTE_STYLE);
            VBox entry = new VBox(2, explanation, rule);
            if (i == stepIndex) {
                explanation.setStyle("-fx-font-weight: bold;");
                entry.setStyle("-fx-border-color: #1d4ed8; -fx-border-width: 0 0 0 3;"
                        + " -fx-padding: 4 0 4 8;");
            }
            tracePanel.getChildren().add(entry);
        }
        stepCaption.setText("Step " + (stepIndex + 1) + " of " + steps.size()
                + ". Previous and Next move through the recorded trace and do not change the tree.");
    }
}
