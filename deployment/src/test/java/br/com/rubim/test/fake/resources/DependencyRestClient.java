package br.com.labbs.quarkusmonitor.test.fake.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/dep")
@RegisterRestClient
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

  @GET
  @Path("/simple/header/{status}/{msg}")
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes(MediaType.TEXT_PLAIN)
  String simpleHeader(final @PathParam("status") Integer status,
      final @PathParam("msg") String msg);

  @GET
  @Path("/simple/container/{status}/{msg}")
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes(MediaType.TEXT_PLAIN)
  String simpleContainer(final @PathParam("status") Integer status,
      final @PathParam("msg") String msg);
}
