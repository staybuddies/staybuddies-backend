package com.example.SP.senior_project.util;

import java.security.SecureRandom;

public final class OtpUtil {
    private static final SecureRandom RND = new SecureRandom();

    private OtpUtil() {
    }

    public static String sixDigits() {
        return "%06d".formatted(RND.nextInt(1_000_000));
    }
}
