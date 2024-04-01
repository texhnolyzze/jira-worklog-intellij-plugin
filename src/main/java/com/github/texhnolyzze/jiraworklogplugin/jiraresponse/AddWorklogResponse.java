package com.github.texhnolyzze.jiraworklogplugin.jiraresponse;

public class AddWorklogResponse {

    private final String error;

    AddWorklogResponse(final String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public static AddWorklogResponse success() {
        return new AddWorklogResponse(null);
    }

    public static AddWorklogResponse error(final String error) {
        return new AddWorklogResponse(error);
    }

    @Override
    public String toString() {
        return "AddWorklogResponse{" +
            "error='" + error + '\'' +
            '}';
    }

}
