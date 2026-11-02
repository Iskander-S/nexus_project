import model.User;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserDomainTest {

    @Test
    void shouldIncreaseLevelWhenXpExceedsThreshold() {
        User user = new User("test_id", "Tester");
        int initialLevel = user.getLevel();

        user.addXp(150);

        assertThat(user.getCurrentXp()).isEqualTo(150);
        assertThat(user.getLevel()).isGreaterThan(initialLevel);
        assertThat(user.getRank()).isNotEqualTo("E_RANK"); // Если порог меняет ранг
    }
}