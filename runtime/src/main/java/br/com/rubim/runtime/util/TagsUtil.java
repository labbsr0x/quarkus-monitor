package br.com.rubim.runtime.util;

import org.eclipse.microprofile.metrics.Tag;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import java.lang.reflect.Method;

public class TagsUtil {
    public static Tag[] extractTags(ContainerRequestContext containerRequestContext,
            ContainerResponseContext containerResponseContext) {
        return new Tag[] {
                new Tag("type", "http"),
                new Tag("method", containerRequestContext.getMethod()),
                new Tag("addr", containerRequestContext.getUriInfo().getPath()),
                new Tag("status", Integer.toString(containerResponseContext.getStatus())),
                new Tag("isError",
                        Boolean.toString(
                                containerResponseContext.getStatus() < 200 || containerResponseContext.getStatus() >= 400))
        };
    }

    public static Tag[] extractTags(ClientRequestContext request,
            ClientResponseContext response) {
        Method method = (Method) request.getProperty("org.eclipse.microprofile.rest.client.invokedMethod");
        return new Tag[] {
                new Tag("name", method.getDeclaringClass().getCanonicalName()),
                new Tag("type", "http"),
                new Tag("method", request.getMethod()),
                new Tag("addr", request.getUri().getPath()),
                new Tag("status", Integer.toString(response.getStatus())),
                new Tag("isError",
                        Boolean.toString(
                                response.getStatus() < 200 || response.getStatus() >= 400))
        };
    }
}
