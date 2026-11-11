package cos212.bst;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class BinarySearchTreeTest {

    // ---- helpers -------------------------------------------------------------

    /** Checks the ordering invariant: every value lies strictly between the bounds of its ancestors. */
    private static <T extends Comparable<T>> boolean isValid(BSTNode<T> node, T low, T high) {
        if (node == null) {
            return true;
        }
        if (low != null && node.value.compareTo(low) <= 0) {
            return false;
        }
        if (high != null && node.value.compareTo(high) >= 0) {
            return false;
        }
        return isValid(node.left, low, node.value) && isValid(node.right, node.value, high);
    }

    private static <T extends Comparable<T>> void assertInvariant(BinarySearchTree<T> tree) {
        assertTrue(isValid(tree.root(), null, null), "ordering invariant broken");
    }

    // ---- empty tree ----------------------------------------------------------

    @Test
    void emptyTreeHasSizeZero() {
        assertEquals(0, new BinarySearchTree<Integer>().size());
    }

    @Test
    void emptyTreeContainsNothing() {
        assertFalse(new BinarySearchTree<Integer>().contains(1));
    }

    @Test
    void clearRemovesEveryValue() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        for (int v : new int[] {5, 3, 7}) {
            tree.insert(v);
        }
        tree.clear();
        assertEquals(0, tree.size());
        assertFalse(tree.contains(5));
        assertTrue(tree.insert(5));
    }

    // ---- insertion -----------------------------------------------------------

    @Test
    void insertIntoEmptyTreeCreatesRoot() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        assertTrue(tree.insert(5));
        assertEquals(1, tree.size());
        assertEquals(5, tree.root().value);
    }

    @Test
    void insertRejectsDuplicates() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        assertTrue(tree.insert(5));
        assertFalse(tree.insert(5));
        assertEquals(1, tree.size());
    }

    @Test
    void duplicateOfDeepNodeIsRejectedAndTreeIsUnchanged() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        for (int v : new int[] {5, 3, 7, 1, 6, 9}) {
            tree.insert(v);
        }
        assertFalse(tree.insert(6));
        assertEquals(6, tree.size());
        assertInvariant(tree);
    }

    @Test
    void smallerValueGoesLeftAndLargerGoesRight() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        tree.insert(5);
        tree.insert(3);
        tree.insert(7);
        assertEquals(3, tree.root().left.value);
        assertEquals(7, tree.root().right.value);
    }

    @Test
    void insertMatchesTheWorkedExampleFromTheDetailedDesign() {
        // Example tree from the Detailed Design section of the SE document: 5 at the
        // root, 3 and 7 below it, 1 below 3, and 6 and 9 below 7. Inserting 4 walks
        // left at 5, then right at 3, and attaches 4 as the right child of 3.
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        for (int v : new int[] {5, 3, 1, 7, 6, 9}) {
            tree.insert(v);
        }
        assertTrue(tree.insert(4));
        assertEquals(4, tree.root().left.right.value);
        assertInvariant(tree);
    }

    @Test
    void sortedInputBuildsAValidDegenerateTree() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        for (int v = 1; v <= 100; v++) {
            assertTrue(tree.insert(v));
        }
        assertEquals(100, tree.size());
        assertInvariant(tree);
        // Every value is still found, even though the cost is O(n).
        for (int v = 1; v <= 100; v++) {
            assertTrue(tree.contains(v));
        }
    }

    // ---- search --------------------------------------------------------------

    @Test
    void containsFindsEveryInsertedValue() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        int[] values = {50, 30, 70, 20, 40, 60, 80};
        for (int v : values) {
            tree.insert(v);
        }
        for (int v : values) {
            assertTrue(tree.contains(v), "missing " + v);
        }
    }

    @Test
    void containsRejectsMissingValues() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        for (int v : new int[] {50, 30, 70}) {
            tree.insert(v);
        }
        assertFalse(tree.contains(40));
        assertFalse(tree.contains(0));
        assertFalse(tree.contains(100));
    }

    // ---- boundary values and null --------------------------------------------

    @Test
    void extremeIntegerValuesAreOrderedCorrectly() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        tree.insert(0);
        assertTrue(tree.insert(Integer.MIN_VALUE));
        assertTrue(tree.insert(Integer.MAX_VALUE));
        assertTrue(tree.contains(Integer.MIN_VALUE));
        assertTrue(tree.contains(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, tree.root().left.value);
        assertEquals(Integer.MAX_VALUE, tree.root().right.value);
        assertInvariant(tree);
    }

    @Test
    void insertNullThrows() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        assertThrows(NullPointerException.class, () -> tree.insert(null));
        assertEquals(0, tree.size());
    }

    @Test
    void containsNullThrows() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        assertThrows(NullPointerException.class, () -> tree.contains(null));
    }

    // ---- in-order traversal --------------------------------------------------

    @Test
    void inOrderListsValuesInSortedOrder() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        for (int v : new int[] {5, 3, 7, 1, 6, 9}) {
            tree.insert(v);
        }
        assertEquals(java.util.List.of(1, 3, 5, 6, 7, 9), tree.inOrder());
    }

    @Test
    void inOrderOfEmptyTreeIsEmpty() {
        assertEquals(java.util.List.of(), new BinarySearchTree<Integer>().inOrder());
    }

    // ---- property test against TreeSet ---------------------------------------

    @Test
    void randomOperationsAgreeWithTreeSet() {
        long seed = 42L; // fixed, so that a failure can be reproduced
        Random random = new Random(seed);
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        TreeSet<Integer> reference = new TreeSet<>();

        for (int i = 0; i < 2000; i++) {
            int value = random.nextInt(500) - 250;
            if (random.nextBoolean()) {
                boolean added = tree.insert(value);
                assertEquals(reference.add(value), added, "insert " + value + " (seed " + seed + ")");
            } else {
                assertEquals(reference.contains(value), tree.contains(value),
                        "contains " + value + " (seed " + seed + ")");
            }
        }

        assertEquals(reference.size(), tree.size(), "size (seed " + seed + ")");
        assertInvariant(tree);
    }
}
