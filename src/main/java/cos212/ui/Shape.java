package cos212.ui;

import cos212.bst.BinarySearchTree.NodeView;

/**
 * A binary tree drawn for a diagnostic question. Unlike {@code BinarySearchTree}, a shape
 * may break the ordering rule, because the questions ask the student to spot the break.
 */
record Shape(Integer value, Shape left, Shape right) implements NodeView<Integer> {

    static Shape leaf(int value) {
        return new Shape(value, null, null);
    }

    static Shape node(int value, Shape left, Shape right) {
        return new Shape(value, left, right);
    }
}
