package cos212.bst;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
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

    // ==== deletion, breadth-first traversal and copying ============================

    private static BinarySearchTree<Integer> treeOf(int... values) {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        for (int v : values) {
            tree.insert(v);
        }
        return tree;
    }

    /** The shape of a tree as text, for example (5 (3 . .) .). A dot is an empty link. */
    private static <T extends Comparable<T>> String shape(BSTNode<T> node) {
        if (node == null) {
            return ".";
        }
        return "(" + node.value + " " + shape(node.left) + " " + shape(node.right) + ")";
    }

    private static <T extends Comparable<T>> int height(BSTNode<T> node) {
        return node == null ? -1 : 1 + Math.max(height(node.left), height(node.right));
    }

    private static <T extends Comparable<T>> void collectNodes(BSTNode<T> node, Set<BSTNode<T>> out) {
        if (node == null) {
            return;
        }
        out.add(node);
        collectNodes(node.left, out);
        collectNodes(node.right, out);
    }

    private static boolean delete(BinarySearchTree<Integer> tree, int value, boolean copying) {
        return copying ? tree.deleteByCopying(value) : tree.deleteByMerging(value);
    }

    private static String name(boolean copying) {
        return copying ? "delete by copying" : "delete by merging";
    }

    private static final boolean[] BOTH_METHODS = {true, false};

    // ---- deletion: behaviour shared by both methods --------------------------

    @Test
    void deleteFromAnEmptyTreeReturnsFalse() {
        for (boolean copying : BOTH_METHODS) {
            BinarySearchTree<Integer> tree = new BinarySearchTree<>();
            assertFalse(delete(tree, 5, copying), name(copying));
            assertEquals(0, tree.size(), name(copying));
        }
    }

    @Test
    void deleteOfAnAbsentValueLeavesTheTreeUnchanged() {
        for (boolean copying : BOTH_METHODS) {
            BinarySearchTree<Integer> tree = treeOf(5, 3, 7);
            String before = shape(tree.root());
            for (int absent : new int[] {4, 0, 100}) {
                assertFalse(delete(tree, absent, copying), name(copying) + " " + absent);
            }
            assertEquals(before, shape(tree.root()), name(copying));
            assertEquals(3, tree.size(), name(copying));
        }
    }

    @Test
    void deleteNullThrows() {
        BinarySearchTree<Integer> tree = treeOf(5);
        assertThrows(NullPointerException.class, () -> tree.deleteByCopying(null));
        assertThrows(NullPointerException.class, () -> tree.deleteByMerging(null));
        assertEquals(1, tree.size());
    }

    @Test
    void deleteOfALeafRemovesOnlyThatNode() {
        for (boolean copying : BOTH_METHODS) {
            BinarySearchTree<Integer> tree = treeOf(50, 30, 70);
            assertTrue(delete(tree, 30, copying), name(copying));
            assertEquals("(50 . (70 . .))", shape(tree.root()), name(copying));
            assertEquals(2, tree.size(), name(copying));
            assertFalse(tree.contains(30), name(copying));
            assertInvariant(tree);
        }
    }

    @Test
    void deleteOfANodeWithOnlyALeftChildPromotesThatChild() {
        for (boolean copying : BOTH_METHODS) {
            BinarySearchTree<Integer> tree = treeOf(50, 30, 20);
            assertTrue(delete(tree, 30, copying), name(copying));
            assertEquals("(50 (20 . .) .)", shape(tree.root()), name(copying));
            assertEquals(2, tree.size(), name(copying));
            assertInvariant(tree);
        }
    }

    @Test
    void deleteOfANodeWithOnlyARightChildPromotesThatChild() {
        for (boolean copying : BOTH_METHODS) {
            BinarySearchTree<Integer> tree = treeOf(50, 30, 40);
            assertTrue(delete(tree, 30, copying), name(copying));
            assertEquals("(50 (40 . .) .)", shape(tree.root()), name(copying));
            assertEquals(2, tree.size(), name(copying));
            assertInvariant(tree);
        }
    }

    @Test
    void deleteOfTheRootWithOneOrNoChildren() {
        for (boolean copying : BOTH_METHODS) {
            BinarySearchTree<Integer> alone = treeOf(5);
            assertTrue(delete(alone, 5, copying), name(copying));
            assertNull(alone.root());
            assertEquals(0, alone.size(), name(copying));

            BinarySearchTree<Integer> leftOnly = treeOf(5, 3);
            assertTrue(delete(leftOnly, 5, copying), name(copying));
            assertEquals("(3 . .)", shape(leftOnly.root()), name(copying));

            BinarySearchTree<Integer> rightOnly = treeOf(5, 7);
            assertTrue(delete(rightOnly, 5, copying), name(copying));
            assertEquals("(7 . .)", shape(rightOnly.root()), name(copying));
        }
    }

    @Test
    void deletingEveryValueInTurnEmptiesTheTree() {
        int[] values = {50, 30, 70, 20, 40, 60, 80, 10, 25, 35, 45, 55, 65, 75, 85};
        List<Integer> insertion = new ArrayList<>();
        for (int v : values) {
            insertion.add(v);
        }
        List<Integer> ascending = new ArrayList<>(insertion);
        Collections.sort(ascending);
        List<Integer> shuffled = new ArrayList<>(insertion);
        Collections.shuffle(shuffled, new Random(7));

        for (boolean copying : BOTH_METHODS) {
            for (List<Integer> order : List.of(insertion, ascending, shuffled)) {
                BinarySearchTree<Integer> tree = treeOf(values);
                int remaining = values.length;
                for (int v : order) {
                    assertTrue(delete(tree, v, copying), name(copying) + " " + v);
                    remaining--;
                    assertEquals(remaining, tree.size(), name(copying) + " " + v);
                    assertFalse(tree.contains(v), name(copying) + " " + v);
                    assertInvariant(tree);
                }
                assertEquals(0, tree.size(), name(copying));
                assertNull(tree.root());
            }
        }
    }

    // ---- deletion: nodes with two children -----------------------------------

    @Test
    void deleteByCopyingUsesTheLargestValueInTheLeftSubtree() {
        // The replacement is the left child itself: 20 moves up and keeps 40 as its right child.
        BinarySearchTree<Integer> shallow = treeOf(50, 30, 70, 20, 40);
        assertTrue(shallow.deleteByCopying(30));
        assertEquals("(50 (20 . (40 . .)) (70 . .))", shape(shallow.root()));
        assertEquals(4, shallow.size());
        assertInvariant(shallow);

        // The replacement is deeper and has a left child of its own, and the deleted node is the root.
        BinarySearchTree<Integer> deep = treeOf(50, 30, 70, 20, 40, 35);
        assertTrue(deep.deleteByCopying(50));
        assertEquals("(40 (30 (20 . .) (35 . .)) (70 . .))", shape(deep.root()));
        assertEquals(5, deep.size());
        assertInvariant(deep);
    }

    @Test
    void deleteByMergingHangsTheRightSubtreeBelowTheLeftSubtreesRightmostNode() {
        // The rightmost node of the left subtree is the left child itself.
        BinarySearchTree<Integer> shallow = treeOf(50, 30, 70, 20, 40);
        assertTrue(shallow.deleteByMerging(30));
        assertEquals("(50 (20 . (40 . .)) (70 . .))", shape(shallow.root()));
        assertEquals(4, shallow.size());
        assertInvariant(shallow);

        // The rightmost node is deeper, and the deleted node is the root.
        BinarySearchTree<Integer> deep = treeOf(50, 30, 70, 20, 40, 35);
        assertTrue(deep.deleteByMerging(50));
        assertEquals("(30 (20 . .) (40 (35 . .) (70 . .)))", shape(deep.root()));
        assertEquals(5, deep.size());
        assertInvariant(deep);
    }

    @Test
    void deleteByMergingCanMakeTheTreeTallerButCopyingDoesNotHere() {
        int[] values = {50, 30, 40, 45, 70, 80};
        BinarySearchTree<Integer> merged = treeOf(values);
        BinarySearchTree<Integer> copied = treeOf(values);
        assertEquals(3, height(merged.root()));

        assertTrue(merged.deleteByMerging(50));
        assertTrue(copied.deleteByCopying(50));

        assertEquals("(30 . (40 . (45 . (70 . (80 . .)))))", shape(merged.root()));
        assertEquals("(45 (30 . (40 . .)) (70 . (80 . .)))", shape(copied.root()));
        assertEquals(4, height(merged.root()));
        assertEquals(2, height(copied.root()));
        assertInvariant(merged);
        assertInvariant(copied);
    }

    @Test
    void randomInsertsAndDeletesAgreeWithTreeSetForBothMethods() {
        long seed = 99L; // fixed, so that a failure can be reproduced
        Random random = new Random(seed);
        BinarySearchTree<Integer> copied = new BinarySearchTree<>();
        BinarySearchTree<Integer> merged = new BinarySearchTree<>();
        TreeSet<Integer> reference = new TreeSet<>();

        for (int i = 0; i < 3000; i++) {
            int value = random.nextInt(120) - 60;
            int op = random.nextInt(3);
            String where = " (step " + i + ", value " + value + ", seed " + seed + ")";
            if (op == 0) {
                boolean expected = reference.add(value);
                assertEquals(expected, copied.insert(value), "insert, copied tree" + where);
                assertEquals(expected, merged.insert(value), "insert, merged tree" + where);
            } else if (op == 1) {
                boolean expected = reference.remove(value);
                assertEquals(expected, copied.deleteByCopying(value), "delete by copying" + where);
                assertEquals(expected, merged.deleteByMerging(value), "delete by merging" + where);
            } else {
                assertEquals(reference.contains(value), copied.contains(value), "contains, copied tree" + where);
                assertEquals(reference.contains(value), merged.contains(value), "contains, merged tree" + where);
            }
            assertInvariant(copied);
            assertInvariant(merged);
            assertEquals(reference.size(), copied.size(), "size, copied tree" + where);
            assertEquals(reference.size(), merged.size(), "size, merged tree" + where);
        }

        List<Integer> sorted = new ArrayList<>(reference);
        assertEquals(sorted, copied.inOrder(), "in-order, copied tree (seed " + seed + ")");
        assertEquals(sorted, merged.inOrder(), "in-order, merged tree (seed " + seed + ")");
        for (BinarySearchTree<Integer> tree : List.of(copied, merged)) {
            List<Integer> levelOrder = new ArrayList<>(tree.breadthFirst());
            Collections.sort(levelOrder);
            assertEquals(sorted, levelOrder, "breadth-first holds each value once (seed " + seed + ")");
        }
    }

    // ---- breadth-first traversal ---------------------------------------------

    @Test
    void breadthFirstOfAnEmptyTreeIsEmpty() {
        assertEquals(List.of(), new BinarySearchTree<Integer>().breadthFirst());
    }

    @Test
    void breadthFirstListsLevelByLevelFromLeftToRight() {
        // Example tree from the Detailed Design: 5; 3 and 7; 1, 6 and 9.
        assertEquals(List.of(5, 3, 7, 1, 6, 9), treeOf(5, 3, 1, 7, 6, 9).breadthFirst());
    }

    @Test
    void breadthFirstDiffersFromInOrderWhenADeepBranchComesBeforeAShallowOne() {
        BinarySearchTree<Integer> tree = treeOf(50, 30, 70, 40);
        assertEquals(List.of(50, 30, 70, 40), tree.breadthFirst());
        assertEquals(List.of(30, 40, 50, 70), tree.inOrder());
    }

    @Test
    void breadthFirstOfADegenerateTreeFollowsTheChain() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        List<Integer> expected = new ArrayList<>();
        for (int v = 1; v <= 20; v++) {
            tree.insert(v);
            expected.add(v);
        }
        assertEquals(expected, tree.breadthFirst());
    }

    // ---- copying and restoring -----------------------------------------------

    @Test
    void copyHasTheSameShapeAndSizeButSharesNoNodes() {
        BinarySearchTree<Integer> original = treeOf(5, 3, 1, 7, 6, 9);
        BinarySearchTree<Integer> copy = original.copy();
        assertEquals(shape(original.root()), shape(copy.root()));
        assertEquals(original.size(), copy.size());

        Set<BSTNode<Integer>> originalNodes = Collections.newSetFromMap(new IdentityHashMap<>());
        collectNodes(original.root(), originalNodes);
        Set<BSTNode<Integer>> copyNodes = Collections.newSetFromMap(new IdentityHashMap<>());
        collectNodes(copy.root(), copyNodes);
        assertEquals(6, copyNodes.size());
        for (BSTNode<Integer> node : copyNodes) {
            assertFalse(originalNodes.contains(node), "a node is shared between the tree and its copy");
        }
    }

    @Test
    void changingTheOriginalNeverChangesItsCopyAndViceVersa() {
        BinarySearchTree<Integer> original = treeOf(50, 30, 70, 20, 40);
        BinarySearchTree<Integer> copy = original.copy();
        String copyBefore = shape(copy.root());

        original.insert(99);
        original.deleteByCopying(30);
        assertEquals(copyBefore, shape(copy.root()));
        assertEquals(5, copy.size());

        String originalBefore = shape(original.root());
        int originalSize = original.size();
        copy.deleteByMerging(50);
        copy.insert(1);
        assertEquals(originalBefore, shape(original.root()));
        assertEquals(originalSize, original.size());
    }

    @Test
    void copyOfAnEmptyTreeIsEmptyAndUsable() {
        BinarySearchTree<Integer> copy = new BinarySearchTree<Integer>().copy();
        assertEquals(0, copy.size());
        assertNull(copy.root());
        assertTrue(copy.insert(1));
    }

    @Test
    void copyOfADegenerateTreeKeepsEveryValue() {
        BinarySearchTree<Integer> original = new BinarySearchTree<>();
        for (int v = 1; v <= 2000; v++) {
            original.insert(v);
        }
        BinarySearchTree<Integer> copy = original.copy();
        assertEquals(2000, copy.size());
        assertEquals(original.breadthFirst(), copy.breadthFirst());
        assertInvariant(copy);
    }

    @Test
    void restoreFromReplacesTheContentsAndKeepsTheTreesIndependent() {
        BinarySearchTree<Integer> target = treeOf(5, 3, 7);
        BinarySearchTree<Integer> source = treeOf(20, 10, 30, 25);
        target.restoreFrom(source);
        assertEquals(shape(source.root()), shape(target.root()));
        assertEquals(4, target.size());
        assertFalse(target.contains(5));

        target.insert(99);
        assertFalse(source.contains(99));
        source.insert(1);
        assertFalse(target.contains(1));
        assertInvariant(target);
        assertInvariant(source);
    }

    @Test
    void restoreFromAnEmptyTreeEmptiesTheTree() {
        BinarySearchTree<Integer> tree = treeOf(5, 3, 7);
        tree.restoreFrom(new BinarySearchTree<>());
        assertEquals(0, tree.size());
        assertNull(tree.root());
        assertTrue(tree.insert(5));
    }

    @Test
    void restoreFromItselfLeavesTheTreeUnchanged() {
        BinarySearchTree<Integer> tree = treeOf(5, 3, 7);
        String before = shape(tree.root());
        tree.restoreFrom(tree);
        assertEquals(before, shape(tree.root()));
        assertEquals(3, tree.size());
    }

    @Test
    void restoreFromNullThrows() {
        BinarySearchTree<Integer> tree = treeOf(5);
        assertThrows(NullPointerException.class, () -> tree.restoreFrom(null));
        assertEquals(1, tree.size());
    }

    @Test
    void restoringASnapshotUndoesDeletions() {
        BinarySearchTree<Integer> tree = treeOf(50, 30, 70, 20, 40, 60, 80);
        String before = shape(tree.root());
        BinarySearchTree<Integer> snapshot = tree.copy();
        tree.deleteByCopying(50);
        tree.deleteByMerging(70);
        tree.restoreFrom(snapshot);
        assertEquals(before, shape(tree.root()));
        assertEquals(7, tree.size());
        assertInvariant(tree);
    }
}