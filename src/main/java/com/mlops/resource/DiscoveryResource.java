package com.mlops.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "MLOps Pipeline Management API");
        body.put("version", "1.0.0");
        body.put("status", "operational");
        body.put("contact", "admin@mlops-lab.ai");
        body.put("description", "RESTful API for managing Machine Learning Workspaces and Models in a cloud-native AI platform.");

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("workspaces", "/api/v1/workspaces");
        resources.put("models", "/api/v1/models");
        body.put("resources", resources);

        body.put("timestamp", System.currentTimeMillis());
        return Response.ok(body).build();
    }

    @GET
    @Path("/admin/trigger-error")
    public Response triggerError() {
        String nullRef = null;
        return Response.ok(nullRef.toLowerCase()).build();
    }
}
