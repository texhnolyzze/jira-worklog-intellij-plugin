package com.github.texhnolyzze.jiraworklogplugin.utils;

import java.util.regex.Pattern;

public final class EmailUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(".+@.+");

    private EmailUtils() {
        throw new UnsupportedOperationException();
    }

    public static boolean isEmail(final String str) {
        return EMAIL_PATTERN.matcher(str).matches();
    }

    public static String getUsername(final String email) {
        final int i = email.indexOf('@');
        if (i < 0) {
            return email;
        }
        return email.substring(0, i);
    }

}
