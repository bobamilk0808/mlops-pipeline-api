package com.mlops.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class WorkspaceNotEmptyExceptionMapper implements ExceptionMapper<WorkspaceNotEmptyException> {

    @Override
    public Response toResponse(WorkspaceNotEmptyException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 409);
        body.put("error", "Conflict");
        body.put("message", e.getMessage());
        body.put("workspaceId", e.getWorkspaceId());
        body.put("modelCount", e.getModelCount());
        body.put("hint", "Delete or reassign all models in this workspace before removing it.");
        body.put("timestamp", System.currentTimeMillis());
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
