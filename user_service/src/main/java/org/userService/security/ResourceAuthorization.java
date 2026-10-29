package org.userService.security;

import org.userService.repository.PaymentCardRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("resourceAuthorization")
public class ResourceAuthorization {

  private final PaymentCardRepository cardRepository;

  public ResourceAuthorization(PaymentCardRepository cardRepository) {
    this.cardRepository = cardRepository;
  }

  public boolean isCurrentUser(
      Long requestedUserId,
      Authentication authentication
  ) {
    Long tokenUserId = getTokenUserId(authentication);

    return tokenUserId != null
        && tokenUserId.equals(requestedUserId);
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
}
