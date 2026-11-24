package org.course_project.order_service.service;

import lombok.RequiredArgsConstructor;
import org.course_project.order_service.dto.UserInfoResponse;
import org.course_project.order_service.exception.NotFoundException;
import org.course_project.order_service.exception.UserServiceUnavailableException;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

  private final RestClient userServiceRestClient;
  private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

  public UserInfoResponse getUserByEmail(String email) {

    String token = getCurrentAccessToken();

    return executeWithCircuitBreaker(
        () -> userServiceRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/users/by-email")
                .queryParam("email", email)
                .build())
            .headers(headers -> addAuthorizationHeader(headers, token))
            .retrieve()
            .body(UserInfoResponse.class)
    );
  }

  public List<UserInfoResponse> getUsersByIds(List<Long> ids) {

    String token = getCurrentAccessToken();

    UserIdsRequest request = new UserIdsRequest(ids);

    return executeWithCircuitBreaker(
        () -> userServiceRestClient.post()
            .uri("/api/users/by-ids")
            .headers(headers -> addAuthorizationHeader(headers, token))
            .body(request)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            })
    );
  }

  public UserInfoResponse getUserById(Long id) {

    String token = getCurrentAccessToken();

    return executeWithCircuitBreaker(
        () -> userServiceRestClient.get()
            .uri("/api/users/{id}", id)
            .headers(headers -> addAuthorizationHeader(headers, token))
            .retrieve()
            .body(UserInfoResponse.class)
    );
  }

  private String getCurrentAccessToken() {

    Authentication authentication =
        SecurityContextHolder.getContext().getAuthentication();

    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      return jwtAuth.getToken().getTokenValue();
    }

    return null;
  }

  private void addAuthorizationHeader(
      HttpHeaders headers,
      String token
  ) {
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
  }

  private <T> T executeWithCircuitBreaker(Supplier<T> action) {

    CircuitBreaker circuitBreaker = circuitBreakerFactory.create("userService");

    return circuitBreaker.run(action, this::handleFallback);
  }

  private <T> T handleFallback(Throwable cause) {

    if (cause instanceof HttpClientErrorException.NotFound) {
      throw new NotFoundException("User not found");
    }

    if (cause instanceof HttpClientErrorException.Unauthorized
        || cause instanceof HttpClientErrorException.Forbidden) {

      throw new org.springframework.security.access.AccessDeniedException(
          "User Service rejected the request"
      );
    }

    if (cause instanceof NotFoundException ex) {
      throw ex;
    }

    throw new UserServiceUnavailableException(
        "User Service is currently unavailable",
        cause
    );
  }

  public record UserIdsRequest(List<Long> userIds) {}
}