package cos212.bst;

import java.util.List;

/**
 * A binary search tree that enforces the ordering invariant: every value in the
 * left subtree of a node is smaller than the node's value, and every value in the
 * right subtree is larger.
 *
 * <p>Scaffold: method bodies are placeholders. Each one is implemented in
 * increment I1 and covered by the tests in {@code BinarySearchTreeTest}.
 *
 * @param <T> the type of value stored; must be comparable
 */
public class BinarySearchTree<T extends Comparable<T>> {

    private BSTNode<T> root;
    private int size;

    /**
     * Inserts a value, unless it is already present.
     *
     * @return {@code true} if the value was added, {@code false} if it was a duplicate
     */
    public boolean insert(T value) {
        throw new UnsupportedOperationException("increment I1");
    }

    /**
     * @return {@code true} if the value is in the tree
     */
    public boolean contains(T value) {
        throw new UnsupportedOperationException("increment I1");
    }

    /**
     * @return the stored values in sorted order
     */
    public List<T> inOrder() {
        throw new UnsupportedOperationException("increment I3");
    }

    /**
     * @return the number of values stored
     */
    public int size() {
        return size;
    }

    /**
     * @return the number of edges on the longest path from the root to a leaf,
     *         or -1 for an empty tree
     */
    public int height() {
        throw new UnsupportedOperationException("increment I3");
    }

    BSTNode<T> root() {
        return root;
    }
}
