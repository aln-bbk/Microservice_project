package org.userService.security;

import org.userService.repository.PaymentCardRepository;
import org.userService.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("resourceAuthorization")
public class ResourceAuthorization {

  private final PaymentCardRepository cardRepository;
  private final UserRepository userRepository;

  public ResourceAuthorization(
      PaymentCardRepository cardRepository,
      UserRepository userRepository
  ) {
    this.cardRepository = cardRepository;
    this.userRepository = userRepository;
  }

  public boolean isCurrentUser(
      Long requestedUserId,
      Authentication authentication
  ) {
    Long tokenUserId = getTokenUserId(authentication);

    return tokenUserId != null
        && tokenUserId.equals(requestedUserId);
  }

  public boolean isCurrentUserEmail(
      String requestedEmail,
      Authentication authentication
  ) {
    if (requestedEmail == null || requestedEmail.isBlank()) {
      return false;
    }

    Long tokenUserId = getTokenUserId(authentication);
    if (tokenUserId == null) {
      return false;
    }

    return userRepository.findById(tokenUserId)
        .map(user -> user.getEmail().equalsIgnoreCase(requestedEmail.trim()))
        .orElse(false);
  }

  public boolean ownsCard(
      Long cardId,
      Authentication authentication
  ) {
    Long tokenUserId = getTokenUserId(authentication);

    return tokenUserId != null
        && cardRepository.existsByIdAndUser_Id(cardId, tokenUserId);
  }

  private Long getTokenUserId(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      return null;
    }

    Object userIdClaim = jwtAuth.getToken().getClaim("userId");

    if (userIdClaim == null) {
      return null;
    }

    try {
      return Long.valueOf(userIdClaim.toString());
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  public boolean areCurrentUsers(
      java.util.List<Long> requestedUserIds,
      Authentication authentication
  ) {
    Long tokenUserId = getTokenUserId(authentication);

    return tokenUserId != null
        && requestedUserIds != null
        && !requestedUserIds.isEmpty()
        && requestedUserIds.stream()
        .allMatch(tokenUserId::equals);
  }
}
