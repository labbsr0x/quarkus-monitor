package br.com.labbs.quarkusmonitor.deployment.test.metrics;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import br.com.labbs.quarkusmonitor.deployment.test.filters.MetricsFilterForError;
import br.com.labbs.quarkusmonitor.deployment.test.resources.DependencyResource;
import br.com.labbs.quarkusmonitor.deployment.test.resources.DependencyRestClient;
import br.com.labbs.quarkusmonitor.deployment.test.resources.RequestResource;
import io.quarkus.test.QuarkusUnitTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


class EndpointMetricsTest {

  private static final String SIMPLE_PATH = "/request/simple";

  @RestClient
  DependencyRestClient restClient;

  @ConfigProperty(name = "quarkus.application.version")
  String version;

  @RegisterExtension
  static QuarkusUnitTest test = new QuarkusUnitTest()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
          .addClasses(RequestResource.class,DependencyResource.class, MetricsFilterForError.class,
              DependencyRestClient.class)
          .addAsResource
              (new StringAsset("quarkus.application.version=1.0.0\n" +
                      "dependencyRestClient/mp-rest/url=${test.url}\n"+
                      "quarkus.b5.monitor.enable-http-response-size=true\n"),
              "application.properties")
      );

  @Test
  void testCreatingApplicationInfoMetrics() {
    var tagValues = new String[]{SIMPLE_PATH, "", "false", "GET", "200", "http"};
    var tagKeys = new String[]{"addr", "errorMessage", "isError", "method", "status", "type"};

    restClient.simple();
    when().get(SIMPLE_PATH).then().statusCode(200);

    given()
        .when().get("/metrics")
        .then()
        .statusCode(200)
        .body(not(containsString("<h1 class=\"container\">Internal Server Error</h1>")))
        .body(containsString("application_info{version=\""+version+"\",} 1.0"))

        .body(containsString("request_seconds_bucket{addr="))
        .body(containsString("request_seconds_count{addr="))
        .body(containsString("request_seconds_sum{addr="))

        .body(containsString("dependency_up{name="))

        .body(containsString("dependency_request_seconds_bucket{addr="))
        .body(containsString("dependency_request_seconds_count{addr="))
        .body(containsString("dependency_request_seconds_sum{addr="));

  }
}
