package lk.ac.ruhuna.fot.ams.unit;

import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashesAndVerifiesWithoutStoringPlaintext() {
        String hash = hasher.hash("ChangeMe-2026!".toCharArray());

        assertThat(hash).startsWith("PBKDF2$");
        assertThat(hasher.verify("ChangeMe-2026!".toCharArray(), hash)).isTrue();
        assertThat(hasher.verify("wrong-password".toCharArray(), hash)).isFalse();
    }

    @Test
    void createsDifferentSaltedHashes() {
        assertThat(hasher.hash("ChangeMe-2026!".toCharArray()))
                .isNotEqualTo(hasher.hash("ChangeMe-2026!".toCharArray()));
    }
}
