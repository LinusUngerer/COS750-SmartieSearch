package cos212.bst;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A binary search tree that enforces the ordering invariant: every value in the
 * left subtree of a node is smaller than the node's value, and every value in the
 * right subtree is larger.
 *
 * <p>Insertion and search are iterative, so their extra space is constant and a
 * degenerate tree cannot exhaust the call stack (decision D6 in the SE document).
 * In-order traversal and height are added in increment I3.
 *
 * @param <T> the type of value stored; must be comparable
 */
public class BinarySearchTree<T extends Comparable<T>> {

    private BSTNode<T> root;
    private int size;

    /**
     * Inserts a value as a new leaf, unless it is already present.
     *
     * @param value the value to insert; must not be null
     * @return {@code true} if the value was added, {@code false} if it was a duplicate
     * @throws NullPointerException if {@code value} is null
     */
    public boolean insert(T value) {
        Objects.requireNonNull(value, "value");
        if (root == null) {
            root = new BSTNode<>(value);
            size++;
            return true;
        }
        BSTNode<T> current = root;
        while (true) {
            int cmp = value.compareTo(current.value);
            if (cmp == 0) {
                return false;
            }
            if (cmp < 0) {
                if (current.left == null) {
                    current.left = new BSTNode<>(value);
                    size++;
                    return true;
                }
                current = current.left;
            } else {
                if (current.right == null) {
                    current.right = new BSTNode<>(value);
                    size++;
                    return true;
                }
                current = current.right;
            }
        }
    }

    /**
     * @param value the value to look for; must not be null
     * @return {@code true} if the value is in the tree
     * @throws NullPointerException if {@code value} is null
     */
    public boolean contains(T value) {
        Objects.requireNonNull(value, "value");
        BSTNode<T> current = root;
        while (current != null) {
            int cmp = value.compareTo(current.value);
            if (cmp == 0) {
                return true;
            }
            current = cmp < 0 ? current.left : current.right;
        }
        return false;
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
     * A read-only view of one node, used to draw the tree. The view has no setters,
     * so the ordering invariant can only change through {@link #insert}.
     */
    public interface NodeView<T> {

        /** @return the value stored in this node */
        T value();

        /** @return the left child, or {@code null} if there is none */
        NodeView<T> left();

        /** @return the right child, or {@code null} if there is none */
        NodeView<T> right();
    }

    /**
     * @return a read-only view of the root, or {@code null} for an empty tree
     */
    public NodeView<T> rootView() {
        return view(root);
    }

    private static <T> NodeView<T> view(BSTNode<T> node) {
        if (node == null) {
            return null;
        }
        return new NodeView<>() {
            @Override
            public T value() {
                return node.value;
            }

            @Override
            public NodeView<T> left() {
                return view(node.left);
            }

            @Override
            public NodeView<T> right() {
                return view(node.right);
            }
        };
    }

    /**
     * Lists the values that a search for {@code value} compares, in order from the root.
     * The list ends at the matching value, or at the last node before an empty link.
     *
     * @param value the value to search for; must not be null
     * @return the compared values, or an empty list for an empty tree
     * @throws NullPointerException if {@code value} is null
     */
    public List<T> comparedPath(T value) {
        Objects.requireNonNull(value, "value");
        List<T> path = new ArrayList<>();
        BSTNode<T> current = root;
        while (current != null) {
            path.add(current.value);
            int cmp = value.compareTo(current.value);
            if (cmp == 0) {
                break;
            }
            current = cmp < 0 ? current.left : current.right;
        }
        return path;
    }

    /**
     * @return the number of edges on the longest path from the root to a leaf,
     *         or -1 for an empty tree
     */
    public int height() {
        throw new UnsupportedOperationException("increment I3");
    }

    /**
     * Exposes the root to the tests in this package, so that the ordering invariant
     * can be checked after each operation.
     */
    BSTNode<T> root() {
        return root;
    }
}
