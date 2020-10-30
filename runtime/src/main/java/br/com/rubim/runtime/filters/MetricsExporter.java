package br.com.rubim.runtime.filters;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;
import org.eclipse.microprofile.config.ConfigProvider;

public class MetricsExporter implements Handler<RoutingContext> {

    private String metricsPath = ConfigProvider.getConfig().getOptionalValue("quarkus.smallrye-metrics.path", String.class)
        .orElse(ConfigProvider.getConfig()
            .getOptionalValue("quarkus.micrometer.export.prometheus.path", String.class)
            .orElse("/metrics"));

    public String getMetricsDefaultRegistry() {
        try (StringWriter writer = new StringWriter()) {
            TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
            writer.flush();
            return writer.toString();
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    public void handle(RoutingContext rc) {
        String path = Objects.requireNonNullElse(rc.normalisedPath(), "");
        if (path.equals(metricsPath)) {
            HttpServerResponse response = rc.response();
            response.setChunked(true);
            response.write(getMetricsDefaultRegistry());
        }

      rc.next();
    }
}
