package br.com.rubim.test.metrics;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import br.com.rubim.test.fake.resources.RequestResource;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class EndpointMetrics {

  @RegisterExtension
  static QuarkusUnitTest test = new QuarkusUnitTest()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
          .addClasses(RequestResource.class)
          .addAsResource(new StringAsset("quarkus.application.version=1.0.0"),
              "application.properties")
      );

  @Test
  public void testCreatingApplicationInfoMetrics() {
    given()
        .when().get("/metrics")
        .then()
        .statusCode(200)
        .body(not(containsString("<h1 class=\"container\">Internal Server Error</h1>")))
        .body(containsString("application_info"));
  }
}
