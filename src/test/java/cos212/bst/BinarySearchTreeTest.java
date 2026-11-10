package cos212.bst;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class BinarySearchTreeTest {

    @Test
    void emptyTreeHasSizeZero() {
        assertEquals(0, new BinarySearchTree<Integer>().size());
    }

    @Disabled("Scaffold: enable when increment I1 implements insert and contains")
    @Test
    void insertRejectsDuplicates() {
        BinarySearchTree<Integer> tree = new BinarySearchTree<>();
        org.junit.jupiter.api.Assertions.assertTrue(tree.insert(5));
        org.junit.jupiter.api.Assertions.assertFalse(tree.insert(5));
        assertEquals(1, tree.size());
    }
}
