package com.github.texhnolyzze.jiraworklogplugin.jiraresponse;

import org.jetbrains.annotations.Nullable;

public abstract class JiraResponse {

    @Nullable
    private final String error;

    protected JiraResponse(final @Nullable String error) {
        this.error = error;
    }

    public @Nullable String getError() {
        return error;
    }

    @Override
    public String toString() {
        return "JiraResponse{" +
                "error='" + error + '\'' +
                '}';
    }

}
