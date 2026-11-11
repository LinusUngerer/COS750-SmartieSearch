package cos212.assess;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The diagnostic gate. It has {@value #ITEM_COUNT} items, and a student passes with at
 * least {@value #PASS_MARK} correct. Each wrong answer routes the student to the lesson
 * for its misconception, and the gate records those lessons so the result can name them.
 */
public final class Diagnostic {

    public static final int ITEM_COUNT = 5;
    public static final int PASS_MARK = 4;

    private int answered;
    private int correct;
    private final Set<String> missedLessons = new LinkedHashSet<>();

    /**
     * Records the answer to one item.
     *
     * @param isCorrect whether the answer was correct
     * @param lessonIfWrong the lesson to route the student to when the answer is wrong
     * @throws IllegalStateException if every item has already been answered
     */
    public void recordAnswer(boolean isCorrect, String lessonIfWrong) {
        Objects.requireNonNull(lessonIfWrong, "lessonIfWrong");
        if (answered >= ITEM_COUNT) {
            throw new IllegalStateException("All " + ITEM_COUNT + " diagnostic items have been answered.");
        }
        answered++;
        if (isCorrect) {
            correct++;
        } else {
            missedLessons.add(lessonIfWrong);
        }
    }

    /** Clears the answers, so the diagnostic can be taken again. */
    public void reset() {
        answered = 0;
        correct = 0;
        missedLessons.clear();
    }

    public int answered() {
        return answered;
    }

    public int correct() {
        return correct;
    }

    public boolean isComplete() {
        return answered == ITEM_COUNT;
    }

    /** @return whether the gate is complete and the student reached the pass mark */
    public boolean passed() {
        return isComplete() && correct >= PASS_MARK;
    }

    /** @return the lessons for the wrong answers, in the order they were missed */
    public Set<String> missedLessons() {
        return Collections.unmodifiableSet(missedLessons);
    }
}
