package cos212.assess;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * Records each answer for an objective and works out the objective's mastery status, using
 * the standard in the progress tracking section of the ID document. An objective is
 * practised after {@value #PRACTISED_AFTER} answers, and secure when at least
 * {@value #SECURE_CORRECT} of the last {@value #SECURE_WINDOW} first answers were correct
 * with no hints.
 */
public final class ProgressTracker {

    public static final int PRACTISED_AFTER = 5;
    public static final int SECURE_WINDOW = 5;
    public static final int SECURE_CORRECT = 4;

    /** Where an objective stands. */
    public enum Status {
        UNTRIED,
        STARTED,
        PRACTISED,
        SECURE
    }

    private record Answer(boolean firstCorrect, int hints) {
    }

    private static final class Record {
        int attempts;
        int correctFirst;
        int hints;
        final Deque<Answer> recent = new ArrayDeque<>();
    }

    private final Map<String, Record> records = new LinkedHashMap<>();

    /**
     * Records one answer.
     *
     * @param objective the objective the answer is for
     * @param firstCorrect whether the answer was correct on the first attempt
     * @param hintsUsed the number of hints the student used before answering
     */
    public void record(String objective, boolean firstCorrect, int hintsUsed) {
        Objects.requireNonNull(objective, "objective");
        if (hintsUsed < 0) {
            throw new IllegalArgumentException("hintsUsed must not be negative");
        }
        Record r = records.computeIfAbsent(objective, key -> new Record());
        r.attempts++;
        if (firstCorrect) {
            r.correctFirst++;
        }
        r.hints += hintsUsed;
        r.recent.addLast(new Answer(firstCorrect, hintsUsed));
        while (r.recent.size() > SECURE_WINDOW) {
            r.recent.removeFirst();
        }
    }

    /** @return the mastery status of the objective; untried if it has no answers */
    public Status status(String objective) {
        Record r = records.get(objective);
        if (r == null || r.attempts == 0) {
            return Status.UNTRIED;
        }
        if (r.attempts < PRACTISED_AFTER) {
            return Status.STARTED;
        }
        return isSecure(r) ? Status.SECURE : Status.PRACTISED;
    }

    private static boolean isSecure(Record r) {
        if (r.recent.size() < SECURE_WINDOW) {
            return false;
        }
        int correct = 0;
        int hints = 0;
        for (Answer answer : r.recent) {
            if (answer.firstCorrect()) {
                correct++;
            }
            hints += answer.hints();
        }
        return correct >= SECURE_CORRECT && hints == 0;
    }

    /** @return the number of answers recorded for the objective */
    public int attempts(String objective) {
        return count(objective, r -> r.attempts);
    }

    /** @return the number of answers that were correct on the first attempt */
    public int correctFirst(String objective) {
        return count(objective, r -> r.correctFirst);
    }

    /** @return the number of hints used for the objective */
    public int hints(String objective) {
        return count(objective, r -> r.hints);
    }

    /**
     * @return the progress bar value for the objective, from 0 to 100. Secure objectives
     *         are shown as complete.
     */
    public int progressPercent(String objective) {
        if (status(objective) == Status.SECURE) {
            return 100;
        }
        return Math.min(100, attempts(objective) * 100 / PRACTISED_AFTER);
    }

    private int count(String objective, ToIntFunction<Record> field) {
        Record r = records.get(objective);
        return r == null ? 0 : field.applyAsInt(r);
    }
}
