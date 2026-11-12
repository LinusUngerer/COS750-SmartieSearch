package cos212.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * A column of answer cards for one multiple-choice question. The student clicks a card (or
 * presses Space or Enter on it) to select it. After grading, the chosen card turns green or
 * red, the other cards fade, and a feedback panel appears directly under the chosen card.
 * Used by the diagnostic gate and by the lesson questions, so both look the same.
 */
final class QuestionView extends VBox {

    /** One answer card and the parts of it that change when it is selected or graded. */
    private record Option(HBox card, Label mark, Label verdict) {
    }

    private final List<Option> options = new ArrayList<>();
    private final List<TextField> blanks = new ArrayList<>();
    private final Runnable onSelect;
    private int selected = -1;
    private boolean graded;

    /**
     * @param onSelect runs whenever the student selects a card, so the screen can clear errors
     */
    QuestionView(Runnable onSelect) {
        super(10);
        this.onSelect = onSelect;
    }

    /** Shows a new set of answers, with nothing selected. */
    void show(List<String> choices) {
        options.clear();
        blanks.clear();
        getChildren().clear();
        for (int i = 0; i < choices.size(); i++) {
            Option option = optionCard(i, choices.get(i));
            options.add(option);
            getChildren().add(option.card());
        }
        selected = -1;
        graded = false;
    }

    /** Shows text fields for a fill-in-the-blank question. */
    void showFillIn(int blankCount) {
        options.clear();
        blanks.clear();
        getChildren().clear();
        HBox fields = Ui.row(8);
        for (int i = 0; i < blankCount; i++) {
            Label label = Ui.css(new Label("Blank " + (i + 1)), "option-letter");
            TextField field = new TextField();
            field.setPromptText("Type answer");
            field.setPrefColumnCount(9);
            field.textProperty().addListener((obs, oldValue, newValue) -> onSelect.run());
            blanks.add(field);
            fields.getChildren().addAll(label, field);
        }
        getChildren().add(fields);
        selected = -1;
        graded = false;
    }

    /** @return the index of the selected answer, or -1 if none is selected */
    int selected() {
        return selected;
    }

    /** @return whether the current question has a complete response */
    boolean hasAnswer() {
        return blanks.isEmpty() ? selected >= 0
                : blanks.stream().allMatch(field -> !field.getText().isBlank());
    }

    /** Compares fill-in responses without regard to case or surrounding whitespace. */
    boolean matches(List<String> expected) {
        return blanks.size() == expected.size()
                && java.util.stream.IntStream.range(0, blanks.size())
                        .allMatch(i -> blanks.get(i).getText().trim().equalsIgnoreCase(expected.get(i).trim()));
    }

    /** @return whether the current answers have been graded */
    boolean graded() {
        return graded;
    }

    /**
     * Marks the selected answer right or wrong and shows feedback under it.
     *
     * @param correct whether the selected answer is the correct one
     * @param heading the feedback heading
     * @param body the feedback text, which names the rule
     * @param pill a short extra note shown as a pill, or {@code null} for none
     */
    void grade(boolean correct, String heading, String body, String pill) {
        if (graded || !hasAnswer()) {
            return;
        }
        graded = true;
        if (!blanks.isEmpty()) {
            blanks.forEach(field -> field.setDisable(true));
            getChildren().add(feedbackPanel(correct, heading, body, pill));
            return;
        }
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            option.card().setCursor(Cursor.DEFAULT);
            option.card().setFocusTraversable(false);
            if (i != selected) {
                option.card().getStyleClass().add("option-muted");
            }
        }
        Option chosen = options.get(selected);
        chosen.card().getStyleClass().remove("option-selected");
        chosen.card().getStyleClass().add(correct ? "option-right" : "option-wrong");
        chosen.mark().setText(correct ? "✓" : "✕");
        chosen.verdict().setText(correct ? "Correct!" : "Not quite");
        chosen.verdict().setVisible(true);
        chosen.verdict().setManaged(true);
        getChildren().add(selected + 1, feedbackPanel(correct, heading, body, pill));
    }

    private Option optionCard(int i, String text) {
        Label mark = Ui.css(new Label(), "option-mark");
        mark.setMinSize(26, 26);
        mark.setMaxSize(26, 26);
        mark.setAlignment(Pos.CENTER);
        Label letter = Ui.css(new Label("OPTION " + (char) ('A' + i)), "option-letter");
        Label body = Ui.css(new Label(text), "option-text");
        body.setWrapText(true);
        VBox words = new VBox(2, letter, body);
        HBox.setHgrow(words, Priority.ALWAYS);
        Label verdict = Ui.css(new Label(), "option-verdict");
        verdict.setVisible(false);
        verdict.setManaged(false);

        HBox card = Ui.row(14, mark, words, verdict);
        card.getStyleClass().add("option");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setFocusTraversable(true);
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(e -> select(i));
        card.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                select(i);
                e.consume();
            }
        });
        return new Option(card, mark, verdict);
    }

    private void select(int choice) {
        if (graded) {
            return;
        }
        selected = choice;
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            option.card().getStyleClass().remove("option-selected");
            option.mark().setText("");
            if (i == choice) {
                option.card().getStyleClass().add("option-selected");
                option.mark().setText("●");
            }
        }
        onSelect.run();
    }

    private static HBox feedbackPanel(boolean correct, String heading, String body, String pill) {
        Label icon = Ui.css(new Label(correct ? "✓" : "!"), "feedback-icon");
        icon.setMinSize(40, 40);
        icon.setMaxSize(40, 40);
        icon.setAlignment(Pos.CENTER);
        Label title = Ui.css(new Label(heading), "feedback-heading");
        Label text = Ui.css(new Label(body), "feedback-body");
        text.setWrapText(true);
        VBox words = new VBox(6, title, text);
        if (pill != null) {
            words.getChildren().add(Ui.row(0, Ui.css(new Label(pill), "feedback-pill")));
        }
        HBox.setHgrow(words, Priority.ALWAYS);
        HBox panel = new HBox(14, icon, words);
        panel.setAlignment(Pos.TOP_LEFT);
        panel.getStyleClass().addAll("feedback-panel", correct ? "feedback-panel-right" : "feedback-panel-wrong");
        return panel;
    }
}
