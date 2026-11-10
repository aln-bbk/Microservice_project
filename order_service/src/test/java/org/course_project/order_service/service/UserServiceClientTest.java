package org.course_project.order_service.service;

import org.course_project.order_service.dto.UserInfoResponse;
import org.course_project.order_service.exception.NotFoundException;
import org.course_project.order_service.exception.UserServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class UserServiceClientTest {

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://user-service");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final CircuitBreakerFactory<?, ?> factory = mock(CircuitBreakerFactory.class);
    private final CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
    private final UserServiceClient client = new UserServiceClient(builder.build(), factory);

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void runCircuitBreakerNormally() {
        when(factory.create("userService")).thenReturn((CircuitBreaker) circuitBreaker);
        when(circuitBreaker.run(any(Supplier.class), any(Function.class))).thenAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(0);
            Function<Throwable, ?> fallback = invocation.getArgument(1);
            try {
                return action.get();
            } catch (Throwable error) {
                return fallback.apply(error);
            }
        });
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserByIdSendsRequestAndForwardsJwt() {
        runCircuitBreakerNormally();
        Jwt jwt = Jwt.withTokenValue("signed-access-token")
                .header("alg", "none")
                .claim("sub", "user")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        server.expect(requestTo("http://user-service/api/users/42"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer signed-access-token"))
                .andRespond(withSuccess("""
                        {"id":42,"email":"user@example.com","name":"Test","surname":"User"}
                        """, MediaType.APPLICATION_JSON));

        UserInfoResponse result = client.getUserById(42L);

        assertEquals(42L, result.id());
        assertEquals("user@example.com", result.email());
        server.verify();
    }

    @Test
    void getUserByIdMaps404ToNotFound() {
        runCircuitBreakerNormally();
        server.expect(requestTo("http://user-service/api/users/404"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThrows(NotFoundException.class, () -> client.getUserById(404L));
        server.verify();
    }

    @Test
    void getUserByIdMapsServerErrorToUnavailable() {
        runCircuitBreakerNormally();
        server.expect(requestTo("http://user-service/api/users/42"))
                .andRespond(withServerError());

        assertThrows(UserServiceUnavailableException.class, () -> client.getUserById(42L));
        server.verify();
    }
}
