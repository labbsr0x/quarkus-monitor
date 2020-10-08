package br.com.rubim.test.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.test.fake.filters.DependencyMapper;
import br.com.rubim.test.fake.filters.MetricsFilterForError;
import br.com.rubim.test.fake.resources.DependencyResource;
import br.com.rubim.test.fake.resources.DependencyRestClient;
import io.quarkus.test.QuarkusUnitTest;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DependencyUpMetricTest {

  private static final String SIMPLE_PATH = "/dep/simple";

  @RegisterExtension
  static QuarkusUnitTest config = new QuarkusUnitTest()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
          .addClasses(DependencyResource.class, MetricsFilterForError.class,
              DependencyRestClient.class,
              DependencyMapper.class)
          .addAsResource(
              new StringAsset("quarkus.b5.monitor.enable-http-response-size=false\n" +
                  "br.com.rubim.test.fake.resources.DependencyRestClient/mp-rest/url=${test.url}"),
              "application.properties"));

  @RestClient
  DependencyRestClient restClient;

  @Test
  void testStructOfDependencyUpMetric() {
    restClient.simple();
    var samples = Metrics.dependencyUp.collect().get(0).samples;

    assertEquals(1, samples.size(),
        "Metric dependency_up not created");

    assertEquals("name", samples.get(0).labelNames.get(0),
        "Metric dependency_up with wrong tag name");

    assertEquals(DependencyRestClient.class.getName(),
        samples.get(0).labelValues.get(0), "Metric dependency_up with wrong tag value");
  }

  @Test
  void testDependencyUpMetric() {
    restClient.simple();
    var samples = Metrics.dependencyUp.collect().get(0).samples;

    assertEquals(DependencyRestClient.class.getName(),
        samples.get(0).labelValues.get(0), "Metric dependency_up with wrong tag value");

    assertEquals(1d, samples.get(0).value, "Metric dependency_up is down with status code 200");

    try {
      restClient.simple(400);
      fail("Exception not thrown for dependency");
    } catch (Exception e) {
      samples = Metrics.dependencyUp.collect().get(0).samples;
      assertEquals(1d, samples.get(0).value, "Metric dependency_up is down with status code 400");
    }
  }


  @Test
  void testDependencyDownMetric() {
    try {
      restClient.simple(500);
      fail("Exception not thrown for dependency");
    } catch (Exception e) {
      var samples = Metrics.dependencyUp.collect().get(0).samples;

      assertEquals(DependencyRestClient.class.getName(),
          samples.get(0).labelValues.get(0), "Metric dependency_up with wrong tag value");

      assertEquals(0d, samples.get(0).value, "Metric dependency_up is up with status code 500");
    }
  }
}
