package org.userService.exception;

public class CardNumberAlreadyExistsException extends RuntimeException {
  public CardNumberAlreadyExistsException(String message) {
    super("Payment card number already exists: " + message);
  }
}
