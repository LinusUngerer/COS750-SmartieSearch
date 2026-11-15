package cos212.ui;

import cos212.bst.BinarySearchTree;
import java.util.ArrayDeque;
import java.util.Deque;
/**
 * Remembers copies of a tree from before each change, so the student can step back
 * (undo) and forward (redo). It keeps at most {@code depth} earlier states and drops
 * the oldest one when that is exceeded. Making a new change clears the redo states,
 * because they no longer follow on from the tree.
 *
 * @param <T> the type of value stored in the tree
 */
final class TreeHistory<T extends Comparable<T>> {

    private record Entry<T extends Comparable<T>>(BinarySearchTree<T> snapshot, String label) {
    }

    private final int depth;
    private final Deque<Entry<T>> undoStates = new ArrayDeque<>();
    private final Deque<Entry<T>> redoStates = new ArrayDeque<>();

    /**
     * @param depth how many earlier states to keep; at least 1
     */
    TreeHistory(int depth) {
        if (depth < 1) {
            throw new IllegalArgumentException("depth must be at least 1");
        }
        this.depth = depth;
    }

    /** Saves the tree as it is now. Call this just before a change is applied. */
    void record(BinarySearchTree<T> tree, String label) {
        undoStates.push(new Entry<>(tree.copy(), label));
        while (undoStates.size() > depth) {
            undoStates.removeLast();
        }
        redoStates.clear();
    }

    boolean canUndo() {
        return !undoStates.isEmpty();
    }

    boolean canRedo() {
        return !redoStates.isEmpty();
    }

    /**
     * @return what undo would undo, for example, "Delete Copy 45"
     * @throws IllegalStateException if there is nothing to undo
     */
    String undoLabel() {
        if (undoStates.isEmpty()) {
            throw new IllegalStateException("nothing to undo");
        }
        return undoStates.getFirst().label();
    }

    /**
     * @return what redo would redo
     * @throws IllegalStateException if there is nothing to redo
     */
    String redoLabel() {
        if (redoStates.isEmpty()) {
            throw new IllegalStateException("nothing to redo");
        }
        return redoStates.getFirst().label();
    }

    /**
     * Puts the tree back to the state before the last change and keeps the current state for redo.
     *
     * @return the label of the change that was undone
     */
    String undo(BinarySearchTree<T> tree) {
        Entry<T> entry = undoStates.pop();
        redoStates.push(new Entry<>(tree.copy(), entry.label()));
        tree.restoreFrom(entry.snapshot());
        return entry.label();
    }

    /**
     * Re-applies the change that was last undone.
     *
     * @return the label of the change that was redone
     */
    String redo(BinarySearchTree<T> tree) {
        Entry<T> entry = redoStates.pop();
        undoStates.push(new Entry<>(tree.copy(), entry.label()));
        tree.restoreFrom(entry.snapshot());
        return entry.label();
    }
}