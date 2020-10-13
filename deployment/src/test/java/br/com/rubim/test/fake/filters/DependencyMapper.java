package br.com.rubim.test.fake.filters;

import java.util.Optional;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class DependencyMapper implements ResponseExceptionMapper<Exception> {

  @ConfigProperty(name = "quarkus.b5.monitor.error-message")
  String errorKey;

  @Override
  public Exception toThrowable(Response response) {
    return new Exception(
        Optional.ofNullable(response.getHeaderString(errorKey)).orElse("Error in dependency"));
  }

  @Override
  public boolean handles(int status, MultivaluedMap<String, Object> headers) {
    return status >= 400;
  }


}
