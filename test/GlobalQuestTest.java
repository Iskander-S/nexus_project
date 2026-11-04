import exception.InvalidGradeException;
import model.GlobalQuest;
import model.PerformanceGrade;
import org.junit.jupiter.api.Test;
import service.EvaluationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobalQuestTest {

    @Test
    void targetReachedGivesS() {
        GlobalQuest quest = new GlobalQuest("g", "t", "d", 300, 70);

        assertThat(quest.evaluatePerformance(70)).isEqualTo(PerformanceGrade.S);
    }

    @Test
    void highTargetIsNotBypassedBy90() {
        GlobalQuest quest = new GlobalQuest("g", "t", "d", 300, 100);

        assertThat(quest.evaluatePerformance(90)).isEqualTo(PerformanceGrade.A);
        assertThat(quest.evaluatePerformance(100)).isEqualTo(PerformanceGrade.S);
    }

    @Test
    void lowScoresMapToLowGrades() {
        GlobalQuest quest = new GlobalQuest("g", "t", "d", 300, 100);

        assertThat(quest.evaluatePerformance(80)).isEqualTo(PerformanceGrade.B);
        assertThat(quest.evaluatePerformance(70)).isEqualTo(PerformanceGrade.C);
        assertThat(quest.evaluatePerformance(60)).isEqualTo(PerformanceGrade.D);
        assertThat(quest.evaluatePerformance(59)).isEqualTo(PerformanceGrade.E);
    }

    @Test
    void rejectsTargetOutsideRange() {
        assertThatThrownBy(() -> new GlobalQuest("g", "t", "d", 300, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GlobalQuest("g", "t", "d", 300, 101)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bonusXpIsAddedToBase() {
        GlobalQuest quest = new GlobalQuest("g", "t", "d", 300, 90);

        assertThat(new EvaluationService().processTaskEvaluation(quest, 95)).isEqualTo(600);
    }

    @Test
    void evaluationRejectsScoreOutOfRange() {
        GlobalQuest quest = new GlobalQuest("g", "t", "d", 300, 90);

        assertThatThrownBy(() -> new EvaluationService().processTaskEvaluation(quest, 101))
                .isInstanceOf(InvalidGradeException.class);
        assertThat(quest.isCompleted()).isFalse();
    }

    @Test
    void evaluationCannotBeAppliedTwice() {
        GlobalQuest quest = new GlobalQuest("g", "t", "d", 300, 90);
        EvaluationService service = new EvaluationService();
        service.processTaskEvaluation(quest, 50);

        assertThatThrownBy(() -> service.processTaskEvaluation(quest, 50)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void evaluationStoresGradeOnTask() {
        GlobalQuest quest = new GlobalQuest("g", "t", "d", 300, 90);

        new EvaluationService().processTaskEvaluation(quest, 95);

        assertThat(quest.getGrade()).isEqualTo(PerformanceGrade.S);
    }

    @Test
    void dailyAndWeeklyQuestsUseTheSameGradeScale() {
        model.DailyTask daily = new model.DailyTask("d", "t", "d", 100);
        model.WeeklyTask weekly = new model.WeeklyTask("w", "t", "d", 100);

        assertThat(daily.evaluatePerformance(90)).isEqualTo(PerformanceGrade.S);
        assertThat(daily.evaluatePerformance(86)).isEqualTo(PerformanceGrade.A);
        assertThat(weekly.evaluatePerformance(80)).isEqualTo(PerformanceGrade.B);
        assertThat(weekly.evaluatePerformance(10)).isEqualTo(PerformanceGrade.E);
    }

    @Test
    void evaluationServiceAwardsBonusForDailyQuest() {
        model.DailyTask daily = new model.DailyTask("d", "t", "d", 100);

        assertThat(new EvaluationService().processTaskEvaluation(daily, 100)).isEqualTo(200);
        assertThat(daily.isCompleted()).isTrue();
    }
}
