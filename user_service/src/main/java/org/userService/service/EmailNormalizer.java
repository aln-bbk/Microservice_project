package org.userService.service;

import java.util.Locale;

import org.userService.exception.InvalidResourceStateException;

public final class EmailNormalizer {

  private EmailNormalizer() {
  }

  public static String normalize(String email) {
    if (email == null || email.isBlank()) {
      throw new InvalidResourceStateException("Email must not be blank");
    }

    return email.trim().toLowerCase(Locale.ROOT);
  }
}
