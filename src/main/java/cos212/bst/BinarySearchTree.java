package cos212.bst;

import java.util.*;

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
        List<T> values = new ArrayList<>(size);
        inOrder(root, values);
        return values;
    }

    private static <T> void inOrder(BSTNode<T> node, List<T> values) {
        if (node == null) {
            return;
        }
        inOrder(node.left, values);
        values.add(node.value);
        inOrder(node.right, values);
    }

    /**
     * @return the stored values level by level, from the root down, left to right
     */
    public List<T> breadthFirst() {
        List<T> values = new ArrayList<>(size);
        if (root == null) {
            return values;
        }
        Queue<BSTNode<T>> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            BSTNode<T> node = queue.remove();
            values.add(node.value);
            if (node.left != null) {
                queue.add(node.left);
            }
            if (node.right != null) {
                queue.add(node.right);
            }
        }
        return values;
    }

    /**
     * @return the number of values stored
     */
    public int size() {
        return size;
    }

    /**
     * Removes every value, leaving an empty tree.
     */
    public void clear() {
        root = null;
        size = 0;
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

    /**
     * Deletes a value by copying. A node with two children takes the value of its
     * in-order predecessor (the largest value in its left subtree), and the
     * predecessor's node is then removed instead.
     *
     * @param value the value to delete; must not be null
     * @return {@code true} if the value was found and removed
     * @throws NullPointerException if {@code value} is null
     */
    public boolean deleteByCopying(T value) {
        Objects.requireNonNull(value, "value");
        BSTNode<T> parent = null;
        BSTNode<T> node = root;
        while (node != null) {
            int cmp = value.compareTo(node.value);
            if (cmp == 0) {
                break;
            }
            parent = node;
            node = cmp < 0 ? node.left : node.right;
        }
        if (node == null) {
            return false;
        }
        if (node.left != null && node.right != null) {
            BSTNode<T> predecessorParent = node;
            BSTNode<T> predecessor = node.left;
            while (predecessor.right != null) {
                predecessorParent = predecessor;
                predecessor = predecessor.right;
            }
            node.value = predecessor.value;
            replaceChild(predecessorParent, predecessor, predecessor.left);
        } else {
            replaceChild(parent, node, node.left != null ? node.left : node.right);
        }
        size--;
        return true;
    }

    /**
     * Deletes a value by merging. The node's right subtree is attached as the right
     * child of the rightmost node of its left subtree, and the left subtree takes
     * the deleted node's place.
     *
     * @param value the value to delete; must not be null
     * @return {@code true} if the value was found and removed
     * @throws NullPointerException if {@code value} is null
     */
    public boolean deleteByMerging(T value) {
        Objects.requireNonNull(value, "value");
        BSTNode<T> parent = null;
        BSTNode<T> node = root;
        while (node != null) {
            int cmp = value.compareTo(node.value);
            if (cmp == 0) {
                break;
            }
            parent = node;
            node = cmp < 0 ? node.left : node.right;
        }
        if (node == null) {
            return false;
        }
        BSTNode<T> replacement;
        if (node.left == null) {
            replacement = node.right;
        } else {
            replacement = node.left;
            BSTNode<T> rightmost = node.left;
            while (rightmost.right != null) {
                rightmost = rightmost.right;
            }
            rightmost.right = node.right;
        }
        replaceChild(parent, node, replacement);
        size--;
        return true;
    }

    /** Puts {@code replacement} where {@code child} hung under {@code parent} (or at the root). */
    private void replaceChild(BSTNode<T> parent, BSTNode<T> child, BSTNode<T> replacement) {
        if (parent == null) {
            root = replacement;
        } else if (parent.left == child) {
            parent.left = replacement;
        } else {
            parent.right = replacement;
        }
    }

    /**
     * @return an independent copy of this tree. Changing either tree never affects the other.
     */
    public BinarySearchTree<T> copy() {
        BinarySearchTree<T> clone = new BinarySearchTree<>();
        clone.size = size;
        if (root == null) {
            return clone;
        }
        Deque<BSTNode<T>> originals = new ArrayDeque<>();
        Deque<BSTNode<T>> duplicates = new ArrayDeque<>();
        clone.root = new BSTNode<>(root.value);
        originals.push(root);
        duplicates.push(clone.root);
        while (!originals.isEmpty()) {
            BSTNode<T> source = originals.pop();
            BSTNode<T> target = duplicates.pop();
            if (source.left != null) {
                target.left = new BSTNode<>(source.left.value);
                originals.push(source.left);
                duplicates.push(target.left);
            }
            if (source.right != null) {
                target.right = new BSTNode<>(source.right.value);
                originals.push(source.right);
                duplicates.push(target.right);
            }
        }
        return clone;
    }

    /**
     * Replaces this tree's contents with a copy of another tree's, in place. The other tree
     * is not shared, so later changes to this tree never reach it.
     *
     * @param other the tree to copy from; must not be null
     * @throws NullPointerException if {@code other} is null
     */
    public void restoreFrom(BinarySearchTree<T> other) {
        Objects.requireNonNull(other, "other");
        BinarySearchTree<T> snapshot = other.copy();
        root = snapshot.root;
        size = snapshot.size;
    }

}
