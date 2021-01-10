package br.com.labbs.quarkusmonitor.deployment.test.filters;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@Priority(Priorities.USER)
public class MetricsFilterForError implements ContainerResponseFilter, ClientResponseFilter {

  @ConfigProperty(name = "quarkus.b5.monitor.error-message")
  String errorKey;

  @Override
  public void filter(ContainerRequestContext request,
      ContainerResponseContext response) {
    if (response.getStatus() >= 400) {
      request.setProperty(errorKey, "error with describe in container");
    }
  }

  @Override
  public void filter(ClientRequestContext clientRequestContext,
      ClientResponseContext clientResponseContext) {
    if (clientResponseContext.getStatus() >= 400) {
      clientRequestContext.setProperty(errorKey, "error with describe in container");
    }
  }
}
