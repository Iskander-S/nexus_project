package service;

import exception.InvalidGradeException;
import model.*;

public class EvaluationService {

    public int processTaskEvaluation(Task task, int submittedScore) throws InvalidGradeException {
        if (submittedScore < 0 || submittedScore > 100) {
            throw new InvalidGradeException("Score must be between 0 and 100. Submitted: " + submittedScore);
        }

        if (task.isCompleted()) {
            throw new IllegalStateException("Task already completed: " + task.getId());
        }

        task.complete();

        if (task instanceof Evaluatable evaluatable) {
            PerformanceGrade grade = evaluatable.evaluatePerformance(submittedScore);
            int bonusXp = evaluatable.calculateBonusXp(task.getBaseXp(), grade);
            return task.getBaseXp() + bonusXp;
        }

        return task.getBaseXp();
    }
}