package com.github.texhnolyzze.jiraworklogplugin.jiraresponse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AddWorklogResponse extends JiraResponse {

    private AddWorklogResponse(final @Nullable String error) {
        super(error);
    }

    public static AddWorklogResponse success() {
        return new AddWorklogResponse(null);
    }

    public static AddWorklogResponse error(@NotNull final String error) {
        return new AddWorklogResponse(error);
    }

    @Override
    public String toString() {
        return "AddWorklogResponse{} " + super.toString();
    }

}
