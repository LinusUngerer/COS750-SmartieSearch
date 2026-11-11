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
 * The COS212 Smartie Search window: the tree canvas on the left, the trace panel on the
 * right and the controls along the bottom. Built as a landscape window, with a minimum
 * size of 1024 by 680 pixels (see the UI section of the SE document).
 */
public final class SmartieSearchApp extends Application {

    private static final String INVALID_VALUE = "Invalid value: Only whole numbers are allowed"
            + " (for example 45 or -3). The tree was not changed.";
    private static final String ERROR_STYLE = "-fx-text-fill: #b42318; -fx-font-weight: bold;";
    private static final String NOTE_STYLE = "-fx-font-size: 11px; -fx-text-fill: #555555;";

    private final BinarySearchTree<Integer> tree = new BinarySearchTree<>();
    private final TreeCanvas canvas = new TreeCanvas();
    private final VBox tracePanel = new VBox(8);
    private final Label status = new Label("Insert a value to start building the tree.");
    private final Label stepCaption = new Label();
    private final TextField valueField = new TextField();

    private List<TraceStep<Integer>> steps = List.of();
    private int stepIndex = -1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        valueField.setPromptText("Value");
        valueField.setPrefColumnCount(8);
        valueField.setOnAction(e -> insert());
        Label valueLabel = new Label("Value (integers only)");
        valueLabel.setStyle(NOTE_STYLE);
        VBox valueBox = new VBox(2, valueLabel, valueField);

        Button insert = new Button("Insert");
        insert.setOnAction(e -> insert());
        Button search = new Button("Search");
        search.setOnAction(e -> search());
        Button previous = new Button("Previous");
        previous.setOnAction(e -> move(-1));
        Button next = new Button("Next");
        next.setOnAction(e -> move(1));
        Button reset = new Button("Reset");
        reset.setOnAction(e -> reset());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controls = new HBox(8, valueBox, insert, search, spacer, previous, next, reset);
        controls.setAlignment(Pos.BOTTOM_LEFT);

        stepCaption.setStyle(NOTE_STYLE);
        status.setPadding(new Insets(6, 10, 6, 10));
        status.setStyle("-fx-border-color: -fx-focus-color; -fx-border-width: 0 0 0 3;");
        VBox bottom = new VBox(8, status, controls, stepCaption);
        bottom.setPadding(new Insets(10));

        Label heading = new Label("Trace");
        heading.setStyle("-fx-font-weight: bold;");
        tracePanel.getChildren().add(heading);
        tracePanel.setPadding(new Insets(12));
        tracePanel.setPrefWidth(280);

        StackPane center = new StackPane(canvas);
        canvas.widthProperty().bind(center.widthProperty());
        canvas.heightProperty().bind(center.heightProperty());

        BorderPane root = new BorderPane(center);
        root.setRight(tracePanel);
        root.setBottom(bottom);

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

    private void insert() {
        Integer value = parseValue();
        if (value == null) {
            return;
        }
        steps = Traces.insert(tree, value);
        boolean added = tree.insert(value);
        stepIndex = 0;
        setStatus(added
                ? value + " added to the tree after " + comparisons() + " comparisons."
                : value + " is already in the tree, so nothing changed.", false);
        valueField.clear();
        refresh();
    }

    private void search() {
        Integer value = parseValue();
        if (value == null) {
            return;
        }
        steps = Traces.search(tree, value);
        stepIndex = 0;
        setStatus(tree.contains(value)
                ? value + " found after " + comparisons() + " comparisons."
                : value + " not found after " + comparisons() + " comparisons.", false);
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
        setStatus("Trace cleared. The tree is unchanged.", false);
        refresh();
    }

    private Integer parseValue() {
        try {
            return Integer.valueOf(valueField.getText().trim());
        } catch (NumberFormatException e) {
            setStatus(INVALID_VALUE, true);
            return null;
        }
    }

    private void setStatus(String text, boolean error) {
        status.setText(text);
        status.setStyle(error
                ? ERROR_STYLE + " -fx-border-color: #b42318; -fx-border-width: 0 0 0 3;"
                : "-fx-border-color: -fx-focus-color; -fx-border-width: 0 0 0 3;");
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
            explanation.setMaxWidth(250);
            Label rule = new Label("Rule: " + step.rule());
            rule.setWrapText(true);
            rule.setMaxWidth(250);
            rule.setStyle(NOTE_STYLE);
            VBox entry = new VBox(2, explanation, rule);
            if (i == stepIndex) {
                explanation.setStyle("-fx-font-weight: bold;");
                entry.setStyle("-fx-border-color: -fx-focus-color; -fx-border-width: 0 0 0 3;"
                        + " -fx-padding: 4 0 4 8;");
            }
            tracePanel.getChildren().add(entry);
        }
        stepCaption.setText("Step " + (stepIndex + 1) + " of " + steps.size()
                + ". Previous and Next move through the recorded trace and do not change the tree.");
    }
}
