package com.mlops.store;

import com.mlops.model.EvaluationMetric;
import com.mlops.model.MachineLearningModel;
import com.mlops.model.MLWorkspace;

import java.util.*;

public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private final Map<String, MLWorkspace> workspaces = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, MachineLearningModel> models = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, List<EvaluationMetric>> metrics = Collections.synchronizedMap(new LinkedHashMap<>());

    private DataStore() {
        seed();
    }

    private void seed() {
        MLWorkspace ws1 = new MLWorkspace("WS-VISION-01", "Computer Vision Lab", 500);
        MLWorkspace ws2 = new MLWorkspace("WS-NLP-02", "NLP Research Team", 250);
        MLWorkspace ws3 = new MLWorkspace("WS-EMPTY-03", "Empty Test Workspace", 100);
        workspaces.put(ws1.getId(), ws1);
        workspaces.put(ws2.getId(), ws2);
        workspaces.put(ws3.getId(), ws3);

        MachineLearningModel m1 = new MachineLearningModel("MOD-8832", "TensorFlow", "DEPLOYED", 0.92, "WS-VISION-01");
        MachineLearningModel m2 = new MachineLearningModel("MOD-1234", "PyTorch", "TRAINING", 0.75, "WS-NLP-02");
        MachineLearningModel m3 = new MachineLearningModel("MOD-5678", "Scikit-Learn", "DEPRECATED", 0.61, "WS-VISION-01");
        models.put(m1.getId(), m1);
        models.put(m2.getId(), m2);
        models.put(m3.getId(), m3);

        ws1.getModelIds().add(m1.getId());
        ws1.getModelIds().add(m3.getId());
        ws2.getModelIds().add(m2.getId());

        metrics.put("MOD-8832", Collections.synchronizedList(new ArrayList<>()));
        metrics.put("MOD-1234", Collections.synchronizedList(new ArrayList<>()));
        metrics.put("MOD-5678", Collections.synchronizedList(new ArrayList<>()));
    }

    public static DataStore getInstance() { return INSTANCE; }

    public Map<String, MLWorkspace> getWorkspaces() { return workspaces; }
    public Map<String, MachineLearningModel> getModels() { return models; }
    public Map<String, List<EvaluationMetric>> getMetrics() { return metrics; }
}
