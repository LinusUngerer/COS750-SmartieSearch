package cos212.ui;

import cos212.assess.ProgressTracker;
import cos212.bst.BinarySearchTree;
import cos212.trace.TraceStep;
import cos212.trace.TraceStep.Comparison;
import cos212.trace.TraceStep.Move;
import cos212.trace.Traces;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * The tree activity. Keeps BorderPane as root so SmartieSearchApp's scroll
 * layout sizes dynamically, while guiding the student through the demo, a
 * 2-step "Your turn!" hand-off (1: New tree -> 2: Insert your first value),
 * Undo/Redo history, Delete Copy/Merge, and theoretical step prediction.
 */
final class TreeActivityScreen extends BorderPane {

    private static final String INVALID_VALUE = "Invalid value: Only whole numbers are allowed"
            + " (for example 45 or -3). The tree was not changed.";
    private static final int HINT_LIMIT = 3;

    private static final String[] GUIDE_TITLES = {
            "Watch how a Binary Search Tree works",
            "Build, Delete & Undo/Redo (Bottom left)",
            "Predict theoretical operations (Bottom right)",
            "Traverse & Customize View (Top & Bottom)"
    };

    private static final String[] GUIDE_TEXTS = {
            "Click 'Watch animated demo' to see a sample search path light up node-by-node while the prediction and step controls highlight in order.",
            "Enter a whole number to Insert, Search, or Delete (by Copy or Merge). Use 'New tree' to clear, or 'Undo' and 'Redo' to step through tree changes.",
            "Enter a value under 'Predict the next step' and choose Insert, Search, Delete Copy, or Delete Merge to test what would happen without changing the tree.",
            "Use 'Depth First' or 'Breadth First' to view traversal orders, and toggle 'Hide Key' or 'Hide Trace' anytime."
    };

    private enum FocusMode {
        NONE,
        GUIDE,
        DEMO_ANIMATING,
        FORCE_NEW_TREE,
        FORCE_FIRST_INSERT
    }

    private final BinarySearchTree<Integer> tree = new BinarySearchTree<>();
    private final ProgressTracker tracker;
    private final TreeCanvas canvas = new TreeCanvas(true);
    private final VBox traceList = new VBox(6);
    private final Label result = Ui.css(new Label("Result: no trace yet. Watch the demo or insert a value to start."),
            "result-bar");
    private final Label stepCaption = Ui.note("");
    private final Label traverseResult = Ui.mono("");
    private final TextField insertField = new TextField();
    private final TextField searchField = new TextField();
    private final TextField deleteField = new TextField();
    private final Button insertBtn = Ui.primary("Insert");
    private final Button deleteCopyBtn = Ui.primary("Delete Copy");
    private final Button deleteMergeBtn = Ui.primary("Delete Merge");
    private final Button hint = Ui.secondary("Hint (0 of 3)");
    private final Button previous = Ui.secondary("Previous");
    private final Button next = Ui.primary("Next");
    private final Button reset = Ui.secondary("Reset");
    private final Button emptyTreeBtn = Ui.secondary("New tree");
    private final Label hintText = Ui.note("");
    private final Label predictFeedback = Ui.note("");

    private static final String PREDICTION_NOTE = "Prediction only: the tree was not changed. ";
    private final TextField predictField = new TextField();
    private List<TraceStep<Integer>> predictSteps;
    private String predictCategory = "";
    private final FlowPane answerBox = new FlowPane(8, 8);
    private final Label predictPrompt = Ui.note("");
    private List<TraceStep<Integer>> builtFor;
    private int builtIndex = -2;

    /**
     * How many earlier tree states Undo can step back through (the T-1 of the
     * design). Raise it for more history.
     */
    private static final int HISTORY_DEPTH = 1;
    private final TreeHistory<Integer> history = new TreeHistory<>(HISTORY_DEPTH);
    private final Button undoButton = Ui.secondary("Undo");
    private final Button redoButton = Ui.secondary("Redo");

    // Walkthrough & spotlight cards
    private final VBox guideBanner = Ui.card();
    private final Label guideStepChip = Ui.css(new Label(), "chip", "chip-question");
    private final Label guideTitle = Ui.title("");
    private final Label guideBody = Ui.note("");
    private final Button guidePrevBtn = Ui.secondary("<");
    private final Button guideNextBtn = Ui.primary(">");

    // Interactive "Your turn" prompt inside controlsCard
    private final HBox yourTurnBanner = new HBox(8);
    private final Label yourTurnChip = Ui.css(new Label(), "chip", "chip-question");
    private final Label yourTurnText = Ui.note("");

    private VBox headerCard;
    private VBox treeCard;
    private VBox traceCard;
    private VBox controlsCard;
    private VBox predictCard;
    private VBox searchControlsBox;
    private HBox traversalsRow;
    private HBox historyRow;

    private int guideIndex = 0;
    private boolean demoHasRun = false;
    private Timeline demoTimeline;
    private FocusMode focusMode = FocusMode.NONE;

    private List<TraceStep<Integer>> steps = List.of();
    private int stepIndex = -1;
    private boolean lastWasInsert;
    private int hintsShown;
    private final Set<Integer> answeredSteps = new HashSet<>();

    TreeActivityScreen(ProgressTracker tracker) {
        this.tracker = tracker;
        setPadding(new Insets(18, 22, 18, 22));
        setTop(topSection());
        setCenter(mainArea());
        setBottom(bottomArea());

        hint.setOnAction(e -> showHint());
        previous.setOnAction(e -> {
            stopDemo();
            move(-1);
        });
        next.setOnAction(e -> {
            stopDemo();
            move(1);
        });
        reset.setOnAction(e -> {
            stopDemo();
            reset();
        });

        refresh();
        setFocusMode(FocusMode.GUIDE);
    }

    private VBox topSection() {
        VBox copy = new VBox(4, Ui.pageTitle("Tree Activity"),
                Ui.note("Create your own BST and follow along with insertion, search, deletion, prediction, and traversal paths."));
        HBox.setHgrow(copy, Priority.ALWAYS);

        Button showGuideBtn = Ui.secondary("Show walkthrough guide");
        showGuideBtn.setMinWidth(Region.USE_PREF_SIZE);
        showGuideBtn.setOnAction(e -> {
            stopDemo();
            guideIndex = 0;
            updateGuideText();
            setFocusMode(FocusMode.GUIDE);
        });

        headerCard = Ui.card(Ui.row(12, copy, showGuideBtn));

        Button watchDemo = Ui.primary("Watch animated demo");
        watchDemo.setOnAction(e -> runAnimatedDemo());

        Button dismissGuide = Ui.secondary("Hide guide");
        dismissGuide.setOnAction(e -> {
            stopDemo();
            if (demoHasRun && tree.size() > 0) {
                setFocusMode(FocusMode.FORCE_NEW_TREE);
            } else {
                setFocusMode(FocusMode.NONE);
            }
        });

        guidePrevBtn.setOnAction(e -> {
            if (guideIndex > 0) {
                guideIndex--;
                updateGuideText();
            }
        });

        guideNextBtn.setOnAction(e -> {
            if (guideIndex < GUIDE_TITLES.length - 1) {
                guideIndex++;
                updateGuideText();
            }
        });

        guideBody.setWrapText(true);
        guideBanner.getChildren().setAll(
                Ui.row(10, guideStepChip, guideTitle),
                guideBody,
                Ui.row(8, watchDemo, guidePrevBtn, guideNextBtn, dismissGuide));
        Ui.toggle(guideBanner, Ui.SPOTLIGHT, true);
        updateGuideText();

        VBox top = new VBox(10, headerCard, guideBanner);
        BorderPane.setMargin(top, new Insets(0, 0, 12, 0));
        return top;
    }

    private void updateGuideText() {
        guideStepChip.setText("Tip " + (guideIndex + 1) + " of " + GUIDE_TITLES.length);
        guideTitle.setText(GUIDE_TITLES[guideIndex]);
        guideBody.setText(GUIDE_TEXTS[guideIndex]);
        guidePrevBtn.setDisable(guideIndex == 0);
        guideNextBtn.setDisable(guideIndex >= GUIDE_TITLES.length - 1);
    }

    private void setFocusMode(FocusMode mode) {
        this.focusMode = mode;

        setCardDimmed(headerCard, false, false);
        setCardDimmed(treeCard, false, false);
        setCardDimmed(traceCard, false, false);
        setCardDimmed(controlsCard, false, false);
        setCardDimmed(predictCard, false, false);
        if (searchControlsBox != null) {
            searchControlsBox.setDisable(false);
            searchControlsBox.setOpacity(1.0);
        }
        if (traversalsRow != null) {
            traversalsRow.setDisable(false);
            traversalsRow.setOpacity(1.0);
        }
        if (historyRow != null) {
            historyRow.setDisable(false);
            historyRow.setOpacity(1.0);
        }
        insertField.setDisable(false);
        Ui.toggle(insertField, Ui.FLASH_FIELD, false);
        insertBtn.setDisable(false);
        Ui.toggle(insertBtn, Ui.FLASH, false);
        emptyTreeBtn.setDisable(false);
        Ui.toggle(emptyTreeBtn, Ui.FLASH, false);
        yourTurnBanner.setVisible(false);
        yourTurnBanner.setManaged(false);
        clearButtonFlashes();

        switch (mode) {
            case NONE -> {
                guideBanner.setVisible(false);
                guideBanner.setManaged(false);
            }
            case GUIDE -> {
                guideBanner.setVisible(true);
                guideBanner.setManaged(true);
                guideBanner.setDisable(false);
                guideBanner.setOpacity(1.0);
                setCardDimmed(headerCard, true, false);
                setCardDimmed(treeCard, true, false);
                setCardDimmed(traceCard, true, false);
                setCardDimmed(controlsCard, true, false);
                setCardDimmed(predictCard, true, false);
            }
            case DEMO_ANIMATING -> {
                guideBanner.setVisible(true);
                guideBanner.setManaged(true);
                setCardDimmed(headerCard, true, false);
                setCardDimmed(treeCard, false, true);
                setCardDimmed(traceCard, true, false);
                setCardDimmed(controlsCard, true, false);
                setCardDimmed(predictCard, false, true);
            }
            case FORCE_NEW_TREE -> {
                guideBanner.setVisible(false);
                guideBanner.setManaged(false);
                setCardDimmed(headerCard, true, false);
                setCardDimmed(treeCard, true, false);
                setCardDimmed(traceCard, true, false);
                setCardDimmed(predictCard, true, false);
                setCardDimmed(controlsCard, false, true);

                // Step 1 of 2: Force user to click 'New tree' first
                insertField.setDisable(true);
                insertBtn.setDisable(true);
                if (historyRow != null) {
                    historyRow.setDisable(true);
                    historyRow.setOpacity(0.35);
                }
                if (searchControlsBox != null) {
                    searchControlsBox.setDisable(true);
                    searchControlsBox.setOpacity(0.35);
                }
                if (traversalsRow != null) {
                    traversalsRow.setDisable(true);
                    traversalsRow.setOpacity(0.35);
                }
                Ui.toggle(emptyTreeBtn, Ui.FLASH, true);

                yourTurnChip.setText("Your turn • 1 / 2");
                yourTurnText.setText("First, click 'New tree' to clear the sample nodes.");
                yourTurnBanner.setVisible(true);
                yourTurnBanner.setManaged(true);
                setResult("Your turn (1 of 2): Click 'New tree' below to clear the sample tree.", false);
            }
            case FORCE_FIRST_INSERT -> {
                guideBanner.setVisible(false);
                guideBanner.setManaged(false);
                setCardDimmed(headerCard, true, false);
                setCardDimmed(treeCard, false, true);
                setCardDimmed(traceCard, true, false);
                setCardDimmed(predictCard, true, false);
                setCardDimmed(controlsCard, false, true);

                // Step 2 of 2: Now spotlight the Insert input & button
                emptyTreeBtn.setDisable(true);
                if (historyRow != null) {
                    historyRow.setDisable(true);
                    historyRow.setOpacity(0.35);
                }
                if (searchControlsBox != null) {
                    searchControlsBox.setDisable(true);
                    searchControlsBox.setOpacity(0.35);
                }
                if (traversalsRow != null) {
                    traversalsRow.setDisable(true);
                    traversalsRow.setOpacity(0.35);
                }
                insertField.setDisable(false);
                Ui.toggle(insertField, Ui.FLASH_FIELD, true);
                insertBtn.setDisable(false);
                Ui.toggle(insertBtn, Ui.FLASH, true);
                insertField.requestFocus();

                yourTurnChip.setText("Your turn • 2 / 2");
                yourTurnText.setText("Now type any whole number and click 'Insert' — there you go!");
                yourTurnBanner.setVisible(true);
                yourTurnBanner.setManaged(true);
                setResult("Your turn (2 of 2): Type a whole number and click 'Insert' — there you go!", false);
            }
        }
        updateControls();
    }

    private static void setCardDimmed(Node card, boolean dimmed, boolean spotlight) {
        if (card == null) {
            return;
        }
        card.setDisable(dimmed);
        card.setOpacity(dimmed ? 0.32 : 1.0);
        Ui.toggle(card, Ui.SPOTLIGHT, spotlight);
    }

    /**
     * Builds a sample BST and animates a theoretical search path with sequential
     * control button flashes.
     */
    private void runAnimatedDemo() {
        stopDemo();
        demoHasRun = true;
        tree.clear();
        for (int v : new int[] { 50, 30, 70, 40 }) {
            tree.insert(v);
        }
        startPredictTrace(Traces.search(tree, 40), "predict-search");
        setFocusMode(FocusMode.DEMO_ANIMATING);
        setResult("Demo running: Theoretical search for 40 in sample tree [50, 30, 70, 40]...", false);
        refresh();
        flashControlsForStep(0);

        demoTimeline = new Timeline(new KeyFrame(Duration.seconds(2.8), e -> {
            if (stepIndex < steps.size() - 1) {
                move(1);
                flashControlsForStep(stepIndex);
            } else {
                clearButtonFlashes();
                setResult("Demo complete! 40 found after " + comparisons()
                        + " comparisons. Click 'Hide guide' for your turn!", false);
                stopDemo();
                setFocusMode(FocusMode.GUIDE);
            }
        }));
        demoTimeline.setCycleCount(steps.size());
        demoTimeline.play();
    }

    private void flashControlsForStep(int currentIdx) {
        clearButtonFlashes();
        List<Button> sequence = new ArrayList<>();
        for (Node n : answerBox.getChildren()) {
            if (n instanceof Button b) {
                sequence.add(b);
            }
        }
        sequence.add(previous);
        sequence.add(next);
        sequence.add(reset);
        if (sequence.isEmpty()) {
            return;
        }
        int first = (currentIdx * 2) % sequence.size();
        int second = (currentIdx * 2 + 1) % sequence.size();
        Ui.toggle(sequence.get(first), Ui.FLASH, true);
        Ui.toggle(sequence.get(second), Ui.FLASH, true);
    }

    private void clearButtonFlashes() {
        for (Node n : answerBox.getChildren()) {
            if (n instanceof Button b) {
                Ui.toggle(b, Ui.FLASH, false);
            }
        }
        for (Button b : new Button[] { hint, previous, next, reset }) {
            Ui.toggle(b, Ui.FLASH, false);
        }
    }

    private void stopDemo() {
        if (demoTimeline != null) {
            demoTimeline.stop();
            demoTimeline = null;
        }
        clearButtonFlashes();
    }

    /** Handles the arrow keys and Escape, as the trace controls do. */
    void handleKey(KeyCode code) {
        if (focusMode != FocusMode.NONE) {
            return;
        }
        switch (code) {
            case RIGHT -> move(1);
            case LEFT -> move(-1);
            case ESCAPE -> reset();
            default -> {
            }
        }
    }

    private HBox mainArea() {
        StackPane pane = new StackPane(canvas);
        pane.setMinHeight(260);
        pane.setPrefHeight(310);

        traceCard = Ui.card(Ui.titleRow("Trace"), traceList);

        Button keyToggle = Ui.secondary("Hide Key");
        keyToggle.setOnAction(e -> {
            boolean show = !canvas.isKeyShown();
            canvas.setKeyShown(show);
            keyToggle.setText(show ? "Hide Key" : "Show Key");
        });

        Button traceToggle = Ui.secondary("Hide Trace");
        traceToggle.setOnAction(e -> {
            boolean show = !traceCard.isVisible();
            traceCard.setVisible(show);
            traceCard.setManaged(show);
            traceToggle.setText(show ? "Hide Trace" : "Show Trace");
        });
        keyToggle.setMinWidth(Region.USE_PREF_SIZE);
        traceToggle.setMinWidth(Region.USE_PREF_SIZE);
        HBox toggles = new HBox(8, keyToggle, traceToggle);

        treeCard = Ui.card(
                Ui.note("Tree canvas. Current node: thick outline and the word current. Visited: dashed."),
                toggles, pane, Ui.treeDescription(canvas));

        VBox.setVgrow(pane, Priority.ALWAYS);
        VBox.setVgrow(traceList, Priority.ALWAYS);
        HBox.setHgrow(treeCard, Priority.ALWAYS);
        traceCard.setPrefWidth(280);
        traceCard.setMinWidth(240);
        VBox.setVgrow(treeCard, Priority.ALWAYS);
        VBox.setVgrow(traceCard, Priority.ALWAYS);
        HBox main = new HBox(12, treeCard, traceCard);
        VBox.setVgrow(main, Priority.ALWAYS);
        return main;
    }

    private VBox bottomArea() {
        insertBtn.setOnAction(e -> insert());
        insertBtn.setMinWidth(Region.USE_PREF_SIZE);
        insertField.setPromptText("Value");
        insertField.setOnAction(e -> insert());
        insertField.setPrefWidth(110);
        insertField.setMinWidth(60);
        HBox.setHgrow(insertField, Priority.ALWAYS);

        emptyTreeBtn.setOnAction(e -> {
            clearTree();
            if (focusMode == FocusMode.FORCE_NEW_TREE) {
                setFocusMode(FocusMode.FORCE_FIRST_INSERT);
            }
        });

        undoButton.setOnAction(e -> undo());
        redoButton.setOnAction(e -> redo());
        undoButton.setMinWidth(Region.USE_PREF_SIZE);
        redoButton.setMinWidth(Region.USE_PREF_SIZE);
        historyRow = Ui.row(8, undoButton, redoButton);

        HBox buildInput = Ui.row(8, insertField, insertBtn);
        VBox build = new VBox(8, Ui.note("Insert value"), buildInput, emptyTreeBtn, historyRow);
        build.setMinWidth(Region.USE_PREF_SIZE);

        Button search = Ui.primary("Search");
        search.setOnAction(e -> search());
        search.setMinWidth(Region.USE_PREF_SIZE);
        searchField.setPromptText("Value");
        searchField.setOnAction(e -> search());
        searchField.setPrefWidth(110);
        searchField.setMinWidth(60);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        HBox searchInput = Ui.row(8, searchField, search);

        deleteField.setPromptText("Value");
        deleteField.setPrefWidth(110);
        deleteField.setMinWidth(60);
        deleteCopyBtn.setOnAction(e -> deleteByCopying());
        deleteCopyBtn.setMinWidth(Region.USE_PREF_SIZE);
        deleteMergeBtn.setOnAction(e -> deleteByMerging());
        deleteMergeBtn.setMinWidth(Region.USE_PREF_SIZE);

        searchControlsBox = new VBox(8,
                Ui.note("Search value"), searchInput,
                Ui.note("Delete value"), deleteField, Ui.row(8, deleteCopyBtn, deleteMergeBtn));
        searchControlsBox.setMinWidth(Region.USE_PREF_SIZE);

        Button depthFirst = Ui.secondary("Depth First");
        depthFirst.setOnAction(e -> inOrder());
        depthFirst.setMinWidth(Region.USE_PREF_SIZE);
        Button breadthFirst = Ui.secondary("Breadth First");
        breadthFirst.setOnAction(e -> BFT());
        breadthFirst.setMinWidth(Region.USE_PREF_SIZE);
        traversalsRow = Ui.row(8, depthFirst, breadthFirst);

        traverseResult.setWrapText(true);
        traverseResult.setMaxWidth(420);

        yourTurnText.setWrapText(true);
        yourTurnBanner.getChildren().setAll(yourTurnChip, yourTurnText);
        yourTurnBanner.setAlignment(Pos.CENTER_LEFT);
        yourTurnBanner.setPadding(new Insets(8, 10, 8, 10));
        yourTurnBanner.getStyleClass().add("your-turn-banner");
        yourTurnBanner.setVisible(false);
        yourTurnBanner.setManaged(false);

        controlsCard = Ui.card(Ui.titleRow("Build, search and traverse"),
                yourTurnBanner,
                Ui.row(10, build, searchControlsBox),
                Ui.note("Duplicates leave the tree unchanged."),
                traversalsRow, traverseResult);

        HBox answers = new HBox(8, answerBox);
        HBox stepButtons = new HBox(8, previous, next, reset);
        predictCard = Ui.card(Ui.titleRow("Predict the next step"),
                predictPrompt, answers, hintText, predictFeedback, stepButtons, stepCaption);

        controlsCard.setMinWidth(Region.USE_PREF_SIZE);
        predictCard.setMinWidth(Region.USE_PREF_SIZE);
        predictCard.setMaxWidth(420);
        predictField.setPromptText("Value");
        predictField.setPrefWidth(110);
        predictField.setMinWidth(60);

        Button pInsert = Ui.secondary("Insert");
        pInsert.setOnAction(e -> predictInsert());
        pInsert.setAccessibleText("Predict insert");
        Button pSearch = Ui.secondary("Search");
        pSearch.setOnAction(e -> predictSearch());
        pSearch.setAccessibleText("Predict search");
        Button pDeleteCopy = Ui.secondary("Delete Copy");
        pDeleteCopy.setOnAction(e -> predictDelete(true));
        pDeleteCopy.setAccessibleText("Predict delete by copying");
        Button pDeleteMerge = Ui.secondary("Delete Merge");
        pDeleteMerge.setOnAction(e -> predictDelete(false));
        pDeleteMerge.setAccessibleText("Predict delete by merging");
        for (Button b : List.of(pInsert, pSearch, pDeleteCopy, pDeleteMerge)) {
            b.setMinWidth(Region.USE_PREF_SIZE);
        }
        FlowPane predictOps = new FlowPane(8, 8, pInsert, pSearch, pDeleteCopy, pDeleteMerge);
        predictOps.setPrefWrapLength(360);
        predictCard.getChildren().addAll(1, List.of(
                Ui.note("Value to predict (the tree is never changed here)"), predictField, predictOps));
        answerBox.setPrefWrapLength(360);
        HBox.setHgrow(answerBox, Priority.ALWAYS);

        FlowPane controlsRow = new FlowPane(12, 12, controlsCard, predictCard);
        controlsRow.setPrefWrapLength(1000);
        result.setMaxWidth(Double.MAX_VALUE);
        VBox bottom = new VBox(10, result, controlsRow);
        BorderPane.setMargin(bottom, new Insets(10, 0, 0, 0));
        return bottom;
    }

    private void insert() {
        if (focusMode == FocusMode.FORCE_NEW_TREE) {
            return;
        }
        stopDemo();
        Integer value = parseValue(insertField);
        if (value == null) {
            return;
        }
        if (focusMode == FocusMode.FORCE_FIRST_INSERT) {
            setFocusMode(FocusMode.NONE);
        }
        startTrace(Traces.insert(tree, value), true);

        if (!tree.contains(value)) {
            history.record(tree, "Insert " + value);
        }
        boolean added = tree.insert(value);

        setResult(added
                ? value + " added to the tree after " + comparisons() + " comparisons. There you go!"
                : value + " is already in the tree, so nothing changed.", false);
        insertField.clear();
        refresh();
    }

    private void search() {
        stopDemo();
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

    private void startPredictTrace(List<TraceStep<Integer>> run, String category) {
        startTrace(run, false);
        predictSteps = run;
        predictCategory = category;
    }

    private void predictInsert() {
        stopDemo();
        Integer value = parseValue(predictField);
        if (value == null) {
            return;
        }
        startPredictTrace(Traces.insert(tree, value), "predict-insert");
        setResult(PREDICTION_NOTE + (tree.contains(value)
                ? "Inserting " + value + " would change nothing, because it is already in the tree."
                : "Inserting " + value + " would add it after " + comparisons() + " comparisons."), false);
        refresh();
    }

    private void predictSearch() {
        stopDemo();
        Integer value = parseValue(predictField);
        if (value == null) {
            return;
        }
        startPredictTrace(Traces.search(tree, value), "predict-search");
        setResult(PREDICTION_NOTE + (tree.contains(value)
                ? "Searching for " + value + " would find it after " + comparisons() + " comparisons."
                : "Searching for " + value + " would not find it after " + comparisons() + " comparisons."), false);
        refresh();
    }

    private void predictDelete(boolean copying) {
        stopDemo();
        Integer value = parseValue(predictField);
        if (value == null) {
            return;
        }
        startPredictTrace(copying ? Traces.deleteByCopying(tree, value) : Traces.deleteByMerging(tree, value),
                copying ? "predict-delete-copy" : "predict-delete-merge");
        String method = copying ? "copying" : "merging";
        setResult(PREDICTION_NOTE + (tree.contains(value)
                ? "Deleting " + value + " by " + method + " would remove it after " + comparisons() + " comparisons."
                : value + " is not in the tree, so deleting it by " + method + " would change nothing."), false);
        refresh();
    }

    /**
     * The moves the student chooses between at a step. They depend on the step and
     * what came before it.
     */
    private List<Move> choicesFor(int index) {
        TraceStep<Integer> step = steps.get(index);
        if (index > 0 && steps.get(index - 1).move() == Move.FOUND) {
            return List.of(Move.DELETE_NODE, Move.FIND_REPLACEMENT, Move.MERGE_REPLACE, Move.RIGHT_REPLACE);
        }
        return switch (step.move()) {
            case LEFT, RIGHT, FOUND, DUPLICATE ->
                List.of(Move.LEFT, Move.RIGHT, isInsertTrace() ? Move.DUPLICATE : Move.FOUND);
            case ATTACH_LEFT, ATTACH_RIGHT, ATTACH_ROOT -> step.value() == null
                    ? List.of(Move.ATTACH_ROOT, Move.ATTACH_LEFT, Move.ATTACH_RIGHT)
                    : List.of(Move.ATTACH_LEFT, Move.ATTACH_RIGHT);
            case MISSING -> List.of(Move.FOUND, Move.MISSING);
            case SEEK_LEFT -> List.of(Move.SEEK_LEFT, Move.RIGHT);
            case SEEK_RIGHT, REPLACEMENT_FOUND -> List.of(Move.SEEK_RIGHT, Move.REPLACEMENT_FOUND);
            case COPY_VALUE, REMOVE_REPLACEMENT -> List.of(Move.COPY_VALUE, Move.REMOVE_REPLACEMENT);
            case MERGE_ATTACH, MERGE_REPLACE -> List.of(Move.MERGE_ATTACH, Move.MERGE_REPLACE);
            case DELETE_NODE, FIND_REPLACEMENT, RIGHT_REPLACE ->
                List.of(Move.DELETE_NODE, Move.FIND_REPLACEMENT, Move.MERGE_REPLACE, Move.RIGHT_REPLACE);
        };
    }

    private boolean isInsertTrace() {
        return steps.stream().anyMatch(s -> switch (s.move()) {
            case ATTACH_LEFT, ATTACH_RIGHT, ATTACH_ROOT, DUPLICATE -> true;
            default -> false;
        });
    }

    private static String label(Move move) {
        return switch (move) {
            case LEFT, SEEK_LEFT -> "Left";
            case RIGHT, SEEK_RIGHT -> "Right";
            case FOUND -> "Found";
            case MISSING -> "Not found";
            case DUPLICATE -> "Duplicate";
            case ATTACH_LEFT -> "Insert Left";
            case ATTACH_RIGHT -> "Insert Right";
            case ATTACH_ROOT -> "Insert Root";
            case DELETE_NODE -> "Remove Node";
            case FIND_REPLACEMENT -> "Find Replacement";
            case RIGHT_REPLACE -> "Right Subtree Replaces";
            case REPLACEMENT_FOUND -> "Replacement Found";
            case COPY_VALUE -> "Copy Value";
            case REMOVE_REPLACEMENT -> "Remove Replacement";
            case MERGE_ATTACH -> "Attach Right Subtree";
            case MERGE_REPLACE -> "Left Subtree Replaces";
        };
    }

    /** A wrong option to rule out in hint 2. */
    private Move wrongChoice(TraceStep<Integer> step) {
        for (Move move : choicesFor(stepIndex)) {
            if (move != step.move()) {
                return move;
            }
        }
        return step.move();
    }

    private void deleteByCopying() {
        stopDemo();
        Integer value = parseValue(deleteField);
        if (value == null) {
            return;
        }
        startTrace(Traces.deleteByCopying(tree, value), false);

        if (tree.contains(value)) {
            history.record(tree, "Delete Copy " + value);
        }
        boolean removed = tree.deleteByCopying(value);

        setResult(removed
                ? value + " deleted by copying after " + comparisons() + " comparisons."
                : value + " not found after " + comparisons() + " comparisons, so nothing changed.", false);
        deleteField.clear();
        refresh();
    }

    private void deleteByMerging() {
        stopDemo();
        Integer value = parseValue(deleteField);
        if (value == null) {
            return;
        }
        startTrace(Traces.deleteByMerging(tree, value), false);

        if (tree.contains(value)) {
            history.record(tree, "Delete Merge " + value);
        }
        boolean removed = tree.deleteByMerging(value);

        setResult(removed
                ? value + " deleted by merging after " + comparisons() + " comparisons."
                : value + " not found after " + comparisons() + " comparisons, so nothing changed.", false);
        deleteField.clear();
        refresh();
    }

    private void clearTree() {
        stopDemo();
        if (tree.size() > 0) {
            history.record(tree, "New tree");
        }
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

    private void BFT() {
        traverseResult.setText(tree.size() == 0
                ? "The tree is empty."
                : tree.breadthFirst().toString().replace("[", "").replace("]", ""));
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

    /**
     * Checks the student's choice of action for the current step and records the
     * first answer.
     */
    private void predict(Move chosen) {
        stopDemo();
        TraceStep<Integer> step = currentStep();
        if (step == null) {
            return;
        }
        boolean correct = step.move() == chosen;
        if (answeredSteps.add(stepIndex)) {
            tracker.record(predictSteps == steps ? predictCategory
                    : lastWasInsert ? "trace-insert" : "trace-search", correct, hintsShown);
        }
        predictFeedback.setText((correct ? "Correct. " : "Not quite. ")
                + "The answer is " + label(step.move()) + " because " + step.rule());
        refresh();
    }

    private void showHint() {
        stopDemo();
        TraceStep<Integer> step = currentStep();
        if (step == null || hintsShown >= HINT_LIMIT) {
            return;
        }
        hintsShown++;
        boolean ordered = step.comparison() == Comparison.LESS || step.comparison() == Comparison.GREATER;
        hintText.setText(switch (hintsShown) {
            case 1 -> "Hint 1: " + step.rule();
            case 2 -> ordered
                    ? "Hint 2: the target is "
                            + (step.comparison() == Comparison.LESS ? "smaller" : "larger")
                            + " than " + step.value() + "."
                    : "Hint 2: the answer is not " + label(wrongChoice(step)) + ".";
            default -> "Hint 3: the answer is " + label(step.move()) + ".";
        });
        refresh();
    }

    private TraceStep<Integer> currentStep() {
        return stepIndex >= 0 && stepIndex < steps.size() ? steps.get(stepIndex) : null;
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
        TraceStep<Integer> step = currentStep();
        boolean answerable = step != null;
        // Rebuild the answer buttons only when the step changes, so keyboard focus
        // survives an answer.
        if (builtFor != steps || builtIndex != stepIndex) {
            answerBox.getChildren().clear();
            if (answerable) {
                for (Move choice : choicesFor(stepIndex)) {
                    Button button = Ui.secondary(label(choice));
                    button.setMinWidth(Region.USE_PREF_SIZE);
                    button.setOnAction(e -> predict(choice));
                    answerBox.getChildren().add(button);
                }
            }
            answerBox.getChildren().add(hint);
            builtFor = steps;
            builtIndex = stepIndex;
        }
        predictPrompt.setText(answerable
                ? "What happens at this step?"
                : "Enter a value under 'Predict the next step' or run an operation above.");
        hint.setDisable(!answerable || hintsShown >= HINT_LIMIT);
        hint.setText("Hint (" + hintsShown + " of " + HINT_LIMIT + ")");
        previous.setDisable(stepIndex <= 0);
        next.setDisable(steps.isEmpty() || stepIndex >= steps.size() - 1);
        reset.setDisable(steps.isEmpty());
        updateHistoryButtons();
    }

    private void undo() {
        stopDemo();
        if (!history.canUndo()) {
            return;
        }
        afterHistoryChange("Undid " + history.undo(tree) + ". The tree is back to how it was before.");
    }

    private void redo() {
        stopDemo();
        if (!history.canRedo()) {
            return;
        }
        afterHistoryChange("Redid " + history.redo(tree) + ".");
    }

    /**
     * The old trace described a different tree, so it is cleared along with the
     * last traversal.
     */
    private void afterHistoryChange(String message) {
        clearTrace();
        traverseResult.setText("");
        setResult(message, false);
        refresh();
    }

    /** Greys out Undo or Redo when there is nothing to step to. */
    private void updateHistoryButtons() {
        undoButton.setDisable(!history.canUndo());
        redoButton.setDisable(!history.canRedo());
        String undoText = history.canUndo() ? "Undo " + history.undoLabel() : null;
        String redoText = history.canRedo() ? "Redo " + history.redoLabel() : null;
        undoButton.setAccessibleText(undoText != null ? undoText : "Undo, nothing to undo");
        redoButton.setAccessibleText(redoText != null ? redoText : "Redo, nothing to redo");
        undoButton.setTooltip(undoText != null ? new Tooltip(undoText) : null);
        redoButton.setTooltip(redoText != null ? new Tooltip(redoText) : null);
    }
}