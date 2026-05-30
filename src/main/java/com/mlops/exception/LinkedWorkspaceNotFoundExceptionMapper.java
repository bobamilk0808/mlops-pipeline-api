package com.mlops.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class LinkedWorkspaceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedWorkspaceNotFoundException> {

    @Override
    public Response toResponse(LinkedWorkspaceNotFoundException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 422);
        body.put("error", "Unprocessable Entity");
        body.put("message", e.getMessage());
        body.put("invalidField", "workspaceId");
        body.put("rejectedValue", e.getWorkspaceId());
        body.put("timestamp", System.currentTimeMillis());
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
