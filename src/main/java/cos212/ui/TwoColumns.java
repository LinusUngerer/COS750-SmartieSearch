package cos212.ui;

import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Two panes side by side, half the width each, that stack (the second under the first) when
 * the available width drops below {@value #STACK_BELOW} pixels.
 */
final class TwoColumns extends GridPane {

    /** Below this width the second pane moves under the first instead of beside it. */
    static final double STACK_BELOW = 860;

    private final Region first;
    private final Region second;
    private Boolean stacked;

    TwoColumns(Region first, Region second) {
        this.first = first;
        this.second = second;
        first.setMinWidth(0);
        second.setMinWidth(0);
        setHgap(18);
        setVgap(16);
        widthProperty().addListener((obs, oldWidth, newWidth) -> arrange(newWidth.doubleValue() < STACK_BELOW));
        arrange(false);
    }

    private void arrange(boolean stack) {
        if (stacked != null && stacked == stack) {
            return;
        }
        stacked = stack;
        getChildren().clear();
        getColumnConstraints().clear();
        if (stack) {
            getColumnConstraints().add(column(100));
            add(first, 0, 0);
            add(second, 0, 1);
        } else {
            getColumnConstraints().addAll(column(50), column(50));
            add(first, 0, 0);
            add(second, 1, 0);
        }
        for (Node pane : new Node[] {first, second}) {
            GridPane.setValignment(pane, VPos.TOP);
        }
        GridPane.setVgrow(first, Priority.ALWAYS);
    }

    private static ColumnConstraints column(double percent) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(percent);
        constraints.setHgrow(Priority.ALWAYS);
        constraints.setFillWidth(true);
        return constraints;
    }
}
