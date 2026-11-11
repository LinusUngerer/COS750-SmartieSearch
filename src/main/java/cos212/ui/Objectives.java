package cos212.ui;

import java.util.List;

/** The learning objectives and lessons, as named in the ID document. */
final class Objectives {

    /** A learning objective, shown on the progress screen. */
    record Objective(String id, String title, String description) {
    }

    /** A lesson, with the objectives it teaches. */
    record Lesson(String id, String title, String description, List<String> objectives) {
    }

    static final List<Objective> ALL = List.of(
            new Objective("define-bst", "Define a BST", "Define a BST and state its ordering rule"),
            new Objective("ordering-rule", "Ordering rule",
                    "Explain and check the ordering rule on a whole subtree"),
            new Objective("trace-search", "Trace search", "Trace a search by hand"),
            new Objective("trace-insert", "Trace insert", "Trace an insertion by hand"),
            new Objective("shape-cost", "Shape and cost", "Predict how tree shape changes cost"));

    static final List<Lesson> LESSONS = List.of(
            new Lesson("L1", "The ordering rule",
                    "What a BST is and why left is smaller, right is larger.",
                    List.of("define-bst", "ordering-rule")),
            new Lesson("L2", "Searching", "Use the rule to choose a subtree at each node.",
                    List.of("trace-search")),
            new Lesson("L3", "Inserting", "Follow the search path and attach a new leaf.",
                    List.of("trace-insert")),
            new Lesson("L4", "Shape and height", "Why insertion order changes the cost of search.",
                    List.of("shape-cost")));

    private Objectives() {
    }

    static Objective find(String id) {
        return ALL.stream().filter(o -> o.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown objective " + id));
    }
}
