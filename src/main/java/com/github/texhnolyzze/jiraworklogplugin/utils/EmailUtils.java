package com.github.texhnolyzze.jiraworklogplugin.utils;

public final class EmailUtils {

    private EmailUtils() {
        throw new UnsupportedOperationException();
    }

    public static boolean sameUser(final String email1, final String email2) {
        return email1 != null &&
                email2 != null &&
                (email1.equals(email2) || getUsername(email1).equals(getUsername(email2)));
    }

    public static String getUsername(final String email) {
        final int i = email.indexOf('@');
        if (i < 0) {
            return email;
        }
        return email.substring(0, i);
    }

}
