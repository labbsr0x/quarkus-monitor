## Extension to add BB Metrics to Quarkus Projects

This projects implements the [Big Brother](https://github.com/labbsr0x/big-brother) Metrics specfication through a [Quarkus](https://quarkus.io) extension.

### Metrics 

Besides this extension use MicroProfile metrics, the metrics created for b5 will be without prefix, as follows:

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

1. The `request_seconds_bucket` metric defines the histogram of how many requests are falling into the well-defined buckets represented by the label `le`;

2. The `request_seconds_count` is a counter that counts the overall number of requests with those exact label occurrences;

3. The `request_seconds_sum` is a counter that counts the overall sum of how long the requests with those exact label occurrences are taking;

4. The `response_size_bytes` is a counter that computes how much data is being sent back to the user for a given request type. 
This metric is disable by default for performance.

5. The `dependency_up` is a metric to register whether a specific dependency is up (1) or down (0). The label `name` registers the dependency name;

6. The `dependency_request_seconds_bucket` is a metric that defines the histogram of how many requests to a specific dependency are falling into the well defined buckets represented by the label le;

7. The `dependency_request_seconds_count` is a counter that counts the overall number of requests to a specific dependency;

8. The `dependency_request_seconds_sum` is a counter that counts the overall sum of how long requests to a specific dependency are taking;

9. The `application_info` holds static info of an application, such as it's semantic version number;

Labels:

1. `type` tells which request protocol was used (e.g. `grpc` or `http`);

2. `status` registers the response status (e.g. HTTP status code);

3. `method` registers the request method;

4. `addr` registers the requested endpoint address, if path has path params it will be replaced by the key between {};

5. `version` tells which version of your app handled the request;

6. `isError` lets us know if the status code reported is an error or not;

7. `errorMessage` registers the error message, passed by headers or property in ContainerRequestContext or;

8. `name` registers the name of the dependency;



#### How to importing dependency

Import the following dependency to your project:

##### Maven

```xml
<dependency>
  <groupId>io.github.marcelorubim</groupId>
  <artifactId>quarkus-monitor</artifactId>
  <version>0.0.9</version>
</dependency>
```

#### How to config

Properties that you can use to config your b5 quarkus monitor, all metrics will start with  `quarkus.b5.monitor`

Property Name            | Description                                               | Default Value
------------------------ | --------------------------------------------------------- | ---------
enable                   | Enable the extension.                                     | true
path                     | Define the path where the metrics are exposed.            | /metrics
exclusions               | List of paths comma-separated where metrics are not apply | /metrics
enable-http-response-size| Enabled the metric for response size                      | false
buckets                  | Values in seconds for the buckets used in b5 metrics      | 0.1, 0.3, 1.5, 10.5
error-message            | Key for error messages in the header or request attribute | error-info


#### How to add error messages in tag

First you need to define the key in error-message in properties, default value is error-info.
Choose your way, by header or adding property in ContainerRequestContext. The extesion will look first in header, 
if it not found it will look in the properties of ContainerRequestContext.
The errors messages it will be added only when the status code is greater than or equal 400.

##### Adding in header
Add in the response header the same key used in error-message, or use error-info , then put your string content for the error.

##### Adding in ContainerRequestContext
Add in the setProperty the same key used in error-message, or use error-info , then put your string content for the error.


#### How it works

##### Path

The paths will be add in tag with path parameter key between {}, for example:

```
    ...
    @GET
    @Path("/user/{id}")
    public Response findUserById(final @PathParam("id") long idUser){
  
    }
    ...
```

Will be add the following addr tag in the metric

```
request_seconds_count{type="http",status="200",method="GET",addr="/user/{id}",isError="false",errorMessage="",} 1.0
```

For exclusions the same logic will apply, if you don`t want to generate metrics for the path show above, just add
/user/{id} in the list of the property quarkus.b5.monitor.exclusions.

##### Dependency Metrics

This project defines as Dependency any service called through REST. The dependency metrics are collected 
by a [MicroProfile Rest Client Provider](https://download.eclipse.org/microprofile/microprofile-rest-client-1.2.1/microprofile-rest-client-1.2.1.html#providers) implementation.
The name of the dependency is the interface name which defines the RestClient.