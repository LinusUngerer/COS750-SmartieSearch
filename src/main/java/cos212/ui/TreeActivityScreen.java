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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
 * layout sizes
 * dynamically, while guiding the student through the demo and a 2-step "Your
 * turn!" hand-off
 * (1: New tree -> 2: Insert your first value).
 */
final class TreeActivityScreen extends BorderPane {

    private static final String INVALID_VALUE = "Invalid value: Only whole numbers are allowed"
            + " (for example 45 or -3). The tree was not changed.";
    private static final int HINT_LIMIT = 3;

    private static final String[] GUIDE_TITLES = {
            "Watch how a Binary Search Tree works",
            "Build & Search your own tree (Bottom left)",
            "Predict & Step through (Bottom right)",
            "Traverse & Customize View (Top & Bottom)"
    };

    private static final String[] GUIDE_TEXTS = {
            "Click 'Watch animated demo' to see a sample search path light up node-by-node while the step controls highlight in order.",
            "Enter a whole number under 'Insert value' or 'Search value'. Smaller values go left; larger values go right.",
            "Before advancing, click 'Left' or 'Right' to predict the next subtree, or use 'Previous' and 'Next' to replay comparisons.",
            "Use 'Depth First' or 'Breadth First' to view traversal orders, and toggle 'Hide Key' or 'Hide Trace' anytime."
    };

    private static final String SPOTLIGHT_STYLE = "-fx-border-color: #003380; -fx-border-width: 2.5; -fx-border-radius: 14; -fx-background-radius: 14;"
            + "-fx-effect: dropshadow(three-pass-box, rgba(0, 51, 128, 0.28), 16, 0.2, 0, 2);";

    private static final String FLASH_BTN_STYLE = "-fx-background-color: #FEF3C7; -fx-border-color: #D97706; -fx-border-width: 2.5;"
            + "-fx-text-fill: #78350F; -fx-font-weight: bold;"
            + "-fx-effect: dropshadow(three-pass-box, rgba(245, 158, 11, 0.65), 10, 0.25, 0, 0);";

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
    private final Button insertBtn = Ui.primary("Insert");
    private final Button left = Ui.secondary("Left");
    private final Button right = Ui.secondary("Right");
    private final Button hint = Ui.secondary("Hint (0 of 3)");
    private final Button previous = Ui.secondary("Previous");
    private final Button next = Ui.primary("Next");
    private final Button reset = Ui.secondary("Reset");
    private final Button emptyTreeBtn = Ui.secondary("New tree");
    private final Label hintText = Ui.note("");
    private final Label predictFeedback = Ui.note("");

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

        left.setOnAction(e -> predict(Move.LEFT));
        right.setOnAction(e -> predict(Move.RIGHT));
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
                Ui.note("Create your own BST and follow along with the insertion, search, and traversal paths."));
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
        guideBanner.setStyle(SPOTLIGHT_STYLE);
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
        insertField.setDisable(false);
        insertField.setStyle("");
        insertBtn.setDisable(false);
        insertBtn.setStyle("");
        emptyTreeBtn.setDisable(false);
        emptyTreeBtn.setStyle("");
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
                if (searchControlsBox != null) {
                    searchControlsBox.setDisable(true);
                    searchControlsBox.setOpacity(0.35);
                }
                if (traversalsRow != null) {
                    traversalsRow.setDisable(true);
                    traversalsRow.setOpacity(0.35);
                }
                emptyTreeBtn.setStyle(FLASH_BTN_STYLE);

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

                // Step 2 of 2: Now spotlight the Insert input & button!
                emptyTreeBtn.setDisable(true);
                if (searchControlsBox != null) {
                    searchControlsBox.setDisable(true);
                    searchControlsBox.setOpacity(0.35);
                }
                if (traversalsRow != null) {
                    traversalsRow.setDisable(true);
                    traversalsRow.setOpacity(0.35);
                }
                insertField.setDisable(false);
                insertField.setStyle("-fx-border-color: #D97706; -fx-border-width: 2; -fx-border-radius: 6;");
                insertBtn.setDisable(false);
                insertBtn.setStyle(FLASH_BTN_STYLE);
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
        card.setStyle(spotlight ? SPOTLIGHT_STYLE : "");
    }

    /**
     * Builds a sample BST and animates a step-by-step search path with sequential
     * control button flashes.
     */
    private void runAnimatedDemo() {
        stopDemo();
        demoHasRun = true;
        tree.clear();
        for (int v : new int[] { 50, 30, 70, 40 }) {
            tree.insert(v);
        }
        startTrace(Traces.search(tree, 40), false);
        setFocusMode(FocusMode.DEMO_ANIMATING);
        setResult("Demo running: Searching for 40 in sample tree [50, 30, 70, 40]...", false);
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
        Button[] sequence = { left, right, hint, previous, next, reset };
        int first = (currentIdx * 2) % sequence.length;
        int second = (currentIdx * 2 + 1) % sequence.length;
        sequence[first].setStyle(FLASH_BTN_STYLE);
        sequence[second].setStyle(FLASH_BTN_STYLE);
    }

    private void clearButtonFlashes() {
        for (Button b : new Button[] { left, right, hint, previous, next, reset }) {
            b.setStyle("");
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

        HBox buildInput = Ui.row(8, insertField, insertBtn);
        VBox build = new VBox(8, Ui.note("Insert value"), buildInput, emptyTreeBtn);
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
        searchControlsBox = new VBox(8, Ui.note("Search value"), searchInput);
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
        yourTurnBanner.setStyle(
                "-fx-background-color: #FEF3C7; -fx-border-color: #F59E0B; -fx-border-radius: 8; -fx-background-radius: 8;");
        yourTurnBanner.setVisible(false);
        yourTurnBanner.setManaged(false);

        controlsCard = Ui.card(Ui.titleRow("Build, search and traverse"),
                yourTurnBanner,
                Ui.row(10, build, searchControlsBox),
                Ui.note("Duplicates leave the tree unchanged."),
                traversalsRow, traverseResult);

        HBox answers = new HBox(8, left, right, hint);
        HBox stepButtons = new HBox(8, previous, next, reset);
        predictCard = Ui.card(Ui.titleRow("Predict the next step"),
                Ui.note("Which subtree next?"), answers, hintText, predictFeedback, stepButtons, stepCaption);

        controlsCard.setMinWidth(Region.USE_PREF_SIZE);
        predictCard.setMinWidth(Region.USE_PREF_SIZE);
        predictCard.setMaxWidth(420);

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

    private void clearTree() {
        stopDemo();
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
     * Checks the student's choice of subtree for the current step and records the
     * first answer.
     */
    private void predict(Move chosen) {
        stopDemo();
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
        stopDemo();
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
        if (focusMode == FocusMode.DEMO_ANIMATING) {
            left.setDisable(false);
            right.setDisable(false);
            hint.setDisable(false);
            previous.setDisable(false);
            next.setDisable(false);
            reset.setDisable(false);
            return;
        }
        left.setDisable(!subtree);
        right.setDisable(!subtree);
        hint.setDisable(!subtree || hintsShown >= HINT_LIMIT);
        hint.setText("Hint (" + hintsShown + " of " + HINT_LIMIT + ")");
        previous.setDisable(stepIndex <= 0);
        next.setDisable(steps.isEmpty() || stepIndex >= steps.size() - 1);
        reset.setDisable(steps.isEmpty());
    }
}