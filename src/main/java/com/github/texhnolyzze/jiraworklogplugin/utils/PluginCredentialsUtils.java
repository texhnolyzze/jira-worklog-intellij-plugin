package com.github.texhnolyzze.jiraworklogplugin.utils;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import org.jetbrains.annotations.NotNull;

public final class PluginCredentialsUtils {

    private PluginCredentialsUtils() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public static CredentialAttributes getCredentialAttributes(final String url) {
        return new CredentialAttributes(
            CredentialAttributesKt.generateServiceName("JiraWorklogPlugin", url)
        );
    }

}
