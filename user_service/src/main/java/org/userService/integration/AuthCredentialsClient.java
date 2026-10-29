package org.userService.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.userService.exception.AuthServiceIntegrationException;

/** Calls the private credentials endpoint of Auth Service. */
@Component
public class AuthCredentialsClient {

  private final RestClient restClient;
  private final String internalApiKey;

  public AuthCredentialsClient(
      RestClient.Builder builder,
      @Value("${app.auth-service.base-url:http://localhost:8081}") String baseUrl,
      @Value("${app.internal-api.key:}") String internalApiKey
  ) {
    this.restClient = builder.baseUrl(baseUrl).build();
    this.internalApiKey = internalApiKey;
  }

  public void createCredentials(Long userId, String login, String password) {
    if (internalApiKey == null || internalApiKey.isBlank()) {
      throw new IllegalStateException(
          "Internal API key is not configured for Auth Service integration"
      );
    }

    restClient.post()
        .uri("/internal/credentials")
        .header("X-Internal-Api-Key", internalApiKey)
        .body(new CreateCredentialsPayload(userId, login, password))
        .retrieve()
        .onStatus(HttpStatusCode::isError, (request, response) -> {
          throw new AuthServiceIntegrationException(
              "Auth Service rejected credential creation with HTTP "
                  + response.getStatusCode().value()
          );
        })
        .toBodilessEntity();
  }

  private record CreateCredentialsPayload(Long userId, String login, String password) {}
}
