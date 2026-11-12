package cos212.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LessonBankTest {

    @Test
    void everyLessonHasPages() {
        for (Objectives.Lesson lesson : Objectives.LESSONS) {
            List<LessonBank.Page> pages = LessonBank.PAGES.get(lesson.id());
            assertNotNull(pages, lesson.id());
            assertTrue(pages.size() > 0, lesson.id());
        }
    }

    @Test
    void stagesRunInOrderPredictShowTryCheck() {
        for (Objectives.Lesson lesson : Objectives.LESSONS) {
            int last = -1;
            for (LessonBank.Page page : LessonBank.PAGES.get(lesson.id())) {
                assertTrue(page.stage().ordinal() >= last, lesson.id() + " goes back a stage");
                last = page.stage().ordinal();
            }
            assertEquals(LessonBank.Stage.CHECK.ordinal(), last, lesson.id() + " ends without a Check");
        }
    }

    @Test
    void questionsAreWellFormedAndRecordTheLessonsObjectives() {
        for (Objectives.Lesson lesson : Objectives.LESSONS) {
            for (LessonBank.Page page : LessonBank.PAGES.get(lesson.id())) {
                assertNotNull(page.tree(), lesson.id() + ": " + page.title());
                if (!page.isQuestion()) {
                    continue;
                }
                assertTrue(page.correct() >= 0 && page.correct() < page.choices().size(),
                        lesson.id() + ": correct index out of range in " + page.text());
                assertNotNull(page.rightFeedback());
                assertNotNull(page.wrongFeedback());
                if (page.stage() == LessonBank.Stage.PREDICT) {
                    assertNull(page.objective(), "Predict answers are not recorded");
                } else {
                    assertTrue(lesson.objectives().contains(page.objective()),
                            lesson.id() + " records " + page.objective() + ", which it does not teach");
                }
                if (page.stage() == LessonBank.Stage.TRY) {
                    assertNotNull(page.hint(), "every Try question has a hint");
                }
            }
        }
    }

    @Test
    void everyObjectiveIsCheckedInSomeLesson() {
        for (Objectives.Objective objective : Objectives.ALL) {
            boolean checked = LessonBank.PAGES.values().stream().flatMap(List::stream)
                    .anyMatch(p -> p.stage() == LessonBank.Stage.CHECK && objective.id().equals(p.objective()));
            assertTrue(checked, objective.id() + " has no Check question");
        }
    }
}
