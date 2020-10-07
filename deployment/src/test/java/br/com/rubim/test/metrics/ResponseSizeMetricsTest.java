package br.com.rubim.test.metrics;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.test.MetricsFilterForError;
import br.com.rubim.test.RequestResource;
import io.quarkus.test.QuarkusUnitTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ResponseSizeMetricsTest {

  private static final String SIMPLE_PATH = "/request/simple";
  private static final String ERROR_PATH = "/request/with-error/";
  private static final String EXCLUSION_BASE_PATH = "/request/metric-exclusion-";

  @RegisterExtension
  static QuarkusUnitTest test = new QuarkusUnitTest()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
          .addClasses(RequestResource.class, MetricsFilterForError.class)
          .addAsResource(
              new StringAsset(
                  "quarkus.b5.monitor.exclusions=/request/metric-exclusion-one,/request/metric-exclusion-two,/request/metric-exclusion-two/{id}\n"
                      + "quarkus.b5.monitor.enable-http-response-size=true\n"
              ),
              "application.properties")
      );

  @ConfigProperty(name = "quarkus.b5.monitor.error-message")
  String errorKey;

  @BeforeEach
  void cleanMetrics() {
    Metrics.responseSizeBytes.clear();
  }

  @Test
  void testCreatingResponseSizeBytesMetrics() {
    var tagValues = new String[]{"http", "200", "GET", SIMPLE_PATH, "false", ""};
    var tagNames = new String[]{"type", "status", "method", "addr", "isError", "errorMessage"};

    when().get(SIMPLE_PATH).then().statusCode(200);
    var samples = Metrics.responseSizeBytes.collect().get(0).samples;

    assertEquals(samples.size(),
        1, "Metric response_size_bytes with wrong number of samples");

    assertArrayEquals(tagValues, samples.get(0).labelValues.toArray(),
        "Tags of response_size_bytes with wrong values");
    assertArrayEquals(tagNames, samples.get(0).labelNames.toArray(),
        "Tags of response_size_bytes with wrong names");

    assertEquals(2d, samples.get(0).value, "Metric response_size_bytes with wrong value");

    when().get(SIMPLE_PATH).then().statusCode(200);
    samples = Metrics.responseSizeBytes.collect().get(0).samples;

    assertEquals(4d, samples.get(0).value, "Metric response_size_bytes with wrong sum value");
  }

  @Test
  void testCreatingResponseSizeBytesMetricsWithTagErrorInHeader() {
    when().get(ERROR_PATH + "header/400" + "/" + errorKey).then().statusCode(400);

    var sample = Metrics.responseSizeBytes.collect().get(0).samples.get(0);

    assertEquals("true", sample.labelValues.get(4),
        "Wrong value for Tag isError from header in response_size_bytes metrics");
    assertEquals("error with describe in header", sample.labelValues.get(5),
        "Wrong value for Tag errorMessage  from header in response_size_bytes metrics");
  }

  @Test
  void testCreatingResponseSizeBytesMetricsWithTagErrorInContainer() {
    when().get(ERROR_PATH + "container/400").then().statusCode(400);

    var sample = Metrics.responseSizeBytes.collect().get(0).samples.get(0);

    assertEquals("true", sample.labelValues.get(4),
        "Wrong value for Tag isError from container in response_size_bytes metrics");
    assertEquals("error with describe in container", sample.labelValues.get(5),
        "Wrong value for Tag errorMessage from container in response_size_bytes metrics");
  }

  @Test
  void testCreatingResponseSizeBytesMetricsExclusions() {
    when().get(EXCLUSION_BASE_PATH + "one").then().statusCode(200);
    assertEquals(0, Metrics.responseSizeBytes.collect().get(0).samples.size(),
        "Error in exclusion of path in response_size_bytes metrics with first path.");

    when().get(EXCLUSION_BASE_PATH + "two").then().statusCode(200);
    assertEquals(0, Metrics.responseSizeBytes.collect().get(0).samples.size(),
        "Error in exclusion of path in response_size_bytes metrics with second path.");

    when().get(EXCLUSION_BASE_PATH + "two/1").then().statusCode(200);
    assertEquals(0, Metrics.responseSizeBytes.collect().get(0).samples.size(),
        "Error in exclusion of path in response_size_bytes metrics with path param.");
  }
}