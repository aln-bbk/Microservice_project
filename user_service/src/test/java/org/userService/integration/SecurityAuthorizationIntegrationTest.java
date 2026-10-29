package org.userService.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  MockMvc mockMvc;

  @Test
  void requestWithoutTokenReturns401() throws Exception {
    mockMvc.perform(get("/api/users"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void adminCanListUsers() throws Exception {
    mockMvc.perform(get("/api/users")
            .with(jwt()
                .jwt(token -> token
                    .claim("userId", 1L)
                    .claim("role", "ADMIN"))
                .authorities(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_ADMIN"
                    )
                )))
        .andExpect(status().isOk());
  }

  @Test
  void userCannotListAllUsers() throws Exception {
    mockMvc.perform(get("/api/users")
            .with(jwt()
                .jwt(token -> token
                    .claim("userId", 1L)
                    .claim("role", "USER"))
                .authorities(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_USER"
                    )
                )))
        .andExpect(status().isForbidden());
  }

  @Test
  void userCannotCreateUser() throws Exception {
    mockMvc.perform(post("/api/users")
            .with(jwt()
                .jwt(token -> token
                    .claim("userId", 1L)
                    .claim("role", "USER"))
                .authorities(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_USER"
                    )
                ))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                                {
                                  "name": "Test",
                                  "surname": "User",
                                  "birthDate": "2000-01-01",
                                  "email": "security-test@example.com"
                                }
                                """))
        .andExpect(status().isForbidden());
  }
}
