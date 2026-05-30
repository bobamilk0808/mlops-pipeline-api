package com.mlops.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class ModelDeprecatedExceptionMapper implements ExceptionMapper<ModelDeprecatedException> {

    @Override
    public Response toResponse(ModelDeprecatedException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("message", e.getMessage());
        body.put("modelId", e.getModelId());
        body.put("modelStatus", "DEPRECATED");
        body.put("timestamp", System.currentTimeMillis());
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
