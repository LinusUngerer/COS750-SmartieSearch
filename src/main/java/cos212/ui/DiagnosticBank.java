package cos212.ui;

import static cos212.ui.Shape.leaf;
import static cos212.ui.Shape.node;

import java.util.List;
import java.util.Set;

/**
 * The five diagnostic items. Each item has a tree to look at, a question, four choices and
 * the index of the correct choice. A wrong answer routes the student to {@code lesson}.
 */
final class DiagnosticBank {

    /** One diagnostic item. */
    record Item(Shape tree, String question, List<String> choices, int correct, String lesson,
            String objective, String correctFeedback, String wrongFeedback, Set<RepresentationMode> modes) {
    }

    static final List<Item> ITEMS = List.of(
            new Item(
                    node(50, node(30, leaf(20), leaf(40)), node(70, leaf(60), leaf(80))),
                    "Which node does a search for 35 compare first?",
                    List.of("50", "30", "20", "70"),
                    0,
                    "L2",
                    "trace-search",
                    "Every search starts at the root. 35 is smaller than 50, so the search goes left next.",
                    "Every search starts at the root, 50, before it moves to a subtree.",
                    Set.of(RepresentationMode.VISUAL, RepresentationMode.READ_WRITE)),
            new Item(
                    node(50, node(30, null, null), node(70, leaf(40), null)),
                    "Is this tree a valid binary search tree?",
                    List.of(
                            "Yes: every parent has a smaller left child and a larger right child",
                            "No: 40 is in the right subtree of 50 but is smaller than 50",
                            "No: a BST can only have two levels",
                            "Not enough information"),
                    1,
                    "L1",
                    "ordering-rule",
                    "40 is smaller than 50, so it cannot sit in 50's right subtree. Every node in a"
                            + " subtree has to follow the rule, not only the children.",
                    "Check every node against every ancestor, not only its parent.",
                    Set.of(RepresentationMode.VISUAL, RepresentationMode.READ_WRITE)),
            new Item(
                    node(50, leaf(30), leaf(70)),
                    "Where does 60 go when it is inserted?",
                    List.of("Left child of 30", "Right child of 30", "Left child of 70", "Right child of 70"),
                    2,
                    "L3",
                    "trace-insert",
                    "60 is larger than 50, so go right to 70. 60 is smaller than 70, so it becomes the"
                            + " left child of 70.",
                    "Follow the search path from the root. 60 is larger than 50, so it goes right.",
                    Set.of(RepresentationMode.VISUAL, RepresentationMode.READ_WRITE)),
            new Item(
                    node(50, leaf(30), node(70, leaf(60), leaf(80))),
                    "A search for 60 starts at 50. Which node does it compare next?",
                    List.of("30", "70", "60", "80"),
                    1,
                    "L2",
                    "trace-search",
                    "60 is larger than 50, so the search goes right to 70.",
                    "60 is larger than 50, so the search goes right, not left.",
                    Set.of(RepresentationMode.VISUAL, RepresentationMode.READ_WRITE)),
            new Item(
                    node(1, null, node(2, null, node(3, null, leaf(4)))),
                    "This tree was built by inserting 1, 2, 3, 4 in that order. What shape is it?",
                    List.of(
                            "A balanced tree",
                            "A chain: every node has only a right child",
                            "A complete tree with two levels",
                            "A tree with no root"),
                    1,
                    "L4",
                    "shape-cost",
                    "Each value is larger than the one before, so every insert goes right of the last"
                            + " node. That makes a chain, and search costs as much as a linked list.",
                    "Look at which children each node has. Here every node has only a right child.",
                    Set.of(RepresentationMode.VISUAL, RepresentationMode.READ_WRITE)));

    private DiagnosticBank() {
    }
}
