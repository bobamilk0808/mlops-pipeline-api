package com.mlops.resource;

import com.mlops.exception.ModelDeprecatedException;
import com.mlops.model.EvaluationMetric;
import com.mlops.model.MachineLearningModel;
import com.mlops.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EvaluationMetricResource {

    private final String modelId;
    private final DataStore store = DataStore.getInstance();

    public EvaluationMetricResource(String modelId) {
        this.modelId = modelId;
    }

    @GET
    public Response getMetrics() {
        MachineLearningModel model = store.getModels().get(modelId);
        if (model == null) {
            return error(404, "Not Found", "Model '" + modelId + "' not found.");
        }
        List<EvaluationMetric> history = store.getMetrics().getOrDefault(modelId, new ArrayList<>());
        return Response.ok(history).build();
    }

    @POST
    public Response addMetric(EvaluationMetric metric) {
        MachineLearningModel model = store.getModels().get(modelId);
        if (model == null) {
            return error(404, "Not Found", "Model '" + modelId + "' not found.");
        }
        if ("DEPRECATED".equalsIgnoreCase(model.getStatus())) {
            throw new ModelDeprecatedException(modelId);
        }
        metric.setId(UUID.randomUUID().toString());
        if (metric.getTimestamp() == 0) {
            metric.setTimestamp(System.currentTimeMillis());
        }
        store.getMetrics().computeIfAbsent(modelId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(metric);
        model.setLatestAccuracy(metric.getAccuracyScore());
        return Response.status(Response.Status.CREATED).entity(metric).build();
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
