package com.mlops.exception;

public class LinkedWorkspaceNotFoundException extends RuntimeException {

    private final String workspaceId;

    public LinkedWorkspaceNotFoundException(String workspaceId) {
        super("Cannot register model: workspace '" + workspaceId
                + "' does not exist in the system. Provide a valid workspaceId.");
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceId() { return workspaceId; }
}
