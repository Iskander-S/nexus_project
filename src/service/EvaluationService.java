package service;

import exception.InvalidGradeException;
import model.PerformanceGrade;
import model.Task;

public class EvaluationService {

    public EvaluationResult evaluate(Task task, int submittedScore) throws InvalidGradeException {
        if (submittedScore < 0 || submittedScore > 100) {
            throw new InvalidGradeException("Score must be between 0 and 100. Submitted: " + submittedScore);
        }

        if (task.isCompleted()) {
            throw new IllegalStateException("Task already completed: " + task.getId());
        }

        PerformanceGrade grade = task.evaluatePerformance(submittedScore);
        int bonusXp = task.calculateBonusXp(task.getBaseXp(), grade);

        task.complete();
        task.setGrade(grade);
        return new EvaluationResult(grade, task.getBaseXp(), bonusXp);
    }

    public int processTaskEvaluation(Task task, int submittedScore) throws InvalidGradeException {
        return evaluate(task, submittedScore).totalXp();
    }
}
