package com.mlops.resource;

import com.mlops.exception.LinkedWorkspaceNotFoundException;
import com.mlops.model.MachineLearningModel;
import com.mlops.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

@Path("/models")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ModelResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Response getModels(@QueryParam("status") String status) {
        List<MachineLearningModel> result = new ArrayList<>(store.getModels().values());
        if (status != null && !status.isBlank()) {
            result = result.stream()
                    .filter(m -> status.equalsIgnoreCase(m.getStatus()))
                    .collect(Collectors.toList());
        }
        return Response.ok(result).build();
    }

    @POST
    public Response createModel(MachineLearningModel model) {
        String wsId = model.getWorkspaceId();
        if (wsId == null || !store.getWorkspaces().containsKey(wsId)) {
            throw new LinkedWorkspaceNotFoundException(wsId);
        }
        model.setId("MOD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        store.getModels().put(model.getId(), model);
        store.getWorkspaces().get(wsId).getModelIds().add(model.getId());
        store.getMetrics().put(model.getId(), Collections.synchronizedList(new ArrayList<>()));
        return Response.status(Response.Status.CREATED).entity(model).build();
    }

    @GET
    @Path("/{modelId}")
    public Response getModel(@PathParam("modelId") String modelId) {
        MachineLearningModel model = store.getModels().get(modelId);
        if (model == null) {
            return error(404, "Not Found", "Model '" + modelId + "' not found.");
        }
        return Response.ok(model).build();
    }

    @Path("/{modelId}/metrics")
    public EvaluationMetricResource getMetricsSubResource(@PathParam("modelId") String modelId) {
        return new EvaluationMetricResource(modelId);
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
