package cos212.ui;

import static cos212.ui.Shape.leaf;
import static cos212.ui.Shape.node;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The content of the five lessons, L0 to L4. Each lesson follows the structure in the content design
 * section of the ID document: Predict (answer before any explanation), Show (a worked
 * example, step by step), Try (a similar example with a hint available) and Check (formative
 * questions that are recorded in the progress record). The misconceptions M1 to M6 that each
 * lesson corrects are noted beside the pages that address them.
 */
final class LessonBank {

    /** The four parts of every lesson. */
    enum Stage {
        PREDICT("Predict"),
        SHOW("Show"),
        TRY("Try"),
        CHECK("Check");

        final String label;

        Stage(String label) {
            this.label = label;
        }
    }

    /**
     * One page of a lesson: either an explanation (no choices) or a multiple-choice question.
     * Predict questions are not recorded; Try and Check questions are recorded against
     * {@code objective}, with any hint the student used.
     */
    record Page(Stage stage, String title, Shape tree, Integer current, Set<Integer> visited, String text,
            List<String> choices, int correct, String rightFeedback, String wrongFeedback, String hint,
            String objective) {

        boolean isQuestion() {
            return choices != null;
        }
    }

    private static Page show(String title, Shape tree, Integer current, Set<Integer> visited, String text) {
        return new Page(Stage.SHOW, title, tree, current, visited, text, null, -1, null, null, null, null);
    }

    private static Page predict(Shape tree, String question, List<String> choices, int correct,
            String right, String wrong) {
        return new Page(Stage.PREDICT, "Predict before you learn", tree, null, Set.of(), question, choices,
                correct, right, wrong, null, null);
    }

    private static Page tryIt(String objective, Shape tree, Integer current, Set<Integer> visited,
            String question, List<String> choices, int correct, String right, String wrong, String hint) {
        return new Page(Stage.TRY, "Your turn", tree, current, visited, question, choices, correct, right,
                wrong, hint, objective);
    }

    private static Page check(String objective, Shape tree, String question, List<String> choices,
            int correct, String right, String wrong) {
        return new Page(Stage.CHECK, "Check your understanding", tree, null, Set.of(), question, choices,
                correct, right, wrong, null, objective);
    }

    // ---- trees used across the lessons ---------------------------------------------------

    private static final Shape BALANCED_50 = node(50, node(30, leaf(20), leaf(40)), node(70, leaf(60), leaf(80)));
    private static final Shape BALANCED_40 = node(40, node(20, leaf(10), leaf(30)), node(60, leaf(50), leaf(70)));
    private static final Shape BALANCED_4 = node(4, node(2, leaf(1), leaf(3)), node(6, leaf(5), leaf(7)));
    private static final Shape CHAIN_7 = chain(1, 7);

    private static Shape chain(int from, int to) {
        return from > to ? null : node(from, null, chain(from + 1, to));
    }

    // ---- L0: what is a BST? (define-bst) -------------------------------------------------
    // SCAFFOLD: the structure (Predict, Show, Try, Check) and the progress wiring are in place.
    // Every text below marked TODO is a placeholder to be replaced with the real lesson content.
    // The Try and Check pages record against "define-bst", so the Lessons and Progress screens
    // already respond to them.

    private static final List<Page> L0 = List.of(
            predict(BALANCED_50,
                    "TODO (Predict): a question that surfaces the student's current idea of what a BST is.",
                    List.of("TODO: choice A", "TODO: choice B (correct)", "TODO: choice C", "TODO: choice D"),
                    1,
                    "TODO: feedback when the prediction is right.",
                    "TODO: feedback when the prediction is wrong. Not recorded."),
            show("TODO (Show): what a binary tree is", BALANCED_50, 50, Set.of(),
                    "TODO: define a binary tree (each node has at most a left and a right child)."),
            show("TODO (Show): what makes it a search tree", BALANCED_50, 30, Set.of(50),
                    "TODO: state the definition of a BST in words, using a worked example."),
            tryIt("define-bst", BALANCED_40, 40, Set.of(),
                    "TODO (Try): a question like the worked example, with a hint available.",
                    List.of("TODO: choice A", "TODO: choice B (correct)", "TODO: choice C", "TODO: choice D"),
                    1,
                    "TODO: feedback when right.",
                    "TODO: feedback when wrong.",
                    "TODO: hint."),
            check("define-bst", BALANCED_50,
                    "TODO (Check): a question that tests the definition of a BST.",
                    List.of("TODO: choice A", "TODO: choice B", "TODO: choice C (correct)", "TODO: choice D"),
                    2,
                    "TODO: feedback when right.",
                    "TODO: feedback when wrong."));

    // ---- L1: the ordering rule -----------------------------------------------------------

    private static final Shape DEEP_BREAK = node(50, node(30, leaf(20), leaf(60)), node(70, leaf(55), leaf(80)));

    private static final List<Page> L1 = List.of(
            // M1 and M2: every node has two children and every child is on the right side of its
            // parent, but 60 breaks the rule for the root.
            predict(DEEP_BREAK,
                    "Every node here has two children or none, and each child is on the correct side of its"
                            + " parent. Is this a binary search tree?",
                    List.of("Yes: every node has two children or none",
                            "No: 60 is in 50's left subtree but is larger than 50",
                            "Yes: each child is on the correct side of its parent",
                            "No: a BST cannot have seven nodes"),
                    1,
                    "The rule is about whole subtrees. 60 sits in the left subtree of 50, so it must be"
                            + " smaller than 50, and it is not.",
                    "Checking only parent and child is not enough. 60 sits in the left subtree of 50, so it"
                            + " must be smaller than 50. The rest of the lesson shows why."),
            show("What a BST is", BALANCED_50, 50, Set.of(),
                    "A binary search tree is a binary tree in which, for every node, every value in its left"
                            + " subtree is smaller and every value in its right subtree is larger.\n\n"
                            + "Here 50 is the root. Every value on its left (30, 20, 40) is smaller than 50,"
                            + " and every value on its right (70, 60, 80) is larger. The same holds at 30 and"
                            + " at 70."),
            show("Having two children is not enough", DEEP_BREAK, 60, Set.of(50, 30),
                    "Now look at the tree from the Predict question. Every pair of parent and child looks"
                            + " fine: 60 is larger than 30, so it sits on 30's right.\n\n"
                            + "But 60 is also inside 50's left subtree, and everything there must be smaller"
                            + " than 50. The rule breaks two levels down, so you must check each value against"
                            + " every ancestor, not just its parent."),
            show("The fixed tree", BALANCED_50, 40, Set.of(50, 30),
                    "Replace 60 with 40 and the tree is a BST. 40 is larger than 30, so it belongs on 30's"
                            + " right, and it is smaller than 50, so it belongs in 50's left subtree. A value"
                            + " must fit every subtree it is in."),
            tryIt("ordering-rule", BALANCED_40, 40, Set.of(),
                    "Suppose 45 is added to this tree. Inside which subtree of 40 must it sit?",
                    List.of("The left subtree of 40", "The right subtree of 40",
                            "Either subtree: both have room", "Neither: 45 cannot be added"),
                    1,
                    "45 is larger than 40, so it must sit in 40's right subtree, where every value is"
                            + " larger than 40.",
                    "Compare 45 with 40. Values larger than a node always belong in its right subtree.",
                    "The ordering rule: smaller values go left, larger values go right. Compare 45 with 40."),
            tryIt("ordering-rule", node(40, node(20, leaf(10), leaf(45)), node(60, leaf(50), leaf(70))), 45,
                    Set.of(40, 20),
                    "Which node breaks the ordering rule?",
                    List.of("10", "45", "50", "None: this is a valid BST"),
                    1,
                    "45 is in the left subtree of 40, so it must be smaller than 40. It is larger, so the"
                            + " rule is broken, even though 45 is on the correct side of its parent, 20.",
                    "Check each value against every ancestor. 45 is in 40's left subtree, but 45 is larger"
                            + " than 40.",
                    "45 is in 40's left subtree. What must be true of every value there?"),
            check("ordering-rule", BALANCED_50, "Which sentence defines a binary search tree?",
                    List.of("A binary tree where every node has zero or two children",
                            "A binary tree where each node's left child is smaller and its right child is"
                                    + " larger",
                            "A binary tree where, for every node, all values in its left subtree are smaller"
                                    + " and all values in its right subtree are larger",
                            "Any binary tree whose root is the middle value"),
                    2,
                    "The rule covers whole subtrees, not just the children.",
                    "The definition is about every value in each subtree. Checking only the children misses"
                            + " breaks deeper down."),
            check("ordering-rule", node(50, node(30, leaf(20), leaf(40)), node(70, leaf(45), leaf(80))),
                    "Is this tree a binary search tree?",
                    List.of("Yes: every child is on the correct side of its parent",
                            "No: 45 is in 50's right subtree but is smaller than 50",
                            "No: 80 should be on the left of 70",
                            "No: 20 and 40 are in the wrong places"),
                    1,
                    "45 is smaller than 70, so it is fine as 70's left child, but it is also in 50's right"
                            + " subtree, where every value must be larger than 50.",
                    "Look at 45. It is on the correct side of 70, but it is inside 50's right subtree, so"
                            + " it must be larger than 50."),
            check("ordering-rule", node(30, node(15, leaf(10), leaf(20)), node(45, leaf(35), leaf(50))),
                    "Is this tree a binary search tree?",
                    List.of("No: 35 is smaller than 45",
                            "Yes: every value fits every subtree it is in",
                            "No: 20 is larger than 15",
                            "No: 10 should be the root"),
                    1,
                    "Every value in 30's left subtree (15, 10, 20) is smaller than 30, every value on its"
                            + " right (45, 35, 50) is larger, and the same holds at 15 and at 45.",
                    "Check each value against every ancestor. 35 is smaller than 45, so it belongs on 45's"
                            + " left, and it is larger than 30, so it belongs in 30's right subtree."));

    // ---- L2: searching -------------------------------------------------------------------

    private static final List<Page> L2 = List.of(
            predict(BALANCED_50, "A search for 40 starts at the root, 50. Which way does it go first?",
                    List.of("Left, because 40 is smaller than 50",
                            "Right, because 40 is smaller than 50",
                            "Right, because 40 is larger than 30",
                            "Nowhere: it checks every node in turn"),
                    0,
                    "40 is smaller than 50, and every smaller value is in the left subtree.",
                    "The search compares 40 with 50 first. 40 is smaller, so it can only be in the left"
                            + " subtree."),
            // M3: each direction is paired with the comparison that produces it.
            show("Step 1: compare with the root", BALANCED_50, 50, Set.of(),
                    "Search for 40. Every search starts at the root.\n\n"
                            + "40 < 50, so go left. Everything in 50's right subtree is larger than 50, so 40"
                            + " cannot be there. One comparison has ruled out half the tree."),
            show("Step 2: compare again", BALANCED_50, 30, Set.of(50),
                    "Now at 30. 40 > 30, so go right.\n\nThe search went left at the root and right here:"
                            + " the direction comes from the comparison at each node, every time."),
            show("Step 3: found", BALANCED_50, 40, Set.of(50, 30),
                    "Now at 40. 40 = 40, so the search has found it after 3 comparisons: 50, 30 and 40."
                            + " It never looked at 70, 60, 80 or 20."),
            show("When the value is not there", BALANCED_50, 60, Set.of(50, 70),
                    "Search for 65: 65 > 50, go right. 65 < 70, go left. 65 > 60, go right.\n\n"
                            + "60 has no right child, so there is nowhere left to look. The search ends: 65 is"
                            + " not in the tree."),
            tryIt("trace-search", BALANCED_50, 50, Set.of(),
                    "Search for 60. After comparing 60 with 50, which node does the search compare next?",
                    List.of("30", "70", "80", "40"),
                    1,
                    "60 > 50, so the search goes right, to 70.",
                    "60 is larger than 50, so the search goes right. The right child of 50 is 70.",
                    "Larger values are in the right subtree. Compare 60 with 50."),
            tryIt("trace-search", BALANCED_50, 70, Set.of(50),
                    "The search for 60 is now at 70. What happens next?",
                    List.of("Go left, to 60", "Go right, to 80", "Go back up to 50",
                            "Stop: 60 is not in the tree"),
                    0,
                    "60 < 70, so the search goes left and finds 60.",
                    "Compare 60 with 70. 60 is smaller, so the search goes left, to 60. A search never goes"
                            + " back up.",
                    "Compare 60 with 70. Smaller values are on the left."),
            check("trace-search", BALANCED_40, "How many nodes does a search for 30 compare, including 30?",
                    List.of("1", "2", "3", "7"),
                    2,
                    "40, then 20, then 30: three comparisons.",
                    "Trace it: 30 < 40, go left to 20. 30 > 20, go right to 30. Found: 40, 20 and 30 is"
                            + " three comparisons."),
            check("trace-search", BALANCED_40, "What happens when you search for 55?",
                    List.of("It is found at 50",
                            "40, then 60, then 50; 50 has no right child, so 55 is not found",
                            "40, then 20, then 30; 55 is not found",
                            "It compares all seven nodes before it fails"),
                    1,
                    "55 > 40, go right. 55 < 60, go left. 55 > 50, go right, but there is no right child.",
                    "55 > 40 sends the search right, 55 < 60 sends it left to 50, and 55 > 50 would send"
                            + " it right, where there is no node."),
            check("trace-search", BALANCED_40, "A search for 65 is at node 60. Where does it go?",
                    List.of("Left, because 65 is smaller than 60",
                            "Right, because 65 is larger than 60",
                            "Left, because 65 is larger than 60",
                            "Right, because 65 is smaller than 60"),
                    1,
                    "65 > 60, and larger values are always on the right.",
                    "Pair the direction with the comparison: larger goes right, smaller goes left. 65 is"
                            + " larger than 60."));

    // ---- L3: inserting -------------------------------------------------------------------

    private static final Shape WITH_65 = node(50, node(30, leaf(20), leaf(40)),
            node(70, node(60, null, leaf(65)), leaf(80)));

    private static final List<Page> L3 = List.of(
            predict(BALANCED_50, "Where does 65 go when it is inserted into this tree?",
                    List.of("It replaces 60",
                            "It becomes the right child of 60",
                            "It becomes the left child of 80",
                            "It becomes the right child of 70, and 80 moves down"),
                    1,
                    "Insertion follows the search path: right at 50, left at 70, right at 60, where there is"
                            + " an empty spot.",
                    "Insertion never replaces or moves a node. It follows the search path to an empty spot:"
                            + " right at 50, left at 70, right at 60."),
            show("Follow the search path", BALANCED_50, 50, Set.of(),
                    "Insert 65. Insertion starts exactly like a search for 65.\n\n65 > 50, so go right."),
            show("Keep following it", BALANCED_50, 70, Set.of(50),
                    "Now at 70. 65 < 70, so go left."),
            show("Find the empty spot", BALANCED_50, 60, Set.of(50, 70),
                    "Now at 60. 65 > 60, so go right. 60 has no right child: this empty spot is where the"
                            + " search for 65 would end, so it is where 65 goes."),
            // M4: the new value is a new leaf and nothing else moves.
            show("Attach a new leaf", WITH_65, 65, Set.of(50, 70, 60),
                    "65 is attached as the right child of 60. Nothing else has moved: 60 is still in place."
                            + " A new value is always added as a new leaf. It never replaces a node."),
            // M5: a duplicate is rejected and the tree is unchanged.
            show("Inserting a value that is already there", WITH_65, 40, Set.of(50, 30),
                    "Insert 40 again: 40 < 50, go left. 40 > 30, go right. 40 = 40: the value is already"
                            + " in the tree.\n\nA BST stores each value once, so the insert is rejected and the"
                            + " tree is unchanged."),
            tryIt("trace-insert", BALANCED_50, null, Set.of(),
                    "Insert 35 into this tree. Which node becomes the parent of 35?",
                    List.of("30", "40", "20", "50"),
                    1,
                    "35 < 50, go left. 35 > 30, go right. 35 < 40, go left, and 40 has no left child, so"
                            + " 40 becomes the parent.",
                    "Follow the search for 35: left at 50, right at 30, left at 40. The empty spot is under"
                            + " 40.",
                    "Follow the search for 35 until you reach an empty spot."),
            tryIt("trace-insert", BALANCED_50, 40, Set.of(50, 30),
                    "The insert of 35 has reached 40. Where is 35 attached?",
                    List.of("As the left child of 40", "As the right child of 40", "In place of 40",
                            "Between 30 and 40"),
                    0,
                    "35 < 40, so it becomes 40's left child, a new leaf.",
                    "Compare 35 with 40. Smaller goes left, and nothing is ever replaced or squeezed in.",
                    "Compare 35 with 40."),
            check("trace-insert", BALANCED_40, "Where does 55 go when it is inserted?",
                    List.of("It becomes the left child of 60, and 50 moves down",
                            "It becomes the right child of 50",
                            "It becomes the left child of 70",
                            "It replaces 50"),
                    1,
                    "55 > 40, right. 55 < 60, left. 55 > 50, right, which is empty.",
                    "Follow the search path: right at 40, left at 60, right at 50. The new value goes in the"
                            + " empty spot, and nothing moves."),
            check("trace-insert", BALANCED_40, "What happens when you insert 30 into this tree?",
                    List.of("A second 30 is added as a new leaf",
                            "The new 30 replaces the old one",
                            "The insert is rejected and the tree is unchanged",
                            "30 moves up to become the root"),
                    2,
                    "30 is already in the tree, so the insert is rejected.",
                    "The search for 30 finds it already in the tree. A BST stores each value once, so the"
                            + " insert is rejected and nothing changes."),
            check("trace-insert", BALANCED_40, "Which nodes are compared when 5 is inserted?",
                    List.of("40, 20 and 10", "10 only", "40, 60 and 70", "Every node"),
                    0,
                    "5 < 40, left. 5 < 20, left. 5 < 10, left, which is empty: 5 becomes 10's left child.",
                    "Insertion follows the search path from the root: 40, then 20, then 10."));

    // ---- L4: shape and height ------------------------------------------------------------

    private static final List<Page> L4 = List.of(
            predict(BALANCED_4,
                    "The values 1 to 7 are inserted in the order 1, 2, 3, 4, 5, 6, 7 into an empty tree."
                            + " What shape does the tree take?",
                    List.of("The same shape as this tree",
                            "A single chain going right, seven levels deep",
                            "A single chain going left, seven levels deep",
                            "A full tree with 1 at the root"),
                    1,
                    "Each new value is larger than everything before it, so it always goes right.",
                    "Each new value is larger than every value already in the tree, so it always goes right"
                            + " and the tree becomes a chain."),
            show("Sorted order makes a chain", CHAIN_7, 7, Set.of(),
                    "Inserted as 1, 2, 3, 4, 5, 6, 7, each value is larger than everything already in the"
                            + " tree, so it always goes right. The tree is a chain seven levels deep: in"
                            + " effect, a linked list."),
            show("Another order makes a balanced tree", BALANCED_4, 4, Set.of(),
                    "Insert the same values as 4, 2, 6, 1, 3, 5, 7 and the tree is balanced, only three"
                            + " levels deep. The values are the same. Only the order of insertion changed."),
            // M6: the same search costs different amounts in the two shapes.
            show("Search cost in the chain", CHAIN_7, 7, Set.of(1, 2, 3, 4, 5, 6),
                    "Search for 7 in the chain: 1, 2, 3, 4, 5, 6, 7. That is seven comparisons, one for every"
                            + " node, exactly like walking a linked list."),
            show("Search cost in the balanced tree", BALANCED_4, 7, Set.of(4, 6),
                    "Search for 7 in the balanced tree: 4, 6, 7. That is three comparisons.\n\nThe worst"
                            + " case for a search is the height of the tree, and the order in which values"
                            + " arrive decides the height."),
            tryIt("shape-cost", BALANCED_4, null, Set.of(),
                    "How many comparisons does a search for 5 need in this tree?",
                    List.of("1", "2", "3", "5"),
                    2,
                    "4, then 6, then 5: three comparisons.",
                    "Trace it: 5 > 4, go right to 6. 5 < 6, go left to 5. That is 4, 6 and 5: three"
                            + " comparisons.",
                    "Count every node the search compares, including 5 itself."),
            tryIt("shape-cost", CHAIN_7, null, Set.of(),
                    "How many comparisons does a search for 5 need in this chain?",
                    List.of("3", "4", "5", "7"),
                    2,
                    "1, 2, 3, 4, 5: five comparisons, one for each node on the way.",
                    "In a chain the search visits every node in order: 1, 2, 3, 4 and 5 is five"
                            + " comparisons.",
                    "In a chain there is only one way to go. Count the nodes from the root to 5."),
            check("shape-cost", CHAIN_7, "Which insertion order gives the shortest tree for the values 1 to 7?",
                    List.of("1, 2, 3, 4, 5, 6, 7", "7, 6, 5, 4, 3, 2, 1", "4, 2, 6, 1, 3, 5, 7",
                            "1, 7, 2, 6, 3, 5, 4"),
                    2,
                    "Starting with the middle value splits the rest evenly, so the tree is three levels deep.",
                    "Sorted orders (either way) make a chain. Starting with the middle value, 4, and then the"
                            + " middle of each half gives a balanced tree three levels deep."),
            check("shape-cost", CHAIN_7,
                    "A BST holds n values in a single chain. In the worst case, how many comparisons does a"
                            + " search need?",
                    List.of("1", "About log₂ n", "n", "2n"),
                    2,
                    "A chain is n levels deep, so the worst search compares every one of the n nodes.",
                    "A chain has one node per level, so a search for the deepest value compares all n nodes."
                            + " About log₂ n is the cost in a balanced tree."),
            check("shape-cost", BALANCED_4, "Two BSTs hold the same seven values. Which statement is true?",
                    List.of("Searches cost the same, because the values are the same",
                            "The taller tree can need more comparisons for a search",
                            "The shorter tree always needs more comparisons",
                            "Shape affects insertion but not search"),
                    1,
                    "Search cost depends on the height, so the taller tree can need more comparisons.",
                    "The values do not decide the cost: the shape does. A search can go as deep as the"
                            + " height, so a taller tree can need more comparisons."));

    static final Map<String, List<Page>> PAGES = Map.of("L0", L0, "L1", L1, "L2", L2, "L3", L3, "L4", L4);

    private LessonBank() {
    }
}
