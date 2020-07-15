package br.com.rubim.runtime;

public enum MetricsEnum {
    REQUEST_SECONDS_BUCKET(
            "is a metric that defines the histogram of how many requests are falling into the well defined buckets represented by the label le"),
    REQUEST_SECONDS_COUNT("is a counter that counts the overall number of requests with those exact label occurrences;"),
    REQUEST_SECONDS_SUM(
            "is a counter that counts the overall sum of how long the requests with those exact label occurrences are taking;"),
    RESPONSE_SIZE_BYTES(
            "is a counter that computes how much data is being sent back to the user for a given request type. It captures the response size from the content-length response header. If there is no such header, the value exposed as metric will be zero"),
    DEPENDENCY_UP(
            "is a metric to register weather a specific dependency is up (1) or down (0). The label name registers the dependency name"),
    DEPENDENCY_REQUEST_SECONDS_BUCKET(
            " is a metric that defines the histogram of how many requests to a specific dependency are falling into the well defined buckets represented by the label le"),
    DEPENDENCY_REQUEST_SECONDS_COUNT("is a counter that counts the overall number of requests to a specific dependency"),
    DEPENDENCY_REQUEST_SECONDS_SUM(
            "is a counter that counts the overall sum of how long requests to a specific dependency are taking"),
    APPLICATION_INFO("holds static info of an application, such as it's semantic version number");

    private String description;
    private String name;

    MetricsEnum(String description) {
        this.description = description;
        this.name = this.name().toLowerCase();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}