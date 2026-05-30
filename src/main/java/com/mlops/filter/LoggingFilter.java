package com.mlops.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        LOGGER.info(String.format(
                "[REQUEST]  %-6s %s | Content-Type: %s | User-Agent: %s",
                req.getMethod(),
                req.getUriInfo().getRequestUri(),
                req.getHeaderString("Content-Type"),
                req.getHeaderString("User-Agent")));
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        LOGGER.info(String.format(
                "[RESPONSE] %-6s %s | Status: %d %s",
                req.getMethod(),
                req.getUriInfo().getRequestUri(),
                res.getStatus(),
                res.getStatusInfo().getReasonPhrase()));
    }
}
