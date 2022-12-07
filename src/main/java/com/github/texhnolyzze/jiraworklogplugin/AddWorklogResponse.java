package com.github.texhnolyzze.jiraworklogplugin;

class AddWorklogResponse {

    private final String error;

    AddWorklogResponse(final String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    static AddWorklogResponse success() {
        return new AddWorklogResponse(null);
    }

    static AddWorklogResponse error(final String error) {
        return new AddWorklogResponse(error);
    }

    @Override
    public String toString() {
        return "AddWorklogResponse{" +
            "error='" + error + '\'' +
            '}';
    }

}
