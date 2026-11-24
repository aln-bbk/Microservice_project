package org.course_project.order_service.security;

import lombok.RequiredArgsConstructor;
import org.course_project.order_service.repository.OrderRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("resourceAuthorization")
@RequiredArgsConstructor
public class ResourceAuthorization {
  private final OrderRepository orderRepository;

  public boolean ownsOrder(Long orderId, Authentication authentication) {
    Long tokenUserId = getUserId(authentication);
    return tokenUserId != null && orderRepository.findByIdAndDeletedFalse(orderId)
        .map(order -> tokenUserId.equals(order.getUserId())).orElse(false);
  }

  public boolean isCurrentUser(Long requestedUserId, Authentication authentication) {
    Long tokenUserId = getUserId(authentication);
    return tokenUserId != null && tokenUserId.equals(requestedUserId);
  }

  public boolean isCurrentUserEmail(String requestedEmail, Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)
        || requestedEmail == null) return false;
    String tokenEmail = jwtAuth.getToken().getClaimAsString("email");
    return tokenEmail != null && tokenEmail.equalsIgnoreCase(requestedEmail.trim());
  }

  private Long getUserId(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) return null;
    Object value = jwtAuth.getToken().getClaim("userId");
    if (value == null) return null;
    try { return Long.valueOf(value.toString()); }
    catch (NumberFormatException ex) { return null; }
  }
}
