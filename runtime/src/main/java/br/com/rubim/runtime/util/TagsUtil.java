package br.com.rubim.runtime.util;

import org.eclipse.microprofile.metrics.Tag;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import java.lang.reflect.Method;

public class TagsUtil {
    public static String[] extractLabelValues(ContainerRequestContext containerRequestContext,
            ContainerResponseContext containerResponseContext) {
        return new String[]{
                "http",
                containerRequestContext.getMethod(),
                containerRequestContext.getUriInfo().getPath(),
                Integer.toString(containerResponseContext.getStatus()),
                Boolean.toString(
                        containerResponseContext.getStatus() < 200 || containerResponseContext.getStatus() >= 400)
        };
    }
    public static String[] extractLabelValues(ClientRequestContext request,
            ClientResponseContext response) {
        Method method = (Method) request.getProperty("org.eclipse.microprofile.rest.client.invokedMethod");

        return new String[]{
                method.getDeclaringClass().getCanonicalName(),
                "http",
                request.getMethod(),
                request.getUri().getPath(),
                Integer.toString(response.getStatus()),
                Boolean.toString(
                        response.getStatus() < 200 || response.getStatus() >= 400)
        };
    }
}
