package cos212.ui;

import cos212.bst.BinarySearchTree;
import cos212.trace.TraceStep;
import cos212.trace.Traces;
import java.util.List;
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

    private final BinarySearchTree<Integer> tree = new BinarySearchTree<>();
    private final TreeCanvas canvas = new TreeCanvas();
    private final VBox tracePanel = new VBox(6);
    private final Label status = new Label("Insert a value to start building the tree.");
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
        HBox controls = new HBox(8, valueField, insert, search, spacer, previous, next, reset);
        controls.setAlignment(Pos.CENTER_LEFT);
        VBox bottom = new VBox(6, status, controls);
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
        status.setText(added
                ? value + " added to the tree."
                : value + " is already in the tree, so nothing changed.");
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
        status.setText(tree.contains(value)
                ? value + " is in the tree."
                : value + " is not in the tree.");
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
        refresh();
    }

    private Integer parseValue() {
        try {
            return Integer.valueOf(valueField.getText().trim());
        } catch (NumberFormatException e) {
            status.setText("Enter a whole number, for example 42.");
            return null;
        }
    }

    private void refresh() {
        Integer current = stepIndex >= 0 ? steps.get(stepIndex).value() : null;
        canvas.show(tree.rootView(), current);

        tracePanel.getChildren().remove(1, tracePanel.getChildren().size());
        if (steps.isEmpty()) {
            tracePanel.getChildren().add(new Label("No trace yet."));
            return;
        }
        for (int i = 0; i < steps.size(); i++) {
            Label line = new Label((i + 1) + ". " + steps.get(i).explanation());
            line.setWrapText(true);
            line.setMaxWidth(250);
            if (i == stepIndex) {
                line.setStyle("-fx-font-weight: bold; -fx-border-color: -fx-focus-color;"
                        + " -fx-border-width: 0 0 0 3; -fx-padding: 4 0 4 8;");
            }
            tracePanel.getChildren().add(line);
        }
    }
}
