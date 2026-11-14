package cos212.trace;

/**
 * One step of a search or insertion: the node compared, the result, the move taken
 * and a sentence the interface can show the student.
 *
 * @param value the node compared or the node whose link is used; {@code null} only for an empty tree
 * @param comparison the result of comparing the target with {@code value}, or {@code null} for a step with no comparison
 * @param move the move the algorithm makes at this step
 * @param explanation one sentence that names the rule applied
 * @param <T> the type of value stored
 */
public record TraceStep<T>(T value, Comparison comparison, Move move, String explanation) {

    /** The result of comparing the target with a stored value. */
    public enum Comparison {
        LESS,
        EQUAL,
        GREATER
    }

    /**
     * @return the rule that justifies this step, shown under the explanation in the trace panel
     */
    public String rule() {
        return switch (move) {
            case LEFT -> "a smaller value belongs in the left subtree.";
            case RIGHT -> "a larger value belongs in the right subtree.";
            case FOUND -> "an equal value ends the search.";
            case MISSING -> "an empty link means the value is not in the tree.";
            case DUPLICATE -> "a duplicate is never inserted.";
            case ATTACH_LEFT, ATTACH_RIGHT, ATTACH_ROOT -> "a new value is attached as a leaf at an empty link.";
            case DELETE_NODE -> "a node with at most one child is replaced by that child, or by nothing.";
            case SEEK_LEFT, SEEK_RIGHT -> "the largest value in a subtree is its rightmost node.";
            case REPLACEMENT_FOUND -> "a node with no right child holds the largest value in its subtree.";
            case COPY_VALUE -> "delete by copying overwrites the deleted value with the replacement's value.";
            case REMOVE_REPLACEMENT -> "the replacement's old node is removed, and its left child takes its place.";
            case MERGE_ATTACH -> "the right subtree hangs below the rightmost node of the left subtree, so every value stays in order.";
            case MERGE_REPLACE -> "the left subtree takes the place of the deleted node.";
        };
    }

    /** The move taken at a step. */
    public enum Move {
        LEFT,
        RIGHT,
        FOUND,
        MISSING,
        DUPLICATE,
        ATTACH_LEFT,
        ATTACH_RIGHT,
        ATTACH_ROOT,
        DELETE_NODE,
        SEEK_LEFT,
        SEEK_RIGHT,
        REPLACEMENT_FOUND,
        COPY_VALUE,
        REMOVE_REPLACEMENT,
        MERGE_ATTACH,
        MERGE_REPLACE
    }
}
