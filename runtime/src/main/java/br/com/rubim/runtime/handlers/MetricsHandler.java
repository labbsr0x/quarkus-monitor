package br.com.rubim.runtime.handlers;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.smallrye.metrics.MetricsRequestHandler;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import java.io.IOException;
import java.util.Enumeration;
import java.util.stream.Stream;

public class MetricsHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsHandler.class);
    String path;
    private CollectorRegistry registry = CollectorRegistry.defaultRegistry;

    public MetricsHandler() {
        this.path = "/metrics";
    }

    public MetricsHandler(String path) {
        this.path = path;
    }

    private static void writeEscapedLabelValue(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                default:
                    sb.append(c);
            }
        }
    }

    @Override public void handle(RoutingContext routingContext) {
        MetricsRequestHandler internalHandler = CDI.current().select(MetricsRequestHandler.class).get();
        HttpServerResponse response = routingContext.response();
        HttpServerRequest request = routingContext.request();
        Stream<String> acceptHeaders = request.headers().getAll("Accept").stream();
        final StringBuilder sb = new StringBuilder();
        try {
            internalHandler.handleRequest(request.path(), this.path, request.rawMethod(), acceptHeaders,
                    (status, message, headers) -> {
                        response.setStatusCode(status);
                        headers.forEach(response::putHeader);
                        sb.append(message);
                    });
            sb.append(readMetricsFromPromClient(registry.metricFamilySamples()));
            response.end(Buffer.buffer(sb.toString()));
        } catch (IOException e) {
            response.setStatusCode(503);
            response.end();
            LOGGER.error("", e);
        }
    }

    public String readMetricsFromPromClient(Enumeration<Collector.MetricFamilySamples> mfs) {
        final StringBuilder sb = new StringBuilder();
        while (mfs.hasMoreElements()) {
            Collector.MetricFamilySamples metricFamilySamples = mfs.nextElement();
            sb.append("# HELP ");
            sb.append(metricFamilySamples.name);
            sb.append(' ');
            sb.append(metricFamilySamples.help);
            sb.append('\n');
            sb.append("# TYPE ");
            sb.append(metricFamilySamples.name);
            sb.append(' ');
            sb.append(metricFamilySamples.type.name().toLowerCase());
            sb.append('\n');
            for (Collector.MetricFamilySamples.Sample sample : metricFamilySamples.samples) {
                sb.append(sample.name);
                if (sample.labelNames.size() > 0) {
                    sb.append('{');
                    for (int i = 0; i < sample.labelNames.size(); ++i) {
                        sb.append(sample.labelNames.get(i));
                        sb.append("=\"");
                        writeEscapedLabelValue(sb, sample.labelValues.get(i));
                        sb.append("\",");
                    }
                    sb.append('}');
                }
                sb.append(' ');
                sb.append(Collector.doubleToGoString(sample.value));
                if (sample.timestampMs != null) {
                    sb.append(' ');
                    sb.append(sample.timestampMs.toString());
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
