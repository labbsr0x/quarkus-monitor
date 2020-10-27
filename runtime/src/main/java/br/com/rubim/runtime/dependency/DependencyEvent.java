package br.com.rubim.runtime.dependency;

public class DependencyEvent {

  private String name;
  private String type;
  private String status;
  private String method;
  private String address;
  private String isError;
  private String errorMessage;

  public DependencyEvent(String name) {
    this.name = name;
    this.type = "";
    this.status = "";
    this.method = "";
    this.address = "";
    this.isError = "false";
    this.errorMessage = "";
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public DependencyEvent setType(String type) {
    this.type = type;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public DependencyEvent setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getMethod() {
    return method;
  }

  public DependencyEvent setMethod(String method) {
    this.method = method;
    return this;
  }

  public String getAddress() {
    return address;
  }

  public DependencyEvent setAddress(String address) {
    this.address = address;
    return this;
  }

  public String getIsError() {
    return isError;
  }

  public DependencyEvent setIsError(boolean isError) {
    this.isError = Boolean.toString(isError);
    return this;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public DependencyEvent setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }
}
