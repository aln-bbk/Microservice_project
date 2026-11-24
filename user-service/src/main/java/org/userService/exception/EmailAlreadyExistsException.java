package org.userService.exception;

public class EmailAlreadyExistsException extends RuntimeException {
  public EmailAlreadyExistsException(String message) {
    super("User with email already exists: " + message);
  }
}
