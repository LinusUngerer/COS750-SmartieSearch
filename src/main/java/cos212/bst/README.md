# bst/

**Domain layer.** The binary search tree and its ordering invariant. Nothing here
knows about the trace, the assessment or the interface.

## What belongs here

- `BSTNode<T>` and `BinarySearchTree<T>`.
- Insertion, search, in-order traversal and height, which enforce the ordering rule.
- `rootView()` and `comparedPath()`: read-only access for the trace and interface
  layers. They expose the shape of the tree without any way to change it.
- No I/O, no UI types and no imports from `cos212.trace`, `cos212.assess` or `cos212.ui`.

## Tests

Tests go in `src/test/java/cos212/bst/`. Check the ordering invariant after every
insertion, and compare results with `java.util.TreeSet` in the property tests. See
the Testing and Verification section of the SE document.
