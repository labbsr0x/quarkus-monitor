package br.com.labbs.quarkusmonitor.deployment.test.metrics;

import static br.com.labbs.quarkusmonitor.runtime.core.Metrics.DEPENDENCY_REQUEST;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.labbs.quarkusmonitor.deployment.test.filters.MetricsFilterForError;
import br.com.labbs.quarkusmonitor.deployment.test.resources.DependencyResource;
import br.com.labbs.quarkusmonitor.deployment.test.resources.DependencyRestClient;
import br.com.labbs.quarkusmonitor.runtime.MonitorMetrics;
import br.com.labbs.quarkusmonitor.runtime.dependency.DependencyEvent;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.quarkus.test.QuarkusUnitTest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class DependencyRequestSecondsMetricsTest {

  private static final String SIMPLE_PATH = "/dep/simple";

  @RegisterExtension
  static QuarkusUnitTest config = new QuarkusUnitTest()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
          .addClasses(DependencyResource.class, MetricsFilterForError.class,
              DependencyRestClient.class)
          .addAsResource(
              new StringAsset("quarkus.b5.monitor.enable-http-response-size=false\n" +
                  "dependencyRestClient/mp-rest/url=${test.url}"),
              "application.properties"));

  @RestClient
  DependencyRestClient restClient;

  @ConfigProperty(name = "quarkus.b5.monitor.error-message", defaultValue = "error-info")
  String errorKey;

  @ConfigProperty(name = "quarkus.b5.monitor.buckets", defaultValue = "0.1, 0.3, 1.5, 10.5")
  List<String> buckets;

  @BeforeEach
  void cleanMetrics() {
    Metrics.globalRegistry.clear();
  }

  @Test
  void testStructOfDependencyRequestMetric() {
    var tagValues = new String[]{SIMPLE_PATH + "/{status}", "", "false", "GET",
        DependencyRestClient.class.getName(), "200", "http"};
    var tagKeys = new String[]{"addr", "errorMessage", "isError", "method", "name", "status",
        "type"};

    restClient.simple(200);

    var samples = Metrics.globalRegistry.find(DEPENDENCY_REQUEST).timers();

    assertEquals(1, samples.size(),
        "Metric with wrong number of samples");

    var sample = samples.toArray(new Timer[0])[0];
    assertEquals(buckets.size(), sample.takeSnapshot().histogramCounts().length,
        "Metric with wrong number of buckets");

    var actualTagValues = sample.getId().getTags().stream().map(Tag::getValue)
        .toArray(String[]::new);
    var actualTagKeys = sample.getId().getTags().stream().map(Tag::getKey).toArray(String[]::new);

    assertArrayEquals(tagKeys, actualTagKeys, "Tags of " + DEPENDENCY_REQUEST + " with wrong names");
    assertArrayEquals(tagValues, actualTagValues, "Tags of " + DEPENDENCY_REQUEST + " with wrong values");

    assertTrue(sample.totalTime(TimeUnit.MILLISECONDS) > 0, "Metric " + DEPENDENCY_REQUEST + "_sum with wrong value");
    assertEquals(1, sample.count(), "Metric " + DEPENDENCY_REQUEST + "_count with wrong value");

    restClient.simple(200);
    assertEquals(2, sample.count(), "Metric " + DEPENDENCY_REQUEST + "_count with wrong value");
  }

  @Test
  void testCreatingRequestMetricsWithTagErrorInHeader() {
    var msgError = "error message example";
    try {
      restClient.simpleHeader(400, msgError);
    } catch (WebApplicationException e) {
      assertEquals(msgError, e.getResponse().getHeaderString(errorKey));
    }

    createRequestMetricsWithError(msgError);
  }

  @Test
  void testCreatingRequestMetricsWithTagErrorInContainer() {
    var msgError = "error with describe in container";
    try {
      restClient.simpleContainer(400, msgError);
    } catch (WebApplicationException e) {
      assertEquals(400, e.getResponse().getStatus());
    }

    createRequestMetricsWithError(msgError);
  }

  private void createRequestMetricsWithError(String msgError) {
    var sample = Metrics.globalRegistry.find(DEPENDENCY_REQUEST).meters().toArray(new Meter[0])[0];

    assertEquals("true", sample.getId().getTag("isError"),
        "Receive wrong value for Tag isError");
    assertEquals(msgError, sample.getId().getTag("errorMessage"),
        "Receive wrong value for Tag error message");
  }

  @Test
  void testCreatingOfDependencyRequestMetricByMonitorMetrics() {
    var tagValues = new String[]{"myAddress", "my error message", "true", "GET", "myChecker", "OK",
        "other"};
    var tagKeys = new String[]{"addr", "errorMessage", "isError", "method", "name", "status",
        "type"};

    DependencyEvent dependencyEvent = new DependencyEvent("myChecker")
        .setType("other")
        .setStatus("OK")
        .setMethod("GET")
        .setAddress("myAddress")
        .setIsError(true)
        .setErrorMessage("my error message");

    MonitorMetrics.INSTANCE.addDependencyEvent(dependencyEvent, 1d);

    var samples = Metrics.globalRegistry.find(DEPENDENCY_REQUEST).timers();

    assertEquals(1, samples.size(),
        "Metric with wrong number of samples");

    var sample = samples.toArray(new Timer[0])[0];
    assertEquals(buckets.size(), sample.takeSnapshot().histogramCounts().length,
        "Metric with wrong number of buckets");

    var actualTagValues = sample.getId().getTags().stream().map(Tag::getValue)
        .toArray(String[]::new);
    var actualTagKeys = sample.getId().getTags().stream().map(Tag::getKey).toArray(String[]::new);

    assertArrayEquals(tagKeys, actualTagKeys, "Tags of " + DEPENDENCY_REQUEST + " with wrong names");
    assertArrayEquals(tagValues, actualTagValues, "Tags of " + DEPENDENCY_REQUEST + " with wrong values");

    assertTrue(sample.totalTime(TimeUnit.MILLISECONDS) > 0, "Metric " + DEPENDENCY_REQUEST + "_sum with wrong value");
    assertEquals(1, sample.count(), "Metric " + DEPENDENCY_REQUEST + "_count with wrong value");
  }
}
