package cos212.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cos212.bst.BinarySearchTree;
import cos212.bst.BinarySearchTree.NodeView;
import cos212.trace.TraceStep.Move;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class TracesTest {

    /** The example tree from the Detailed Design: 5 at the root, 3 and 7 below it. */
    private static BinarySearchTree<Integer> exampleTree() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        for (int v : new int[] {5, 3, 1, 7, 6, 9}) {
            tree.insert(v);
        }
        return tree;
    }

    private static <T> List<Move> moves(List<TraceStep<T>> steps) {
        return steps.stream().map(TraceStep::move).toList();
    }

    @Test
    void searchForAbsentValueEndsAtTheEmptyLink() {
        List<TraceStep<Integer>> steps = Traces.search(exampleTree(), 4);
        assertEquals(List.of(Move.LEFT, Move.RIGHT, Move.MISSING), moves(steps));
        assertEquals(3, steps.get(2).value());
    }

    @Test
    void searchForPresentValueEndsWithFound() {
        List<TraceStep<Integer>> steps = Traces.search(exampleTree(), 6);
        assertEquals(List.of(Move.RIGHT, Move.LEFT, Move.FOUND), moves(steps));
        assertEquals(6, steps.get(2).value());
    }

    @Test
    void insertTraceAttachesLeafWhereSearchEnds() {
        List<TraceStep<Integer>> steps = Traces.insert(exampleTree(), 4);
        assertEquals(List.of(Move.LEFT, Move.RIGHT, Move.ATTACH_RIGHT), moves(steps));
        assertEquals(3, steps.get(2).value());
    }

    @Test
    void insertTraceForDuplicateEndsWithDuplicate() {
        List<TraceStep<Integer>> steps = Traces.insert(exampleTree(), 6);
        assertEquals(Move.DUPLICATE, steps.get(steps.size() - 1).move());
    }

    @Test
    void insertTraceDoesNotChangeTheTree() {
        BinarySearchTree<Integer> tree = exampleTree();
        Traces.insert(tree, 4);
        assertEquals(6, tree.size());
        assertEquals(List.of(5, 3), tree.comparedPath(3));
    }

    @Test
    void emptyTreeTracesAreSingleSteps() {
        BinarySearchTree<Integer> empty = new BinarySearchTree<>();
        List<TraceStep<Integer>> search = Traces.search(empty, 1);
        assertEquals(List.of(Move.MISSING), moves(search));
        assertNull(search.get(0).value());

        List<TraceStep<Integer>> insert = Traces.insert(empty, 1);
        assertEquals(List.of(Move.ATTACH_ROOT), moves(insert));
    }

    @Test
    void everyStepHasAnExplanationAndARule() {
        for (TraceStep<Integer> step : Traces.search(exampleTree(), 4)) {
            assertFalse(step.explanation().isBlank());
            assertFalse(step.rule().isBlank());
        }
        for (TraceStep<Integer> step : Traces.insert(exampleTree(), 6)) {
            assertFalse(step.rule().isBlank());
        }
    }

    // ==== deletion traces and trace safety ==========================================

    private static final boolean[] BOTH_METHODS = {true, false};

    private static BinarySearchTree<Integer> treeOf(int... values) {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        for (int v : values) {
            tree.insert(v);
        }
        return tree;
    }

    private static <T> List<T> values(List<TraceStep<T>> steps) {
        return steps.stream().map(TraceStep::value).toList();
    }

    private static List<TraceStep<Integer>> deleteTrace(BinarySearchTree<Integer> tree, int value, boolean copying) {
        return copying ? Traces.deleteByCopying(tree, value) : Traces.deleteByMerging(tree, value);
    }

    private static String name(boolean copying) {
        return copying ? "delete by copying" : "delete by merging";
    }

    private static NodeView<Integer> find(BinarySearchTree<Integer> tree, int value) {
        NodeView<Integer> node = tree.rootView();
        while (node != null && node.value() != value) {
            node = value < node.value() ? node.left() : node.right();
        }
        return node;
    }

    // ---- not found ------------------------------------------------------------

    @Test
    void deleteTraceForAnAbsentValueEndsAtTheEmptyLink() {
        for (boolean copying : BOTH_METHODS) {
            List<TraceStep<Integer>> inside = deleteTrace(exampleTree(), 4, copying);
            assertEquals(List.of(Move.LEFT, Move.RIGHT, Move.MISSING), moves(inside), name(copying));
            assertEquals(3, inside.get(2).value(), name(copying));

            List<TraceStep<Integer>> beyond = deleteTrace(exampleTree(), 100, copying);
            assertEquals(List.of(Move.RIGHT, Move.RIGHT, Move.RIGHT, Move.MISSING), moves(beyond), name(copying));
            assertEquals(List.of(5, 7, 9, 9), values(beyond), name(copying));
        }
    }

    @Test
    void deleteTraceForAnEmptyTreeIsASingleMissingStep() {
        for (boolean copying : BOTH_METHODS) {
            List<TraceStep<Integer>> steps = deleteTrace(new BinarySearchTree<>(), 1, copying);
            assertEquals(List.of(Move.MISSING), moves(steps), name(copying));
            assertNull(steps.get(0).value(), name(copying));
        }
    }

    // ---- found, with at most one child ------------------------------------------

    @Test
    void deleteTraceForALeafEndsWithRemoveNode() {
        for (boolean copying : BOTH_METHODS) {
            List<TraceStep<Integer>> steps = deleteTrace(exampleTree(), 1, copying);
            assertEquals(List.of(Move.LEFT, Move.LEFT, Move.FOUND, Move.DELETE_NODE), moves(steps), name(copying));
            assertEquals(List.of(5, 3, 1, 1), values(steps), name(copying));
        }
    }

    @Test
    void deleteTraceForANodeWithOnlyALeftChildEndsWithLeftSubtreeReplaces() {
        for (boolean copying : BOTH_METHODS) {
            // 3 has only the left child 1.
            List<TraceStep<Integer>> steps = deleteTrace(exampleTree(), 3, copying);
            assertEquals(List.of(Move.LEFT, Move.FOUND, Move.MERGE_REPLACE), moves(steps), name(copying));
            assertEquals(3, steps.get(2).value(), name(copying));
        }
    }

    @Test
    void deleteTraceForANodeWithOnlyARightChildEndsWithRightSubtreeReplaces() {
        for (boolean copying : BOTH_METHODS) {
            List<TraceStep<Integer>> steps = deleteTrace(treeOf(5, 3, 4), 3, copying);
            assertEquals(List.of(Move.LEFT, Move.FOUND, Move.RIGHT_REPLACE), moves(steps), name(copying));
            assertEquals(3, steps.get(2).value(), name(copying));
        }
    }

    // ---- found, with two children -------------------------------------------------

    @Test
    void deleteByCopyingTraceFindsCopiesAndRemovesTheReplacement() {
        // 7 has the children 6 and 9; the largest value in its left subtree is 6.
        List<TraceStep<Integer>> steps = Traces.deleteByCopying(exampleTree(), 7);
        assertEquals(List.of(Move.RIGHT, Move.FOUND, Move.FIND_REPLACEMENT, Move.SEEK_LEFT,
                Move.REPLACEMENT_FOUND, Move.COPY_VALUE, Move.REMOVE_REPLACEMENT), moves(steps));
        assertEquals(List.of(5, 7, 7, 7, 6, 6, 6), values(steps));
        assertTrue(steps.get(6).explanation().contains("leaf"));
    }

    @Test
    void deleteByMergingTraceFindsAttachesAndReplaces() {
        List<TraceStep<Integer>> steps = Traces.deleteByMerging(exampleTree(), 7);
        assertEquals(List.of(Move.RIGHT, Move.FOUND, Move.FIND_REPLACEMENT, Move.SEEK_LEFT,
                Move.REPLACEMENT_FOUND, Move.MERGE_ATTACH, Move.MERGE_REPLACE), moves(steps));
        assertEquals(List.of(5, 7, 7, 7, 6, 6, 6), values(steps));
    }

    @Test
    void theDecisionToFindAReplacementAndTheDecisionToGoLeftAreSeparateSteps() {
        for (boolean copying : BOTH_METHODS) {
            List<TraceStep<Integer>> steps = deleteTrace(exampleTree(), 7, copying);
            // After FOUND at 7, the next two steps are both made at 7 itself: first
            // "find a replacement", then "go left", before the walk reaches 6.
            assertEquals(Move.FIND_REPLACEMENT, steps.get(2).move(), name(copying));
            assertEquals(Move.SEEK_LEFT, steps.get(3).move(), name(copying));
            assertEquals(7, steps.get(2).value(), name(copying));
            assertEquals(7, steps.get(3).value(), name(copying));
        }
    }

    @Test
    void theWalkGoesRightWhileThereIsARightChildAndDecidesAtEachNode() {
        // 50 has the left subtree 30 -> 40 -> 45, a chain of right children.
        BinarySearchTree<Integer> chain = treeOf(50, 30, 40, 45, 70, 80);

        List<TraceStep<Integer>> copy = Traces.deleteByCopying(chain, 50);
        assertEquals(List.of(Move.FOUND, Move.FIND_REPLACEMENT, Move.SEEK_LEFT, Move.SEEK_RIGHT,
                Move.SEEK_RIGHT, Move.REPLACEMENT_FOUND, Move.COPY_VALUE, Move.REMOVE_REPLACEMENT), moves(copy));
        assertEquals(List.of(50, 50, 50, 30, 40, 45, 45, 45), values(copy));

        List<TraceStep<Integer>> merge = Traces.deleteByMerging(chain, 50);
        assertEquals(List.of(Move.FOUND, Move.FIND_REPLACEMENT, Move.SEEK_LEFT, Move.SEEK_RIGHT,
                Move.SEEK_RIGHT, Move.REPLACEMENT_FOUND, Move.MERGE_ATTACH, Move.MERGE_REPLACE), moves(merge));
        assertEquals(List.of(50, 50, 50, 30, 40, 45, 45, 30), values(merge));
    }

    @Test
    void removeReplacementStepSaysWhatHappensToTheReplacementsLeftChild() {
        // The replacement 40 has the left child 35, which takes its place.
        List<TraceStep<Integer>> steps = Traces.deleteByCopying(treeOf(50, 30, 70, 20, 40, 35), 50);
        TraceStep<Integer> last = steps.get(steps.size() - 1);
        assertEquals(Move.REMOVE_REPLACEMENT, last.move());
        assertTrue(last.explanation().contains("35"));
    }

    // ---- properties over random trees -----------------------------------------------

    @Test
    void deleteTracesAgreeWithTheTreeOnRandomTrees() {
        long seed = 5L; // fixed, so that a failure can be reproduced
        Random random = new Random(seed);
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        TreeSet<Integer> reference = new TreeSet<>();
        for (int i = 0; i < 60; i++) {
            int v = random.nextInt(200);
            tree.insert(v);
            reference.add(v);
        }
        List<Integer> levelsBefore = tree.breadthFirst();

        for (int target = -5; target < 205; target++) {
            for (boolean copying : BOTH_METHODS) {
                String where = name(copying) + " " + target + " (seed " + seed + ")";
                List<TraceStep<Integer>> steps = deleteTrace(tree, target, copying);
                List<Move> moves = moves(steps);

                if (!reference.contains(target)) {
                    // Absent: the same steps as a search that ends at an empty link.
                    assertEquals(moves(Traces.search(tree, target)), moves, where);
                    assertEquals(Move.MISSING, moves.get(moves.size() - 1), where);
                    assertFalse(moves.contains(Move.FOUND), where);
                    continue;
                }

                // Present: the path to the node is exactly the path the tree compares.
                List<Integer> path = tree.comparedPath(target);
                int found = path.size() - 1;
                assertEquals(path, values(steps).subList(0, path.size()), where);
                assertEquals(Move.FOUND, moves.get(found), where);
                long compared = steps.stream().filter(step -> step.comparison() != null).count();
                assertEquals((long) path.size(), compared, where);

                NodeView<Integer> node = find(tree, target);
                List<Move> after = moves.subList(found + 1, moves.size());
                if (node.left() != null && node.right() != null) {
                    assertEquals(Move.FIND_REPLACEMENT, after.get(0), where);
                    assertEquals(Move.SEEK_LEFT, after.get(1), where);
                    int replacementAt = after.indexOf(Move.REPLACEMENT_FOUND);
                    assertTrue(replacementAt >= 0, where);
                    // The replacement is the largest value below the deleted one, for both methods.
                    assertEquals(reference.lower(target), steps.get(found + 1 + replacementAt).value(), where);
                    assertEquals(copying ? Move.REMOVE_REPLACEMENT : Move.MERGE_REPLACE,
                            moves.get(moves.size() - 1), where);
                } else if (node.left() == null && node.right() == null) {
                    assertEquals(List.of(Move.DELETE_NODE), after, where);
                } else if (node.right() == null) {
                    assertEquals(List.of(Move.MERGE_REPLACE), after, where);
                } else {
                    assertEquals(List.of(Move.RIGHT_REPLACE), after, where);
                }
            }
        }
        // None of those traces changed the tree.
        assertEquals(levelsBefore, tree.breadthFirst());
        assertEquals(reference.size(), tree.size());
    }

    @Test
    void buildingAnyTraceLeavesTheTreeUnchanged() {
        BinarySearchTree<Integer> tree = exampleTree();
        List<Integer> levelsBefore = tree.breadthFirst();
        for (int target : new int[] {1, 3, 5, 7, 4, 100, -1}) {
            Traces.search(tree, target);
            Traces.insert(tree, target);
            Traces.deleteByCopying(tree, target);
            Traces.deleteByMerging(tree, target);
        }
        assertEquals(levelsBefore, tree.breadthFirst());
        assertEquals(6, tree.size());
    }

    // ---- every step can be shown -------------------------------------------------------

    @Test
    void everyKindOfMoveHasARule() {
        for (Move move : Move.values()) {
            assertFalse(new TraceStep<Integer>(1, null, move, "x").rule().isBlank(), move.toString());
        }
    }

    @Test
    void everyStepOfEveryDeleteTraceHasAnExplanationAndARule() {
        List<BinarySearchTree<Integer>> trees = List.of(exampleTree(), treeOf(50, 30, 40, 45, 70, 80),
                treeOf(50, 30, 70, 20, 40, 35), treeOf(5, 3, 4));
        for (BinarySearchTree<Integer> tree : trees) {
            for (int target : new ArrayList<>(tree.inOrder())) {
                for (boolean copying : BOTH_METHODS) {
                    for (TraceStep<Integer> step : deleteTrace(tree, target, copying)) {
                        assertFalse(step.explanation().isBlank(), name(copying) + " " + target);
                        assertFalse(step.rule().isBlank(), name(copying) + " " + target);
                    }
                }
            }
        }
    }
}