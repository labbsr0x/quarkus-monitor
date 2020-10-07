package br.com.rubim.test;

import java.io.IOException;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@Priority(Priorities.USER)
public class MetricsFilterForError implements ContainerRequestFilter, ContainerResponseFilter {

  @ConfigProperty(name = "quarkus.b5.monitor.error-message")
  String errorKey;

  @Override
  public void filter(ContainerRequestContext request) throws IOException {
  }

  @Override
  public void filter(
      ContainerRequestContext request,
      ContainerResponseContext response)
      throws IOException {

    if (response.getStatus() >= 400) {
      request.setProperty(errorKey, "error with describe in container");
    }
  }
}
