package br.com.rubim.test.fake.resources;

import br.com.rubim.test.fake.filters.DependencyMapper;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/dep")
@RegisterRestClient
@RegisterProvider(DependencyMapper.class)
public interface DependencyRestClient {

  @GET
  @Path("/simple")
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes(MediaType.TEXT_PLAIN)
  String simple();

  @GET
  @Path("/simple/{status}")
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes(MediaType.TEXT_PLAIN)
  Response simple(final @PathParam("status") Integer status);
}
