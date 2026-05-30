package com.mlops.exception;

public class WorkspaceNotEmptyException extends RuntimeException {

    private final String workspaceId;
    private final int modelCount;

    public WorkspaceNotEmptyException(String workspaceId, int modelCount) {
        super("Workspace '" + workspaceId + "' cannot be deleted: it still contains "
                + modelCount + " model(s). Remove all models from this workspace first.");
        this.workspaceId = workspaceId;
        this.modelCount = modelCount;
    }

    public String getWorkspaceId() { return workspaceId; }
    public int getModelCount() { return modelCount; }
}
