import model.Rank;
import model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserDomainTest {

    @Test
    void shouldNotLevelUpBelowThreshold() {
        User user = new User("test_id", "Tester");

        user.addXp(150); // порог уровня — 200 XP

        assertThat(user.getCurrentXp()).isEqualTo(150);
        assertThat(user.getLevel()).isEqualTo(1);
        assertThat(user.getRank()).isEqualTo(Rank.E_RANK);
    }

    @Test
    void shouldIncreaseLevelWhenXpExceedsThreshold() {
        User user = new User("test_id", "Tester");
        int initialLevel = user.getLevel();

        user.addXp(250);

        assertThat(user.getCurrentXp()).isEqualTo(250);
        assertThat(user.getLevel()).isGreaterThan(initialLevel);
        assertThat(user.getRank()).isEqualTo(Rank.E_RANK); // до D-Rank нужно 400 XP
    }

    @Test
    void shouldPromoteRankAtThreshold() {
        User user = new User("test_id", "Tester");

        user.addXp(400);

        assertThat(user.getRank()).isEqualTo(Rank.D_RANK);
    }

    @Test
    void shouldRejectNegativeXp() {
        User user = new User("test_id", "Tester");

        assertThatThrownBy(() -> user.addXp(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(user.getCurrentXp()).isZero();
    }

    @Test
    void shouldGenerateDistinctIds() {
        assertThat(new User("A").getId()).isNotEqualTo(new User("B").getId());
    }

    @Test
    void shouldReportXpInsideCurrentLevel() {
        User user = new User("test_id", "Tester");

        user.addXp(250);

        assertThat(user.getXpIntoLevel()).isEqualTo(50);
    }

    @Test
    void rankChainEndsAtS() {
        assertThat(Rank.E_RANK.next().get()).isEqualTo(Rank.D_RANK);
        assertThat(Rank.A_RANK.next().get()).isEqualTo(Rank.S_RANK);
        assertThat(Rank.S_RANK.next().isPresent()).isFalse();
    }
}
