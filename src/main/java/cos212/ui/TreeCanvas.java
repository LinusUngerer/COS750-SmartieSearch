package cos212.ui;

import cos212.bst.BinarySearchTree.NodeView;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Draws the tree. Each node is a circle with its value inside. The node being compared
 * has a thick outline and the word "current" above it, so the highlight does not depend
 * on colour alone (NFR5).
 */
final class TreeCanvas extends Canvas {

    private static final double MARGIN = 40;
    private static final double RADIUS = 20;
    private static final Color HIGHLIGHT = Color.web("#1d4ed8");

    private NodeView<Integer> root;
    private Integer current;

    TreeCanvas() {
        widthProperty().addListener((obs, oldValue, newValue) -> paint());
        heightProperty().addListener((obs, oldValue, newValue) -> paint());
    }

    /** Shows the tree, highlighting the node with the given value, or no node if null. */
    void show(NodeView<Integer> root, Integer current) {
        this.root = root;
        this.current = current;
        paint();
    }

    private void paint() {
        GraphicsContext g = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        g.clearRect(0, 0, width, height);
        g.setFont(Font.font(14));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);

        if (root == null) {
            g.setFill(Color.GRAY);
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText("The tree is empty. Insert a value to start.", MARGIN, MARGIN);
            return;
        }

        List<Placed> placed = new ArrayList<>();
        List<int[]> edges = new ArrayList<>();
        collect(root, 0, -1, new int[] {0}, placed, edges);

        int count = placed.size();
        int maxDepth = 0;
        for (Placed p : placed) {
            maxDepth = Math.max(maxDepth, p.depth);
        }
        for (Placed p : placed) {
            p.x = MARGIN + (p.rank + 0.5) * (width - 2 * MARGIN) / count;
            p.y = MARGIN + (p.depth + 0.5) * (height - 2 * MARGIN) / (maxDepth + 1);
        }

        g.setStroke(Color.GRAY);
        g.setLineWidth(1);
        for (int[] edge : edges) {
            Placed from = placed.get(edge[0]);
            Placed to = placed.get(edge[1]);
            g.strokeLine(from.x, from.y, to.x, to.y);
        }

        for (Placed p : placed) {
            boolean isCurrent = current != null && current.equals(p.value);
            g.setFill(Color.WHITE);
            g.fillOval(p.x - RADIUS, p.y - RADIUS, 2 * RADIUS, 2 * RADIUS);
            g.setStroke(isCurrent ? HIGHLIGHT : Color.GRAY);
            g.setLineWidth(isCurrent ? 4 : 1);
            g.strokeOval(p.x - RADIUS, p.y - RADIUS, 2 * RADIUS, 2 * RADIUS);
            g.setFill(Color.BLACK);
            g.fillText(String.valueOf(p.value), p.x, p.y);
            if (isCurrent) {
                g.setFill(HIGHLIGHT);
                g.fillText("current", p.x, p.y - RADIUS - 12);
            }
        }
    }

    /** Walks the tree in order, giving each node its in-order rank and recording its links. */
    private static void collect(NodeView<Integer> node, int depth, int parent, int[] rank,
            List<Placed> out, List<int[]> edges) {
        if (node == null) {
            return;
        }
        int index = out.size();
        Placed placed = new Placed(node.value(), depth);
        out.add(placed);
        if (parent >= 0) {
            edges.add(new int[] {parent, index});
        }
        collect(node.left(), depth + 1, index, rank, out, edges);
        placed.rank = rank[0]++;
        collect(node.right(), depth + 1, index, rank, out, edges);
    }

    private static final class Placed {
        final Integer value;
        final int depth;
        int rank;
        double x;
        double y;

        Placed(Integer value, int depth) {
            this.value = value;
            this.depth = depth;
        }
    }
}
