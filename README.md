## Extension to add BB Metrics to Quarkus Projects

This projects implements the [Big Brother](https://github.com/labbsr0x/big-brother) Metrics specfication through a [Quarkus](quarkus.io/) extension.

As Quarkus follows the MicroProfile Metrics specifications, some changes have to be made in the metrics name.

All BB Metrics will have the application prefix.

```
application_request_seconds_bucket{type, status, isError, errorMessage, method, addr, le}
application_request_seconds_count{type, status, isError, errorMessage, method, addr}
application_request_seconds_sum{type, status, isError, errorMessage, method, addr}
application_response_size_bytes{type, status, isError, errorMessage, method, addr}
application_dependency_up{name}
application_dependency_request_seconds_bucket{name, type, status, isError, errorMessage, method, addr, le}
application_dependency_request_seconds_count{name, type, status, isError, errorMessage, method, add}
application_dependency_request_seconds_sum{name, type, status, isError, errorMessage, method, add}
application_info{version}
```

##### Dependency Metrics

This project defines as Dependency any service called through REST. The dependency metrics are collected by a [MicroProfile Rest Client Provider](https://download.eclipse.org/microprofile/microprofile-rest-client-1.2.1/microprofile-rest-client-1.2.1.html#providers) implementation.
The name of the dependency is the interface name which defines the RestClient.