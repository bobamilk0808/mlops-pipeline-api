package com.mlops.exception;

public class ModelDeprecatedException extends RuntimeException {

    private final String modelId;

    public ModelDeprecatedException(String modelId) {
        super("Model '" + modelId + "' is DEPRECATED and no longer accepts new evaluation metrics.");
        this.modelId = modelId;
    }

    public String getModelId() { return modelId; }
}
