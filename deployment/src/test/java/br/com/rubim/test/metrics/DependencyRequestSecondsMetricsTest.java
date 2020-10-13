package br.com.rubim.test.metrics;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.test.fake.filters.DependencyMapper;
import br.com.rubim.test.fake.filters.MetricsFilterForError;
import br.com.rubim.test.fake.resources.DependencyResource;
import br.com.rubim.test.fake.resources.DependencyRestClient;
import io.quarkus.test.QuarkusUnitTest;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DependencyRequestSecondsMetricsTest {

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

  @ConfigProperty(name = "quarkus.b5.monitor.buckets")
  List<String> buckets;

  @Test
  void testStructOfDependencyUpMetric() {
    var tagValues = new String[]{DependencyRestClient.class.getName(),
        "http", "200", "GET", SIMPLE_PATH, "false", "", buckets.get(0)};

    var tagNames = new String[]{"name",
        "type", "status", "method", "addr", "isError", "errorMessage", "le"};

    restClient.simple();

    var samples = Metrics.dependencyRequestSeconds.collect().get(0).samples;

    assertEquals(samples.size(),
        buckets.size() + 3, "Metric dependency_request_seconds with wrong number of samples");

    var sampleBucket = samples.stream()
        .filter(sample -> "dependency_request_seconds_bucket".equals(sample.name)).findFirst();

    assertTrue(sampleBucket.isPresent(),
        "Metric dependency_request_seconds with wrong number of samples");

    sampleBucket.ifPresent(s -> {
      assertArrayEquals(tagValues, s.labelValues.toArray(), "Tags with wrong values");
      assertArrayEquals(tagNames, s.labelNames.toArray(), "Tags with wrong names");
    });

    var bucketSamples = samples.stream()
        .filter(sample -> "dependency_request_seconds_bucket".equals(sample.name)).collect(
            Collectors.toList());

    assertEquals(buckets.size() + 1, bucketSamples.size(),
        "Metric dependency_request_seconds with wrong number of samples");

    var sampleCount = samples.stream()
        .filter(sample -> "dependency_request_seconds_count".equals(sample.name)).findFirst();

    assertTrue(sampleCount.isPresent(),
        "Metric sample for dependency_request_seconds_count not found.");
    sampleCount.ifPresent(
        s -> assertEquals(1, s.value, "Metric dependency_request_seconds_count with wrong value"));

    var sampleSum = samples.stream()
        .filter(sample -> "dependency_request_seconds_sum".equals(sample.name))
        .collect(Collectors.toList());

    assertEquals(1d, sampleSum.size(),
        "Metric sample for dependency_request_seconds_sum not found.");
    assertTrue(sampleSum.get(0).value > 0d,
        "Metric dependency_request_seconds_sum with wrong value");

    restClient.simple();
    samples = Metrics.dependencyRequestSeconds.collect().get(0).samples;

    sampleCount = samples.stream()
        .filter(sample -> "dependency_request_seconds_count".equals(sample.name)).findFirst();

    assertTrue(sampleCount.isPresent(),
        "Metric sample for dependency_request_seconds_count not found.");

    sampleCount.
        ifPresent(s -> assertEquals(2d, s.value,
            "Metric dependency_request_seconds_count with wrong value"));
  }
}
