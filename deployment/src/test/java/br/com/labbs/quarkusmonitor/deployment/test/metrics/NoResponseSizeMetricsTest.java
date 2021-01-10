package br.com.labbs.quarkusmonitor.deployment.test.metrics;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertNull;

import br.com.labbs.quarkusmonitor.deployment.test.filters.MetricsFilterForError;
import br.com.labbs.quarkusmonitor.deployment.test.resources.RequestResource;
import io.micrometer.core.instrument.Metrics;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class NoResponseSizeMetricsTest {

  private static final String SIMPLE_PATH = "/request/simple";

  @RegisterExtension
  static QuarkusUnitTest test = new QuarkusUnitTest()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
          .addClasses(RequestResource.class, MetricsFilterForError.class)
          .addAsResource(
              new StringAsset("quarkus.b5.monitor.enable-http-response-size=false\n"
              ),
              "application.properties")
      );

  @Test
  void testNotCreatingResponseSizeBytesMetrics() {
    when().get(SIMPLE_PATH).then().statusCode(200);

    assertNull(
        Metrics.globalRegistry.find("response_size_bytes").counter(),
        "Metric response_size_bytes created besides disable");
  }
}
