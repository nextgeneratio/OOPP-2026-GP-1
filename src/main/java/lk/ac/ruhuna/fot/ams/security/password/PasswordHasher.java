package lk.ac.ruhuna.fot.ams.security.password;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(char[] password) {
        validate(password);
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS, KEY_BITS);
        return "PBKDF2$" + ITERATIONS + "$" + encode(salt) + "$" + encode(derived);
    }

    public boolean verify(char[] password, String encodedHash) {
        if (password == null || encodedHash == null) {
            return false;
        }
        try {
            String[] parts = encodedHash.split("\\$", -1);
            if (parts.length != 4 || !"PBKDF2".equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password, salt, iterations, expected.length * 8);
            return constantTimeEquals(actual, expected);
        } catch (RuntimeException failure) {
            return false;
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterations, int keyBits) {
        try {
            PBEKeySpec specification = new PBEKeySpec(password, salt, iterations, keyBits);
            try {
                return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(specification).getEncoded();
            } finally {
                specification.clearPassword();
            }
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("Password hashing is unavailable.", failure);
        }
    }

    private void validate(char[] password) {
        if (password == null || password.length < 8) {
            throw new ValidationException("Password must contain at least 8 characters.");
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private boolean constantTimeEquals(byte[] left, byte[] right) {
        if (left.length != right.length) {
            return false;
        }
        int difference = 0;
        for (int index = 0; index < left.length; index++) {
            difference |= left[index] ^ right[index];
        }
        return difference == 0;
    }
}
