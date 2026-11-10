package cos212.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import cos212.bst.BinarySearchTree;
import cos212.trace.TraceStep.Move;
import java.util.List;
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
    void everyStepHasAnExplanation() {
        for (TraceStep<Integer> step : Traces.search(exampleTree(), 4)) {
            assertFalse(step.explanation().isBlank());
        }
    }
}
