package lk.ac.ruhuna.fot.ams.unit;

import lk.ac.ruhuna.fot.ams.domain.model.Assessment;
import lk.ac.ruhuna.fot.ams.domain.model.Course;
import lk.ac.ruhuna.fot.ams.domain.model.Mark;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainValidationTest {
    @Test
    void acceptsBoundaryMarks() {
        assertThat(new Mark(0).score()).isZero();
        assertThat(new Mark(100).score()).isEqualTo(100);
    }

    @Test
    void rejectsMarksOutsideRange() {
        assertThatThrownBy(() -> new Mark(-0.01)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Mark(100.01)).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNonPositiveCredits() {
        assertThatThrownBy(() -> new Course("ICT2132", "OOP", 0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNonPositiveAssessmentWeight() {
        assertThatThrownBy(() -> new Assessment("Quiz", 0, true))
                .isInstanceOf(ValidationException.class);
    }
}
