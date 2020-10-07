package br.com.rubim.test.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.test.RequestResource;
import io.quarkus.test.QuarkusUnitTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ApplicationInfoMetricsTest {

  @RegisterExtension
  static QuarkusUnitTest test = new QuarkusUnitTest()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
          .addClasses(RequestResource.class)
          .addAsResource(new StringAsset("quarkus.application.version=1.0.0"),
              "application.properties")

      );

  @ConfigProperty(name = "quarkus.application.version")
  String version;

  @Test
  void testCreatingApplicationInfoMetrics() {
    var samples = Metrics.applicationInfo.collect().get(0).samples;
    assertEquals(1d, samples.get(0).value, "Metric with wrong value");
    assertEquals(version, samples.get(0).labelValues.get(0), "Metric with wrong label value");
  }
}
