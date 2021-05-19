[![Maven Central](https://img.shields.io/maven-central/v/br.com.labbs/quarkus-monitor.svg?label=Maven%20Central)](https://search.maven.org/artifact/br.com.labbs/quarkus-monitor)
# Extension to add BB Metrics to Quarkus Projects

This projects implements the [Big Brother](https://github.com/labbsr0x/big-brother) Metrics specfication through a [Quarkus](https://quarkus.io) extension.

## Metrics 

This extensions uses quarkus micrometer extension to create the following metrics 

```
application_info{version}
request_seconds_bucket{type, status, isError, errorMessage, method, addr, le}
request_seconds_count{type, status, isError, errorMessage, method, addr}
request_seconds_sum{type, status, isError, errorMessage, method, addr}
response_size_bytes{type, status, isError, errorMessage, method, addr}
dependency_up{name}
dependency_request_seconds_bucket{name, type, status, isError, errorMessage, method, addr, le}
dependency_request_seconds_count{name, type, status, isError, errorMessage, method, add}
dependency_request_seconds_sum{name, type, status, isError, errorMessage, method, add}
```

Details:

1. The `request_seconds_bucket` metric defines the histogram of the amount of requests are falling into the well-defined buckets represented by the label `le`;

2. The `request_seconds_count` is a counter that counts the overall number of requests with those exact label occurrences;

3. The `request_seconds_sum` is a counter of the overall sum of how long the requests with those exact label occurrences are taking;

4. The `response_size_bytes` is a counter that computes how much data is being sent back to the user for a given request type. 
This metric is disabled by default for performance.

5. The `dependency_up` is a metric to register whether a specific dependency is up (1) or down (0). The label `name` registers the dependency name;

6. The `dependency_request_seconds_bucket` is a metric that defines the histogram of the amount of requests to a specific dependency are falling into the defined buckets represented by the label le;

7. The `dependency_request_seconds_count` is a counter that counts the overall number of requests to a specific dependency;

8. The `dependency_request_seconds_sum` is a counter of the overall sum of how long requests to a specific dependency are taking;

9. The `application_info` holds static info of an application, such as its semantic version number;

Labels:

1. `type` tells which request protocol was used (e.g. `grpc` or `http`);

2. `status` registers the response status (e.g. HTTP status code);

3. `method` registers the request method;

4. `addr` registers the requested endpoint address, if the path has path params it will be replaced by the key between {};

5. `version` tells which version of your app handled the request;

6. `isError` lets us know if the status code reported is an error or not;

7. `errorMessage` registers the error message, passed by headers or property in ContainerRequestContext or;

8. `name` registers the name of the dependency;


### How it works

In this project we have three types of metrics, request, dependency and application info, the first two are created using filters, the third is for holding static version info. 

#### Request
The filters in the request metric type are created using ContainerRequestFilter, ContainerResponseFilter from JAX-RS, 
so only the endpoint using JAX-RS will have its metrics created automatically, and you can disable it using the [exclusion path configuration](#how-to-config).
The addr tag will be filled with the path in the same pattern used in JAX-RS, like  `@Path("/user/{id})`.

But if you need to create your own request metric you could use the addRequestEvent method in MonitorMetrics.

#### Dependency

This project defines as Dependency any service called through REST. The dependency metrics are collected 
by a [MicroProfile Rest Client Provider](https://download.eclipse.org/microprofile/microprofile-rest-client-1.2.1/microprofile-rest-client-1.2.1.html#providers) implementation.
The name of the dependency is the interface name which defines the RestClient. In this implementation, all requests made using rest client will create two metrics,
which are dependency_up and dependency_request. The addr tag will be filled with path in the same pattern used in JAX-RS, like  `@Path("/user/{id})`.

##### Dependency_up
If the request made using RestClient returns a status code between 200 and 499 it will be considered up, 
if status code is greater than or equal 500 it will be consider down. 

If you needed to create your own dependency_up metric, you can use the addDependencyChecker method in MonitorMetrics to add a checker if your dependency is up or down.

##### Dependency_request
You can create your own dependency_request metric using addDependencyEvent method in MonitorMetrics to add a new dependency request event.

### How to Use

Import the following dependency to your project(**check the latest version in the Maven badge at the beginning of this README**):

#### Maven

```xml
<dependency>
  <groupId>br.com.labbs</groupId>
  <artifactId>quarkus-monitor</artifactId>
  <version>${version}</version>
</dependency>
```

### How to config

These are properties that you can use to config your b5 quarkus monitor, all properties will start with  `quarkus.b5.monitor`

Property Name            | Description                                               | Default Value
------------------------ | --------------------------------------------------------- | ---------
enable                   | Enable the extension.                                     | true
path                     | Define the path where the metrics are exposed.            | /metrics
exclusions               | List of paths comma-separated where metrics do not apply | /metrics
enable-http-response-size| Enable the metric for response size                      | false
buckets                  | Values in seconds for the buckets used in b5 metrics      | 0.1, 0.3, 1.5, 10.5
error-message            | Key for error messages in the header or request attribute | error-info


### How to add error messages in tag
First you need to define the key in error-message in the properties, default value is error-info.
Choose your way, by the header or adding the property in ContainerRequestContext. This extension will look first in the header, 
if it is not found it will look in the properties of ContainerRequestContext.
The errors messages will be added only when the status code is greater than or equal 400.

#### Adding in the header
Add in the response header the same key used in error-message, or use error-info, then put your string content for the error.

#### Adding in ContainerRequestContext
Add in the setProperty the same key used in error-message, or use error-info, then put your string content for the error.


### How to Implement

#### Request

If you create your endpoint using JAX-RS, this extension will create request metrics automatically.
To add your own request metric event you can do like this example:

```java 
  RequestEvent requestEvent = new RequestEvent()
    .setType("gprc")
    .setStatus("OK")
    .setMethod("GET")
    .setAddress("myAddress")
    .setIsError(true)
    .setErrorMessage("my error message");
    
  Instant start = Instant.now();
  
  your_method();  
  double timeElapsed = MonitorMetrics.calcTimeElapsedInSeconds(start);
  MonitorMetrics.INSTANCE.addRequestEvent(requestEvent, timeElapsed);
```

##### Dependency

For Dependency metrics ,Dependency up and Dependency Request, if you are using the Rest Client from 
JAX-RS, the value of tag name will be extract from annotation @Name from java.inject, if present, 
or canonical name of interface used in Rest Client.

##### Dependency Up

If you are using the Rest Client from JAX-RS, this extension will create the dependency_up metric automatically, but to create your own checker you can do it like this example:

```java 

MonitorMetrics.INSTANCE.addDependencyChecker("myChecker", () -> {
  // your implementation for state of dependency  
  return DependencyState.UP;
  }, 1, TimeUnit.SECONDS);

```

If you need to cancel or checker you can cancel one or all checker with the methods cancelDependencyChecker, cancelAllDependencyCheckers.

```java 
MonitorMetrics.INSTANCE.cancelDependencyChecker("myChecker");
```

```java 
MonitorMetrics.INSTANCE.cancelAllDependencyCheckers();
```

To know all checkers are active, you can call listOfCheckersScheduled.

```java 
var list = MonitorMetrics.INSTANCE.listOfCheckersScheduled();
```

##### Dependency Request

If you are using the Rest Client from JAX-RS, this extension will create the dependency_request metric automatically, but to create your own dependency request you can do it like this example:

```java 
 DependencyEvent dependencyEvent = new DependencyEvent("dependency_event")
        .setType("other")
        .setStatus("OK")
        .setMethod("GET")
        .setAddress("myAddress")
        .setIsError(true)
        .setErrorMessage("my error message");
    
  Instant start = Instant.now();
  
  your_dependecy method();  
  
  double timeElapsed = MonitorMetrics.calcTimeElapsedInSeconds(start);
  MonitorMetrics.INSTANCE.addDependencyEvent(dependencyEvent, timeElapsed);
```
