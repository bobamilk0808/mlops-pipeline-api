package com.mlops;

import com.mlops.exception.GlobalExceptionMapper;
import com.mlops.exception.LinkedWorkspaceNotFoundExceptionMapper;
import com.mlops.exception.ModelDeprecatedExceptionMapper;
import com.mlops.exception.WorkspaceNotEmptyExceptionMapper;
import com.mlops.filter.LoggingFilter;
import com.mlops.resource.DiscoveryResource;
import com.mlops.resource.ModelResource;
import com.mlops.resource.WorkspaceResource;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/api/v1")
public class MLOpsApplication extends ResourceConfig {

    public MLOpsApplication() {
        register(DiscoveryResource.class);
        register(WorkspaceResource.class);
        register(ModelResource.class);
        register(JacksonFeature.class);
        register(WorkspaceNotEmptyExceptionMapper.class);
        register(LinkedWorkspaceNotFoundExceptionMapper.class);
        register(ModelDeprecatedExceptionMapper.class);
        register(GlobalExceptionMapper.class);
        register(LoggingFilter.class);
    }
}
