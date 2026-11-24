package org.userService.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentCardSecurityIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void userCannotListAllCards() throws Exception {
    mockMvc.perform(get("/api/cards")
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
  void adminCanListAllCards() throws Exception {
    mockMvc.perform(get("/api/cards")
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
  void userCannotGetCardsOfAnotherUser() throws Exception {
    mockMvc.perform(get("/api/cards/user/2")
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
  void requestWithoutTokenCannotGetCard() throws Exception {
    mockMvc.perform(get("/api/cards/1"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void userCannotGetAnotherUsersCard() throws Exception {
    mockMvc.perform(get("/api/cards/1")
            .with(jwt()
                .jwt(token -> token
                    .claim("userId", 2L)
                    .claim("role", "USER"))
                .authorities(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_USER"
                    )
                )))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanGetAnotherUsersCard() throws Exception {
    mockMvc.perform(get("/api/cards/1")
            .with(jwt()
                .jwt(token -> token
                    .claim("userId", 99L)
                    .claim("role", "ADMIN"))
                .authorities(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_ADMIN"
                    )
                )))
        .andExpect(status().isNotFound());
  }

}
