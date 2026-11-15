package cos212.trace;

import cos212.bst.BinarySearchTree;
import cos212.trace.TraceStep.Comparison;
import cos212.trace.TraceStep.Move;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import cos212.bst.BinarySearchTree.NodeView;

/**
 * Builds the step lists the interface replays. A trace reads the tree but never changes
 * it, so a student can step through a trace without affecting the tree.
 */
public final class Traces {

    private Traces() {
    }

    /**
     * @return the steps of a search for {@code target}, ending with FOUND or MISSING
     */
    public static <T extends Comparable<T>> List<TraceStep<T>> search(BinarySearchTree<T> tree, T target) {
        return build(tree, target, false);
    }

    /**
     * @return the steps of inserting {@code target}, ending with an attach or DUPLICATE
     *         step. The tree is not changed; call {@link BinarySearchTree#insert} to apply it.
     */
    public static <T extends Comparable<T>> List<TraceStep<T>> insert(BinarySearchTree<T> tree, T target) {
        return build(tree, target, true);
    }

    private static <T extends Comparable<T>> List<TraceStep<T>> build(
            BinarySearchTree<T> tree, T target, boolean insert) {
        Objects.requireNonNull(target, "target");
        List<T> path = tree.comparedPath(target);
        List<TraceStep<T>> steps = new ArrayList<>();

        if (path.isEmpty()) {
            steps.add(insert
                    ? new TraceStep<>(null, null, Move.ATTACH_ROOT,
                            "The tree is empty, so " + target + " becomes the root.")
                    : new TraceStep<>(null, null, Move.MISSING,
                            "The tree is empty, so " + target + " is not in the tree."));
            return steps;
        }

        for (T node : path) {
            int cmp = target.compareTo(node);
            String head = "Compare " + target + " with " + node + ". ";
            if (cmp == 0) {
                steps.add(insert
                        ? new TraceStep<>(node, Comparison.EQUAL, Move.DUPLICATE,
                                head + target + " is already in the tree, so nothing is added.")
                        : new TraceStep<>(node, Comparison.EQUAL, Move.FOUND,
                                head + "They are equal, so " + target + " is in the tree."));
            } else if (cmp < 0) {
                steps.add(new TraceStep<>(node, Comparison.LESS, Move.LEFT,
                        head + target + " is smaller, so go left."));
            } else {
                steps.add(new TraceStep<>(node, Comparison.GREATER, Move.RIGHT,
                        head + target + " is larger, so go right."));
            }
        }

        // The path ends either at a match or at a node whose link in the needed direction is empty.
        T end = path.get(path.size() - 1);
        int endCmp = target.compareTo(end);
        if (endCmp != 0) {
            String side = endCmp < 0 ? "left" : "right";
            if (insert) {
                steps.add(new TraceStep<>(end, null,
                        endCmp < 0 ? Move.ATTACH_LEFT : Move.ATTACH_RIGHT,
                        "The " + side + " link of " + end + " is empty, so attach "
                                + target + " as its " + side + " child."));
            } else {
                steps.add(new TraceStep<>(end, null, Move.MISSING,
                        "The " + side + " link of " + end + " is empty, so " + target
                                + " is not in the tree."));
            }
        }
        return steps;
    }

    /**
     * @return the steps of deleting {@code target} by copying: find the node, then (for two
     *         children) find the largest value in its left subtree and copy it up. The tree is not changed.
     */
    public static <T extends Comparable<T>> List<TraceStep<T>> deleteByCopying(BinarySearchTree<T> tree, T target) {
        return buildDelete(tree, target, true);
    }

    /**
     * @return the steps of deleting {@code target} by merging: find the node, then (for two
     *         children) find the rightmost node of its left subtree and hang the right subtree there.
     *         The tree is not changed.
     */
    public static <T extends Comparable<T>> List<TraceStep<T>> deleteByMerging(BinarySearchTree<T> tree, T target) {
        return buildDelete(tree, target, false);
    }

    private static <T extends Comparable<T>> List<TraceStep<T>> buildDelete(
            BinarySearchTree<T> tree, T target, boolean copying) {
        Objects.requireNonNull(target, "target");
        List<TraceStep<T>> steps = new ArrayList<>();
        NodeView<T> node = tree.rootView();
        if (node == null) {
            steps.add(new TraceStep<>(null, null, Move.MISSING,
                    "The tree is empty, so " + target + " is not in the tree and nothing is deleted."));
            return steps;
        }
        while (true) {
            int cmp = target.compareTo(node.value());
            String head = "Compare " + target + " with " + node.value() + ". ";
            if (cmp == 0) {
                steps.add(new TraceStep<>(node.value(), Comparison.EQUAL, Move.FOUND,
                        head + "They are equal, so " + target + " is found and will be deleted."));
                break;
            }
            String side = cmp < 0 ? "left" : "right";
            steps.add(new TraceStep<>(node.value(),
                    cmp < 0 ? Comparison.LESS : Comparison.GREATER,
                    cmp < 0 ? Move.LEFT : Move.RIGHT,
                    head + target + " is " + (cmp < 0 ? "smaller" : "larger") + ", so go " + side + "."));
            NodeView<T> next = cmp < 0 ? node.left() : node.right();
            if (next == null) {
                steps.add(new TraceStep<>(node.value(), null, Move.MISSING,
                        "The " + side + " link of " + node.value() + " is empty, so " + target
                                + " is not in the tree and nothing is deleted."));
                return steps;
            }
            node = next;
        }
        if (copying) {
            addCopyingSteps(steps, node);
        } else {
            addMergingSteps(steps, node);
        }
        return steps;
    }

    /** Handles a found node with at most one child, for both delete methods. Returns true if it did. */
    private static <T> boolean addSimpleSteps(List<TraceStep<T>> steps, NodeView<T> node) {
        T value = node.value();
        NodeView<T> left = node.left();
        NodeView<T> right = node.right();
        if (left != null && right != null) {
            return false;
        }
        if (left == null && right == null) {
            steps.add(new TraceStep<>(value, null, Move.DELETE_NODE,
                    value + " has no children, so its node is simply removed."));
        } else if (right == null) {
            steps.add(new TraceStep<>(value, null, Move.MERGE_REPLACE,
                    value + " has only a left child, " + left.value()
                            + ", so the left subtree takes the place of " + value + "."));
        } else {
            steps.add(new TraceStep<>(value, null, Move.RIGHT_REPLACE,
                    value + " has only a right child, " + right.value()
                            + ", so the right subtree takes the place of " + value + "."));
        }
        return true;
    }

    /** The steps after the node is found, for delete by copying. */
    private static <T> void addCopyingSteps(List<TraceStep<T>> steps, NodeView<T> node) {
        if (addSimpleSteps(steps, node)) {
            return;
        }
        T value = node.value();
        NodeView<T> largest = seekRightmost(steps, node,
                "Delete by copying needs a replacement: the largest value in its left subtree");
        T replacement = largest.value();
        steps.add(new TraceStep<>(replacement, null, Move.REPLACEMENT_FOUND,
                replacement + " has no right child, so it is the largest value in the left subtree"
                        + " and becomes the replacement."));
        steps.add(new TraceStep<>(replacement, null, Move.COPY_VALUE,
                "Copy " + replacement + " into the node that held " + value + "."));
        steps.add(new TraceStep<>(replacement, null, Move.REMOVE_REPLACEMENT,
                "Remove the old node of " + replacement + ". " + (largest.left() == null
                        ? "It is a leaf, so it is simply removed."
                        : "Its left child, " + largest.left().value() + ", takes its place.")));
    }

    /** The steps after the node is found, for delete by merging. */
    private static <T> void addMergingSteps(List<TraceStep<T>> steps, NodeView<T> node) {
        if (addSimpleSteps(steps, node)) {
            return;
        }
        T value = node.value();
        NodeView<T> left = node.left();
        NodeView<T> right = node.right();
        NodeView<T> rightmost = seekRightmost(steps, node,
                "Delete by merging needs the rightmost node of its left subtree, so the right subtree can hang below it");
        T end = rightmost.value();
        steps.add(new TraceStep<>(end, null, Move.REPLACEMENT_FOUND,
                end + " has no right child, so it is the rightmost node of the left subtree."));
        steps.add(new TraceStep<>(end, null, Move.MERGE_ATTACH,
                "Attach the right subtree of " + value + ", rooted at " + right.value()
                        + ", as the right child of " + end + "."));
        steps.add(new TraceStep<>(left.value(), null, Move.MERGE_REPLACE,
                "The left subtree, rooted at " + left.value() + ", takes the place of " + value + "."));
    }

    /**
     * For a node with two children: the decision to find a replacement, the decision to go left,
     * then the walk right to the rightmost node of the left subtree. Returns that node.
     */
    private static <T> NodeView<T> seekRightmost(List<TraceStep<T>> steps, NodeView<T> node, String purpose) {
        T value = node.value();
        steps.add(new TraceStep<>(value, null, Move.FIND_REPLACEMENT,
                value + " has two children, so it cannot simply be removed. " + purpose + "."));
        NodeView<T> current = node.left();
        steps.add(new TraceStep<>(value, null, Move.SEEK_LEFT,
                "The largest value smaller than " + value + " is in its left subtree, so go left to "
                        + current.value() + "."));
        while (current.right() != null) {
            steps.add(new TraceStep<>(current.value(), null, Move.SEEK_RIGHT,
                    current.value() + " has a right child, " + current.right().value()
                            + ", and larger values are to the right, so go right."));
            current = current.right();
        }
        return current;
    }
}
