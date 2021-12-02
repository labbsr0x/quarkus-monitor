package br.com.labbs.quarkusmonitor.deployment.test.metrics;

import static br.com.labbs.quarkusmonitor.runtime.core.Metrics.DEPENDENCY_UP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.fail;

import br.com.labbs.quarkusmonitor.deployment.test.filters.MetricsFilterForError;
import br.com.labbs.quarkusmonitor.deployment.test.resources.DependencyResource;
import br.com.labbs.quarkusmonitor.deployment.test.resources.DependencyRestClient;
import br.com.labbs.quarkusmonitor.runtime.MonitorMetrics;
import br.com.labbs.quarkusmonitor.runtime.dependency.DependencyState;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.quarkus.test.QuarkusUnitTest;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DependencyUpMetricsTest {

  public static final String WRONG_TAG_VALUE = "Metric dependency_up with wrong tag value";

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

  @BeforeEach
  void cleanMetrics() {
    Metrics.globalRegistry.clear();
  }

  @Test
  void testStructOfDependencyUpMetric() {
    restClient.simple();

    assertNotNull(Metrics.globalRegistry.find(DEPENDENCY_UP).meter(), "Metric dependency_up not found");
    assertNotNull(Metrics.globalRegistry.get(DEPENDENCY_UP).meter().getId().getTag("name"),
        "Metric dependency_up with wrong tag name");
    assertEquals(DependencyRestClient.class.getName(),
        Metrics.globalRegistry.get(DEPENDENCY_UP).meter().getId().getTag("name"), WRONG_TAG_VALUE);
  }

  @Test
  void testDependencyUpMetric() {
    restClient.simple();

    var samples = Metrics.globalRegistry.find(DEPENDENCY_UP).gauges();

    assertEquals(1, samples.size(),
        "Metric with wrong number of samples");

    var sample = samples.toArray(new Gauge[0])[0];

    assertEquals(1d, sample.value(), "Metric dependency_up is down with status code 200");

    try {
      restClient.simple(400);
      fail("Exception not thrown for dependency");
    } catch (Exception e) {
      assertEquals(1d, sample.value(), "Metric dependency_up is down with status code 400");
    }
  }

  @Test
  void testDependencyDownMetric() {
    try {
      restClient.simple(500);
      fail("Exception not thrown for dependency");
    } catch (Exception e) {
      var samples = Metrics.globalRegistry.find(DEPENDENCY_UP).gauges();

      assertEquals(1, samples.size(),
          "Metric with wrong number of samples");

      var sample = samples.toArray(new Gauge[0])[0];

      assertEquals(0d, sample.value(), "Metric dependency_up is up with status code 500");
    }
  }

  @Test
  void testDependencyUpMetricRemovedByMonitorMetrics() {
    dependencyUpMetricCreateByMonitorMetrics("myChecker", DependencyState.UP);
    MonitorMetrics.INSTANCE.cancelDependencyChecker("myChecker");

    assertEquals(0, MonitorMetrics.INSTANCE.listOfCheckersScheduled().size(),
        "cancelDependencyChecker does not remove the checker ");
  }

  @Test
  void testDependencyDownMetricCreateByMonitorMetrics() {
    dependencyUpMetricCreateByMonitorMetrics("myChecker", DependencyState.UP);
  }

  @Test
  void testDependencyUpMetricCreateByMonitorMetrics() {
    dependencyUpMetricCreateByMonitorMetrics("myChecker", DependencyState.DOWN);
  }

  private void dependencyUpMetricCreateByMonitorMetrics(String checker, DependencyState state) {
    MonitorMetrics.INSTANCE.addDependencyChecker(checker, () -> state,
        100, TimeUnit.MILLISECONDS);

    assertTimeout(Duration.ofMillis(200), () -> {
      while (Metrics.globalRegistry.find(DEPENDENCY_UP).gauges().size() <= 0) {
        try {
          TimeUnit.MILLISECONDS.sleep(1);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }, "Timeout to execute a dependency checker");

    var sample = Metrics.globalRegistry.find(DEPENDENCY_UP).gauges().toArray(new Gauge[0])[0];

    assertEquals("myChecker", Metrics.globalRegistry.get(DEPENDENCY_UP).meter().getId().getTag("name"),
        WRONG_TAG_VALUE);

    assertEquals(state.getValue(), sample.value(), "Metric dependency_up is "
        + (sample.value() == 1d ? "up" : "down"));
  }
}
