package cos212.ui;

import cos212.bst.BinarySearchTree.NodeView;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Draws a tree. Each node is a circle with its value inside. The node being compared has
 * a thick outline and the word "current" above it, and nodes already passed have a dashed
 * outline. The key in the top left explains the marks, so no mark depends on colour alone
 * (NFR5). The key can be turned off for the diagnostic, where the question has its own text.
 */
final class TreeCanvas extends Canvas {

    private static final double MARGIN = 40;
    private static final double RADIUS = 20;
    private static final double TOP = 140;
    private static final double TOP_WITHOUT_KEY = 40;
    private static final Color HIGHLIGHT = Color.web("#021B6E");
    private static final Color LINE = Color.web("#5B5366");

    private final boolean showKey;
    private NodeView<Integer> root;
    private Integer current;
    private Set<Integer> visited = Set.of();

    TreeCanvas(boolean showKey) {
        this.showKey = showKey;
        widthProperty().addListener((obs, oldValue, newValue) -> paint());
        heightProperty().addListener((obs, oldValue, newValue) -> paint());
    }

    /**
     * Shows the tree.
     *
     * @param root the root of the tree, or {@code null} for an empty tree
     * @param current the value being compared now, or {@code null} for none
     * @param visited the values already passed in the trace
     */
    void show(NodeView<Integer> root, Integer current, Set<Integer> visited) {
        this.root = root;
        this.current = current;
        this.visited = visited;
        paint();
    }

    private void paint() {
        GraphicsContext g = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        double top = showKey ? TOP : TOP_WITHOUT_KEY;
        g.clearRect(0, 0, width, height);
        g.setFont(Font.font(14));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);

        if (root == null) {
            g.setFill(LINE);
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText("The tree is empty. Insert a value to start.", MARGIN, top);
        } else {
            drawTree(g, width, height, top);
        }
        if (showKey) {
            drawKey(g);
        }
    }

    private void drawTree(GraphicsContext g, double width, double height, double top) {
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
            p.y = top + (p.depth + 0.5) * (height - top - MARGIN) / (maxDepth + 1);
        }

        g.setStroke(LINE);
        g.setLineWidth(1);
        for (int[] edge : edges) {
            Placed from = placed.get(edge[0]);
            Placed to = placed.get(edge[1]);
            g.strokeLine(from.x, from.y, to.x, to.y);
        }

        for (Placed p : placed) {
            boolean isCurrent = current != null && current.equals(p.value);
            boolean isVisited = visited.contains(p.value);
            g.setFill(Color.WHITE);
            g.fillOval(p.x - RADIUS, p.y - RADIUS, 2 * RADIUS, 2 * RADIUS);
            if (isCurrent) {
                g.setStroke(HIGHLIGHT);
                g.setLineWidth(4);
                g.setLineDashes();
            } else {
                g.setStroke(LINE);
                g.setLineWidth(1);
                if (isVisited) {
                    g.setLineDashes(5, 4);
                } else {
                    g.setLineDashes();
                }
            }
            g.strokeOval(p.x - RADIUS, p.y - RADIUS, 2 * RADIUS, 2 * RADIUS);
            g.setLineDashes();
            g.setFill(Color.BLACK);
            g.fillText(String.valueOf(p.value), p.x, p.y);
            if (isCurrent) {
                g.setFill(HIGHLIGHT);
                g.fillText("current", p.x, p.y - RADIUS - 12);
            }
        }
    }

    /** Draws the key in the top left corner: what each mark means. */
    private static void drawKey(GraphicsContext g) {
        double x = 16;
        double y = 16;
        double w = 230;
        double h = 118;
        g.setFill(Color.web("#FCF5EE"));
        g.fillRect(x, y, w, h);
        g.setStroke(Color.web("#D8C7B1"));
        g.setLineWidth(1);
        g.strokeRect(x, y, w, h);

        String[] labels = {
            "Node: not yet compared",
            "Current: being compared now",
            "Visited: already passed",
            "Link: parent to child"
        };
        g.setFont(Font.font(12));
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.CENTER);
        for (int i = 0; i < labels.length; i++) {
            double cy = y + 22 + i * 24;
            double cx = x + 20;
            g.setFill(Color.WHITE);
            if (i == 1) {
                g.setStroke(HIGHLIGHT);
                g.setLineWidth(3);
                g.setLineDashes();
            } else if (i == 2) {
                g.setStroke(LINE);
                g.setLineWidth(1);
                g.setLineDashes(4, 3);
            } else {
                g.setStroke(LINE);
                g.setLineWidth(1);
                g.setLineDashes();
            }
            if (i == 3) {
                g.strokeLine(cx - 8, cy, cx + 8, cy);
            } else {
                g.fillOval(cx - 8, cy - 8, 16, 16);
                g.strokeOval(cx - 8, cy - 8, 16, 16);
            }
            g.setLineDashes();
            g.setFill(Color.BLACK);
            g.fillText(labels[i], x + 38, cy);
        }
        g.setTextAlign(TextAlignment.CENTER);
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
