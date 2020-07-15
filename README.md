## Extension to add BB Metrics to Quarkus Projects
```
request_seconds_bucket{type, status, isError, errorMessage, method, addr, le}
request_seconds_count{type, status, isError, errorMessage, method, addr}
request_seconds_sum{type, status, isError, errorMessage, method, addr}
response_size_bytes{type, status, isError, errorMessage, method, addr}
dependency_up{name}
dependency_request_seconds_bucket{name, type, status, isError, errorMessage, method, addr, le}
dependency_request_seconds_count{name, type, status, isError, errorMessage, method, add}
dependency_request_seconds_sum{name, type, status, isError, errorMessage, method, add}
application_info{version}
```