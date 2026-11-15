package cos212.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cos212.bst.BinarySearchTree;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TreeHistoryTest {

    private static BinarySearchTree<Integer> treeOf(int... values) {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        for (int v : values) {
            tree.insert(v);
        }
        return tree;
    }

    // ---- when Undo and Redo are possible (what greys the buttons out) --------

    @Test
    void aNewHistoryHasNothingToUndoOrRedo() {
        TreeHistory<Integer> history = new TreeHistory<>(1);
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
    }

    @Test
    void recordingAStateMakesUndoPossibleButNotRedo() {
        TreeHistory<Integer> history = new TreeHistory<>(1);
        history.record(treeOf(5), "Insert 3");
        assertTrue(history.canUndo());
        assertFalse(history.canRedo());
    }

    @Test
    void undoMakesRedoPossibleAndUsesUpTheOnlyState() {
        BinarySearchTree<Integer> tree = treeOf(5);
        TreeHistory<Integer> history = new TreeHistory<>(1);
        history.record(tree, "Insert 3");
        tree.insert(3);
        history.undo(tree);
        assertFalse(history.canUndo());
        assertTrue(history.canRedo());
    }

    @Test
    void redoUsesUpTheRedoStateAndMakesUndoPossibleAgain() {
        BinarySearchTree<Integer> tree = treeOf(5);
        TreeHistory<Integer> history = new TreeHistory<>(1);
        history.record(tree, "Insert 3");
        tree.insert(3);
        history.undo(tree);
        history.redo(tree);
        assertTrue(history.canUndo());
        assertFalse(history.canRedo());
    }

    // ---- what Undo and Redo restore ------------------------------------------

    @Test
    void undoPutsBackTheTreeFromBeforeTheChange() {
        BinarySearchTree<Integer> tree = treeOf(50, 30, 70);
        List<Integer> before = tree.breadthFirst();
        TreeHistory<Integer> history = new TreeHistory<>(1);

        history.record(tree, "Delete Copy 50");
        tree.deleteByCopying(50);
        assertEquals("Delete Copy 50", history.undo(tree));

        assertEquals(before, tree.breadthFirst());
        assertEquals(3, tree.size());
    }

    @Test
    void redoAppliesTheUndoneChangeAgain() {
        BinarySearchTree<Integer> tree = treeOf(50, 30, 70);
        TreeHistory<Integer> history = new TreeHistory<>(1);

        history.record(tree, "Delete Merge 50");
        tree.deleteByMerging(50);
        List<Integer> after = tree.breadthFirst();

        history.undo(tree);
        assertEquals("Delete Merge 50", history.redo(tree));

        assertEquals(after, tree.breadthFirst());
        assertEquals(2, tree.size());
    }

    @Test
    void theSavedStateIsNotAffectedByLaterChangesToTheTree() {
        BinarySearchTree<Integer> tree = treeOf(50, 30, 70);
        List<Integer> before = tree.breadthFirst();
        TreeHistory<Integer> history = new TreeHistory<>(1);

        history.record(tree, "Insert 10");
        tree.insert(10);
        tree.insert(20);
        tree.deleteByCopying(30);
        tree.deleteByMerging(50);
        history.undo(tree);

        assertEquals(before, tree.breadthFirst());
    }

    @Test
    void theRestoredTreeCanBeChangedWithoutDamagingTheRedoState() {
        BinarySearchTree<Integer> tree = treeOf(50, 30, 70);
        TreeHistory<Integer> history = new TreeHistory<>(1);

        history.record(tree, "Delete Copy 30");
        tree.deleteByCopying(30);
        List<Integer> after = tree.breadthFirst();
        history.undo(tree);
        tree.insert(99); // changes the live tree, but redo must still give the tree as it was after the delete

        history.redo(tree);
        assertEquals(after, tree.breadthFirst());
    }

    // ---- a new change and the depth limit ---------------------------------------

    @Test
    void aNewChangeClearsTheRedoState() {
        BinarySearchTree<Integer> tree = treeOf(5);
        TreeHistory<Integer> history = new TreeHistory<>(1);
        history.record(tree, "Insert 3");
        tree.insert(3);
        history.undo(tree);
        assertTrue(history.canRedo());

        history.record(tree, "Insert 8");
        tree.insert(8);
        assertFalse(history.canRedo());
        assertTrue(history.canUndo());
    }

    @Test
    void depthOneKeepsOnlyTheLatestState() {
        BinarySearchTree<Integer> tree = treeOf(1);
        TreeHistory<Integer> history = new TreeHistory<>(1);
        history.record(tree, "Insert 2");
        tree.insert(2);
        history.record(tree, "Insert 3");
        tree.insert(3);

        history.undo(tree);
        assertEquals(List.of(1, 2), tree.inOrder());
        assertFalse(history.canUndo(), "the older state should have been dropped");
    }

    @Test
    void depthThreeKeepsThreeStatesAndDropsTheOldest() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        TreeHistory<Integer> history = new TreeHistory<>(3);
        for (int v = 1; v <= 4; v++) {
            history.record(tree, "Insert " + v);
            tree.insert(v);
        }

        assertEquals("Insert 4", history.undo(tree));
        assertEquals(List.of(1, 2, 3), tree.inOrder());
        assertEquals("Insert 3", history.undo(tree));
        assertEquals(List.of(1, 2), tree.inOrder());
        assertEquals("Insert 2", history.undo(tree));
        assertEquals(List.of(1), tree.inOrder());
        assertFalse(history.canUndo(), "the state before Insert 1 should have been dropped");
    }

    @Test
    void undoThenRedoWalksBackAndForthThroughTheHistory() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        TreeHistory<Integer> history = new TreeHistory<>(3);
        for (int v = 1; v <= 4; v++) {
            history.record(tree, "Insert " + v);
            tree.insert(v);
        }
        for (int i = 0; i < 3; i++) {
            history.undo(tree);
        }
        assertEquals(List.of(1), tree.inOrder());

        assertEquals("Insert 2", history.redo(tree));
        assertEquals(List.of(1, 2), tree.inOrder());
        assertEquals("Insert 3", history.redo(tree));
        assertEquals(List.of(1, 2, 3), tree.inOrder());
        assertEquals("Insert 4", history.redo(tree));
        assertEquals(List.of(1, 2, 3, 4), tree.inOrder());
        assertFalse(history.canRedo());
        assertTrue(history.canUndo());
    }

    // ---- labels, bad calls and bad depths ---------------------------------------

    @Test
    void labelsNameTheChangeThatWouldBeUndoneOrRedone() {
        BinarySearchTree<Integer> tree = treeOf(45, 30);
        TreeHistory<Integer> history = new TreeHistory<>(1);
        history.record(tree, "Delete Copy 45");
        tree.deleteByCopying(45);
        assertEquals("Delete Copy 45", history.undoLabel());

        history.undo(tree);
        assertEquals("Delete Copy 45", history.redoLabel());
    }

    @Test
    void askingForALabelWithNothingToStepToThrows() {
        TreeHistory<Integer> history = new TreeHistory<>(1);
        assertThrows(IllegalStateException.class, history::undoLabel);
        assertThrows(IllegalStateException.class, history::redoLabel);
    }

    @Test
    void undoOrRedoWithNothingToStepToThrows() {
        BinarySearchTree<Integer> tree = treeOf(5);
        TreeHistory<Integer> history = new TreeHistory<>(1);
        assertThrows(NoSuchElementException.class, () -> history.undo(tree));
        assertThrows(NoSuchElementException.class, () -> history.redo(tree));
        assertEquals(List.of(5), tree.inOrder());
    }

    @Test
    void aDepthBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TreeHistory<Integer>(0));
        assertThrows(IllegalArgumentException.class, () -> new TreeHistory<Integer>(-1));
    }

    // ---- property test against a reference model -----------------------------------

    /** A state the reference model expects: the tree in level order, and the change's label. */
    private record Snapshot(List<Integer> levels, String label) {
    }

    @Test
    void randomChangesUndosAndRedosAgreeWithAReferenceModel() {
        long seed = 2026L; // fixed, so that a failure can be reproduced
        Random random = new Random(seed);

        for (int depth : new int[] {1, 3}) {
            BinarySearchTree<Integer> tree = new BinarySearchTree<>();
            TreeHistory<Integer> history = new TreeHistory<>(depth);
            Deque<Snapshot> modelUndo = new ArrayDeque<>();
            Deque<Snapshot> modelRedo = new ArrayDeque<>();

            for (int i = 0; i < 1500; i++) {
                int op = random.nextInt(12);
                int value = random.nextInt(25);
                String where = " (depth " + depth + ", step " + i + ", seed " + seed + ")";

                if (op <= 7) {
                    // A change. As on the screen, a change that does nothing saves no state.
                    String label = null;
                    if (op <= 4 && !tree.contains(value)) {
                        label = "Insert " + value;
                    } else if (op == 5 && tree.contains(value)) {
                        label = "Delete Copy " + value;
                    } else if (op == 6 && tree.contains(value)) {
                        label = "Delete Merge " + value;
                    } else if (op == 7 && tree.size() > 0) {
                        label = "New tree";
                    }
                    if (label != null) {
                        modelUndo.addFirst(new Snapshot(tree.breadthFirst(), label));
                        while (modelUndo.size() > depth) {
                            modelUndo.removeLast();
                        }
                        modelRedo.clear();
                        history.record(tree, label);
                        if (op <= 4) {
                            tree.insert(value);
                        } else if (op == 5) {
                            tree.deleteByCopying(value);
                        } else if (op == 6) {
                            tree.deleteByMerging(value);
                        } else {
                            tree.clear();
                        }
                    }
                } else if (op <= 9) {
                    if (!modelUndo.isEmpty()) {
                        Snapshot expected = modelUndo.removeFirst();
                        modelRedo.addFirst(new Snapshot(tree.breadthFirst(), expected.label()));
                        assertEquals(expected.label(), history.undo(tree), "undo label" + where);
                        assertEquals(expected.levels(), tree.breadthFirst(), "undo tree" + where);
                    }
                } else {
                    if (!modelRedo.isEmpty()) {
                        Snapshot expected = modelRedo.removeFirst();
                        modelUndo.addFirst(new Snapshot(tree.breadthFirst(), expected.label()));
                        assertEquals(expected.label(), history.redo(tree), "redo label" + where);
                        assertEquals(expected.levels(), tree.breadthFirst(), "redo tree" + where);
                    }
                }

                assertEquals(!modelUndo.isEmpty(), history.canUndo(), "canUndo" + where);
                assertEquals(!modelRedo.isEmpty(), history.canRedo(), "canRedo" + where);
                if (history.canUndo()) {
                    assertEquals(modelUndo.peekFirst().label(), history.undoLabel(), "undoLabel" + where);
                }
                if (history.canRedo()) {
                    assertEquals(modelRedo.peekFirst().label(), history.redoLabel(), "redoLabel" + where);
                }
            }
        }
    }
}