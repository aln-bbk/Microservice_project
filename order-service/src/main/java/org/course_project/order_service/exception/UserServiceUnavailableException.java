package org.course_project.order_service.exception;

public class UserServiceUnavailableException extends RuntimeException {

  public UserServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
