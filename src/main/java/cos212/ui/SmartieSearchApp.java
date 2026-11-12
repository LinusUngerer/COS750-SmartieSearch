package cos212.ui;

import cos212.assess.Diagnostic;
import cos212.assess.LessonProgress;
import cos212.assess.ProgressStore;
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
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * The COS212 BST tutor window, with persistent navigation and the active screen in the
 * middle. The lessons are open from the start; the tree activity is locked until the diagnostic is passed, as FR8 requires.
 */
public final class SmartieSearchApp extends Application {

    private final ProgressTracker tracker = new ProgressTracker();
    private final Diagnostic diagnostic = new Diagnostic();
    private final LessonProgress lessons = new LessonProgress();
    private final ProgressStore store = new ProgressStore();
    private final BorderPane root = new BorderPane();
    private final Button lessonsTab = Ui.nav("Lessons", "lessons");
    private final Button diagnosticTab = Ui.nav("Diagnostic Gate", "diagnostic");
    private final Button treeTab = Ui.nav("Tree Activity", "tree");
    private final Button progressTab = Ui.nav("My Progress", "progress");
    private final Button settingsTab = Ui.nav("Settings", "settings");
    private final Settings settings = Settings.current();

    private TreeActivityScreen treeScreen;
    private LessonsScreen lessonsScreen;
    private ProgressScreen progressScreen;
    private DiagnosticScreen diagnosticScreen;
    private LessonScreen lessonScreen;
    private SettingsScreen settingsScreen;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        store.load(tracker, diagnostic, lessons);
        settings.load();
        tracker.addChangeListener(this::saveProgress);
        treeScreen = new TreeActivityScreen(tracker);
        lessonsScreen = new LessonsScreen(tracker, diagnostic, lessons, this::startDiagnostic,
                this::showProgress, this::openLesson);
        lessonScreen = new LessonScreen(tracker, lessons, this::saveProgress, this::showLessons,
                this::openLesson);
        progressScreen = new ProgressScreen(tracker, this::showLessons);
        settingsScreen = new SettingsScreen(settings);
        diagnosticScreen = new DiagnosticScreen(tracker, diagnostic, this::onDiagnosticChanged,
                this::showTree, this::showLessons);

        lessonsTab.setOnAction(e -> showLessons());
        diagnosticTab.setOnAction(e -> startDiagnostic());
        treeTab.setOnAction(e -> showTree());
        progressTab.setOnAction(e -> showProgress());
        settingsTab.setOnAction(e -> showSettings());

        root.getStyleClass().add("app");
        root.setTop(topBar());
        root.setLeft(sidebar());

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double windowWidth = Math.min(1366, bounds.getWidth() * 0.98);
        double windowHeight = Math.min(768, bounds.getHeight() * 0.96);
        Scene scene = new Scene(root, Math.max(900, windowWidth - 16), Math.max(560, windowHeight - 40));
        applyStyles(scene);
        settings.textSizeProperty().addListener((obs, oldValue, newValue) -> applyStyles(scene));
        settings.paletteProperty().addListener((obs, oldValue, newValue) -> applyStyles(scene));
        settings.strongFocusProperty().addListener((obs, oldValue, newValue) -> applyStyles(scene));
        addShortcuts(scene);

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
        VBox navigation = new VBox(10, lessonsTab, diagnosticTab, treeTab, progressTab, settingsTab);
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

    private void openLesson(Objectives.Lesson lesson) {
        select(lessonsTab);
        root.setCenter(scroll(lessonScreen));
        lessonScreen.open(lesson);
    }

    private void startDiagnostic() {
        select(diagnosticTab);
        root.setCenter(scroll(diagnosticScreen));
        diagnosticScreen.resume();
    }

    private void showTree() {
        if (!diagnostic.unlocked()) {
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

    private void showSettings() {
        select(settingsTab);
        root.setCenter(scroll(settingsScreen));
    }

    /** Rebuilds the stylesheet from the current text size and palette. */
    private void applyStyles(Scene scene) {
        scene.getStylesheets().setAll(Stylesheet.uri(settings));
    }

    /**
     * Keyboard shortcuts, so the whole app works without a mouse (NFR5): Ctrl+1 to Ctrl+5
     * open the five sections, A to D pick an answer on a question, and the arrow keys and
     * Escape step through a trace in the tree activity. Enter submits or moves on, because
     * the main button on each question screen is the default button.
     */
    private void addShortcuts(Scene scene) {
        Runnable[] sections = {this::showLessons, this::startDiagnostic, this::showTree, this::showProgress,
            this::showSettings};
        KeyCode[] digits = {KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4, KeyCode.DIGIT5};
        for (int i = 0; i < sections.length; i++) {
            scene.getAccelerators().put(new KeyCodeCombination(digits[i], KeyCombination.SHORTCUT_DOWN), sections[i]);
        }
        scene.setOnKeyPressed(e -> {
            Node content = root.getCenter() instanceof ScrollPane pane ? pane.getContent() : null;
            if (content == treeScreen && (e.getCode() == KeyCode.RIGHT
                    || e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.ESCAPE)) {
                treeScreen.handleKey(e.getCode());
                return;
            }
            boolean typing = scene.getFocusOwner() instanceof TextInputControl;
            boolean plain = !e.isShortcutDown() && !e.isAltDown() && !e.isShiftDown();
            int letter = switch (e.getCode()) {
                case A -> 0;
                case B -> 1;
                case C -> 2;
                case D -> 3;
                default -> -1;
            };
            if (!typing && plain && letter >= 0) {
                if (content == diagnosticScreen) {
                    diagnosticScreen.chooseOption(letter);
                    e.consume();
                } else if (content == lessonScreen) {
                    lessonScreen.chooseOption(letter);
                    e.consume();
                }
            }
        });
    }

    /** Keeps the tree activity unlocked only after the gate is passed, and refreshes the lessons banner. */
    private void onDiagnosticChanged() {
        treeTab.setDisable(!diagnostic.unlocked());
        if (lessonsScreen != null) {
            lessonsScreen.refresh();
        }
        saveProgress();
    }

    private void saveProgress() {
        store.save(tracker, diagnostic, lessons);
    }

    @Override
    public void stop() {
        saveProgress();
    }

    private void select(Button tab) {
        for (Button other : new Button[] {lessonsTab, diagnosticTab, treeTab, progressTab, settingsTab}) {
            other.getStyleClass().remove("tab-selected");
        }
        tab.getStyleClass().add("tab-selected");
    }

    private ScrollPane scroll(Node content) {
        ScrollPane pane = new ScrollPane(content);
        pane.setFitToWidth(true);
        pane.getStyleClass().add("content-scroll");
        // Tab moves straight from the navigation into the screen, not onto the scroll pane.
        pane.setFocusTraversable(false);
        BorderPane.setMargin(pane, new Insets(8, 8, 8, 0));
        return pane;
    }
}
