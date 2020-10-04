package br.com.rubim.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/request")
public class RequestResource {


  @GET
  @Path("/simple")
  @Produces(MediaType.TEXT_PLAIN)
  public String simpleRequest() {
    return "OK";
  }

  @GET
  @Path("/with-one-path-param/{myparam}")
  @Produces(MediaType.TEXT_PLAIN)
  public String requestWithOnePathParam(final @PathParam("myparam") String myparam) {
    return "OK";
  }

  @GET
  @Path("/with-two-path-param/{first}/another/{second}")
  @Produces(MediaType.TEXT_PLAIN)
  public String requestWithTwoPathParam(final @PathParam("first") String first,
      final @PathParam("second") String second) {
    return "OK";
  }

  @GET
  @Path("/with-query-param")
  @Produces(MediaType.TEXT_PLAIN)
  public String requestWithQueryParam(final @QueryParam("first") String first) {
    return "OK";
  }

  @GET
  @Path("/with-path-and-query-param/{path}")
  @Produces(MediaType.TEXT_PLAIN)
  public String requestWithPathAndQueryParam(final @PathParam("path") String myparam,
      final @QueryParam("query") String first) {
    return "OK";
  }

  @GET
  @Path("/metric-exclusion-one")
  @Produces(MediaType.TEXT_PLAIN)
  public String metricExclusionOne() {
    return "OK";
  }

  @GET
  @Path("/metric-exclusion-two")
  @Produces(MediaType.TEXT_PLAIN)
  public String metricExclusionTwo() {
    return "OK";
  }

  @GET
  @Path("/metric-exclusion-/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  public String metricExclusionTwo(final @PathParam("path") String myparam) {
    return "OK";
  }
}
