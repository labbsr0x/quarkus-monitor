package br.com.rubim.test.metrics;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.test.MetricsFilterForError;
import br.com.rubim.test.RequestResource;
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
    var samples = Metrics.responseSizeBytes.collect().get(0).samples;

    assertEquals(samples.size(),
        0, "Metric response_size_bytes created besides disable");
  }
}
