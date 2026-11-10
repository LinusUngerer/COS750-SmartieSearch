package cos212.bst;

/**
 * A node in a binary search tree. Holds one value and two links.
 *
 * <p>Scaffold: see the Detailed Design section of the SE document.
 *
 * @param <T> the type of value stored; must be comparable
 */
class BSTNode<T> {

    T value;
    BSTNode<T> left;
    BSTNode<T> right;

    BSTNode(T value) {
        this.value = value;
    }
}
