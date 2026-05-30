package com.mlops.resource;

import com.mlops.exception.WorkspaceNotEmptyException;
import com.mlops.model.MLWorkspace;
import com.mlops.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/workspaces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkspaceResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Response getAllWorkspaces() {
        return Response.ok(new ArrayList<>(store.getWorkspaces().values())).build();
    }

    @POST
    public Response createWorkspace(MLWorkspace workspace) {
        if (workspace.getId() == null || workspace.getId().isBlank()) {
            workspace.setId("WS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (workspace.getModelIds() == null) {
            workspace.setModelIds(new ArrayList<>());
        }
        store.getWorkspaces().put(workspace.getId(), workspace);
        return Response.status(Response.Status.CREATED).entity(workspace).build();
    }

    @GET
    @Path("/{workspaceId}")
    public Response getWorkspace(@PathParam("workspaceId") String workspaceId) {
        MLWorkspace workspace = store.getWorkspaces().get(workspaceId);
        if (workspace == null) {
            return error(404, "Not Found", "Workspace '" + workspaceId + "' not found.");
        }
        return Response.ok(workspace).build();
    }

    @DELETE
    @Path("/{workspaceId}")
    public Response deleteWorkspace(@PathParam("workspaceId") String workspaceId) {
        MLWorkspace workspace = store.getWorkspaces().get(workspaceId);
        if (workspace == null) {
            return error(404, "Not Found", "Workspace '" + workspaceId + "' not found.");
        }
        if (!workspace.getModelIds().isEmpty()) {
            throw new WorkspaceNotEmptyException(workspaceId, workspace.getModelIds().size());
        }
        store.getWorkspaces().remove(workspaceId);
        return Response.noContent().build();
    }

    @HEAD
    @Path("/{workspaceId}")
    public Response headWorkspace(@PathParam("workspaceId") String workspaceId) {
        return store.getWorkspaces().containsKey(workspaceId)
                ? Response.ok().build()
                : Response.status(Response.Status.NOT_FOUND).build();
    }

    private Response error(int status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", System.currentTimeMillis());
        return Response.status(status).entity(body).build();
    }
}
