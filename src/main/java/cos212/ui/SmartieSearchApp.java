package cos212.ui;

import cos212.assess.Diagnostic;
import cos212.assess.ProgressTracker;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * The COS212 BST tutor window, with persistent navigation and the active screen in the
 * middle. The tree activity is locked until the diagnostic is passed, as FR8 requires.
 */
public final class SmartieSearchApp extends Application {

    private final ProgressTracker tracker = new ProgressTracker();
    private final Diagnostic diagnostic = new Diagnostic();
    private final BorderPane root = new BorderPane();
    private final Button lessonsTab = Ui.nav("Lessons", "lessons");
    private final Button diagnosticTab = Ui.nav("Diagnostic Gate", "diagnostic");
    private final Button treeTab = Ui.nav("Tree Activity", "tree");
    private final Button progressTab = Ui.nav("My Progress", "progress");

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
        diagnosticTab.setOnAction(e -> startDiagnostic());
        treeTab.setOnAction(e -> showTree());
        progressTab.setOnAction(e -> showProgress());

        root.getStyleClass().add("app");
        root.setTop(topBar());
        root.setLeft(sidebar());

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double windowWidth = Math.min(1366, bounds.getWidth() * 0.98);
        double windowHeight = Math.min(768, bounds.getHeight() * 0.96);
        Scene scene = new Scene(root, Math.max(900, windowWidth - 16), Math.max(560, windowHeight - 40));
        scene.getStylesheets().add(getClass().getResource("app.css").toExternalForm());
        scene.setOnKeyPressed(e -> {
            if (root.getCenter() instanceof ScrollPane pane && pane.getContent() == treeScreen
                    && (e.getCode() == KeyCode.RIGHT
                    || e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.ESCAPE)) {
                treeScreen.handleKey(e.getCode());
            }
        });

        stage.setTitle("COS212 Smartie Search");
        stage.setMinWidth(Math.min(980, windowWidth));
        stage.setMinHeight(Math.min(620, windowHeight));
        stage.setWidth(windowWidth);
        stage.setHeight(windowHeight);
        stage.setX(bounds.getMinX() + (bounds.getWidth() - windowWidth) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - windowHeight) / 2);
        stage.setScene(scene);
        stage.show();
        showLessons();
    }

    private HBox topBar() {
        Label mark = Ui.css(new Label("🌳"), "brand-mark");
        Label title = Ui.css(new Label("SmartieSearch"), "top-title");
        HBox brand = Ui.row(12, mark, title);
        HBox bar = Ui.row(28, brand);
        bar.getStyleClass().add("top-bar");
        treeTab.setTooltip(new Tooltip("Pass the diagnostic check to unlock the tree activities."));
        onDiagnosticChanged();
        return bar;
    }

    private VBox sidebar() {
        VBox navigation = new VBox(10, lessonsTab, diagnosticTab, treeTab, progressTab);
        navigation.getStyleClass().add("sidebar-nav");
        VBox side = new VBox(navigation);
        side.getStyleClass().add("sidebar");
        return side;
    }

    private void showLessons() {
        select(lessonsTab);
        lessonsScreen.refresh();
        root.setCenter(scroll(lessonsScreen));
    }

    private void startDiagnostic() {
        select(diagnosticTab);
        root.setCenter(scroll(diagnosticScreen));
        diagnosticScreen.restart();
    }

    private void showTree() {
        if (!diagnostic.passed()) {
            return;
        }
        select(treeTab);
        root.setCenter(scroll(treeScreen));
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
        for (Button other : new Button[] {lessonsTab, diagnosticTab, treeTab, progressTab}) {
            other.getStyleClass().remove("tab-selected");
        }
        tab.getStyleClass().add("tab-selected");
    }

    private ScrollPane scroll(Node content) {
        ScrollPane pane = new ScrollPane(content);
        pane.setFitToWidth(true);
        pane.getStyleClass().add("content-scroll");
        BorderPane.setMargin(pane, new Insets(8, 8, 8, 0));
        return pane;
    }
}
