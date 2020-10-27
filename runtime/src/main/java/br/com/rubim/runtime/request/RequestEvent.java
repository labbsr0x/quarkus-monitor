package br.com.rubim.runtime.request;

public class RequestEvent {

  private String type;
  private String status;
  private String method;
  private String address;
  private String isError;
  private String errorMessage;

  public RequestEvent() {
    this.type = "";
    this.status = "";
    this.method = "";
    this.address = "";
    this.isError = "false";
    this.errorMessage = "";
  }

  public String getType() {
    return type;
  }

  public RequestEvent setType(String type) {
    this.type = type;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public RequestEvent setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getMethod() {
    return method;
  }

  public RequestEvent setMethod(String method) {
    this.method = method;
    return this;
  }

  public String getAddress() {
    return address;
  }

  public RequestEvent setAddress(String address) {
    this.address = address;
    return this;
  }

  public String getIsError() {
    return isError;
  }

  public RequestEvent setIsError(boolean isError) {
    this.isError = Boolean.toString(isError);
    return this;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public RequestEvent setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }
}
