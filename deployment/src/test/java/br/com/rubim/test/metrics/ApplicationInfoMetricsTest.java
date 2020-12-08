package br.com.labbs.quarkusmonitor.test.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.com.labbs.quarkusmonitor.test.fake.resources.RequestResource;
import io.micrometer.core.instrument.Metrics;
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
    assertNotNull(Metrics.globalRegistry.find("application_info").counter(),
        "Metric application info not found");
    assertEquals(1d, Metrics.globalRegistry.get("application_info").counter().count(),
        "Metric with wrong value");
    assertEquals(version,
        Metrics.globalRegistry.get("application_info").counter().getId().getTag("version"),
        "Metric with wrong label value");
  }
}
