package cos212.ui;

import cos212.assess.Diagnostic;
import cos212.assess.ProgressTracker;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * The COS212 BST tutor window: a navy top bar with the three screens, and the screen in the
 * middle. The tree activity is locked until the diagnostic is passed, as FR8 requires.
 * Built as a landscape window, with a minimum size of 1024 by 680 pixels.
 */
public final class SmartieSearchApp extends Application {

    private final ProgressTracker tracker = new ProgressTracker();
    private final Diagnostic diagnostic = new Diagnostic();
    private final BorderPane root = new BorderPane();
    private final Button lessonsTab = Ui.tab("Lessons");
    private final Button treeTab = Ui.tab("Tree activity");
    private final Button progressTab = Ui.tab("Progress");

    private TreeActivityScreen treeScreen;
    private LessonsScreen lessonsScreen;
    private ProgressScreen progressScreen;
    private DiagnosticScreen diagnosticScreen;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        treeScreen = new TreeActivityScreen(tracker);
        lessonsScreen = new LessonsScreen(tracker, diagnostic, this::startDiagnostic, this::showProgress);
        progressScreen = new ProgressScreen(tracker, this::showLessons);
        diagnosticScreen = new DiagnosticScreen(tracker, diagnostic, this::onDiagnosticChanged,
                this::showTree, this::showLessons);

        lessonsTab.setOnAction(e -> showLessons());
        treeTab.setOnAction(e -> showTree());
        progressTab.setOnAction(e -> showProgress());

        root.getStyleClass().add("app");
        root.setTop(topBar());

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("app.css").toExternalForm());
        scene.setOnKeyPressed(e -> {
            if (root.getCenter() == treeScreen && (e.getCode() == KeyCode.RIGHT
                    || e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.ESCAPE)) {
                treeScreen.handleKey(e.getCode());
            }
        });

        stage.setTitle("COS212 Smartie Search");
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
        showLessons();
    }

    private HBox topBar() {
        Label title = Ui.css(new Label("COS212 BST tutor"), "top-title");
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        treeTab.setTooltip(new Tooltip("Pass the diagnostic check to unlock the tree activities."));
        HBox bar = Ui.row(28, title, space, lessonsTab, treeTab, progressTab);
        bar.getStyleClass().add("top-bar");
        onDiagnosticChanged();
        return bar;
    }

    private void showLessons() {
        select(lessonsTab);
        lessonsScreen.refresh();
        root.setCenter(scroll(lessonsScreen));
    }

    private void startDiagnostic() {
        select(lessonsTab);
        root.setCenter(scroll(diagnosticScreen));
        diagnosticScreen.restart();
    }

    private void showTree() {
        if (!diagnostic.passed()) {
            return;
        }
        select(treeTab);
        root.setCenter(treeScreen);
    }

    private void showProgress() {
        select(progressTab);
        progressScreen.refresh();
        root.setCenter(scroll(progressScreen));
    }

    /** Keeps the tree activity unlocked only after the gate is passed, and refreshes the lessons. */
    private void onDiagnosticChanged() {
        treeTab.setDisable(!diagnostic.passed());
        if (lessonsScreen != null) {
            lessonsScreen.refresh();
        }
    }

    private void select(Button tab) {
        for (Button other : new Button[] {lessonsTab, treeTab, progressTab}) {
            other.getStyleClass().remove("tab-selected");
        }
        tab.getStyleClass().add("tab-selected");
    }

    private static ScrollPane scroll(Node content) {
        ScrollPane pane = new ScrollPane(content);
        pane.setFitToWidth(true);
        pane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return pane;
    }
}
