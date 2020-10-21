package br.com.rubim.test.fake.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/dep")
public class DependencyResource {

  @GET
  @Path("/simple")
  @Produces(MediaType.TEXT_PLAIN)
  public String simpleRequest() {
    return "OK";
  }

  @GET
  @Path("/simple/{status}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response simpleRequest(final @PathParam("status") Integer status) {
    return Response.status(status).entity("OK").build();
  }

  @GET
  @Path("/simple/header/{status}/{msg}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response simpleRequestHeader(final @PathParam("status") Integer status,
      final @PathParam("msg") String msg) {
    return Response.status(status).header("error-info", msg).entity("OK").build();
  }

  @GET
  @Path("/simple/container/{status}/{msg}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response simpleRequest(final @PathParam("status") Integer status,
      final @PathParam("msg") String msg) {
    return Response.status(status).entity("OK").build();
  }
}
