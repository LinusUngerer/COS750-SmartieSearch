package cos212.trace;

import cos212.bst.BinarySearchTree;
import cos212.trace.TraceStep.Comparison;
import cos212.trace.TraceStep.Move;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
}
