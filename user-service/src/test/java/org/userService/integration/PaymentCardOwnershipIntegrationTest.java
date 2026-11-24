package org.userService.integration;
import org.springframework.boot.test.context.SpringBootTest;
import org.userService.entity.PaymentCard;
import org.userService.entity.User;
import org.userService.repository.PaymentCardRepository;
import org.userService.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
@SpringBootTest
@AutoConfigureMockMvc
public class PaymentCardOwnershipIntegrationTest extends AbstractIntegrationTest{

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PaymentCardRepository cardRepository;

  private User user1;
  private User user2;
  private PaymentCard card;

  @BeforeEach
  void setUp() {
    cardRepository.deleteAll();
    userRepository.deleteAll();

    user1 = createUser("Alice", "Owner", "alice.security@test.com");
    user2 = createUser("Bob", "Other", "bob.security@test.com");

    card = new PaymentCard();
    card.setUser(user1);
    card.setNumber("4111111111111111");
    card.setHolder("ALICE OWNER");
    card.setExpirationDate(LocalDate.now().plusYears(2));
    card.setActive(true);

    card = cardRepository.saveAndFlush(card);
  }

  private User createUser(String name, String surname, String email) {
    User user = new User();
    user.setName(name);
    user.setSurname(surname);
    user.setEmail(email);
    user.setBirthDate(LocalDate.of(1995, 1, 1));
    user.setActive(true);
    return userRepository.saveAndFlush(user);
  }

  @Test
  void ownerCanGetOwnCard() throws Exception {
    mockMvc.perform(get("/api/cards/{id}", card.getId())
            .with(jwt().jwt(token -> token
                    .claim("userId", user1.getId())
                    .claim("role", "USER"))
                .authorities(() -> "ROLE_USER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(card.getId().intValue())));
  }

  @Test
  void anotherUserCannotGetCard() throws Exception {
    mockMvc.perform(get("/api/cards/{id}", card.getId())
            .with(jwt().jwt(token -> token
                    .claim("userId", user2.getId())
                    .claim("role", "USER"))
                .authorities(() -> "ROLE_USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanGetAnotherUsersCard() throws Exception {
    mockMvc.perform(get("/api/cards/{id}", card.getId())
            .with(jwt().jwt(token -> token
                    .claim("userId", user2.getId())
                    .claim("role", "ADMIN"))
                .authorities(() -> "ROLE_ADMIN")))
        .andExpect(status().isOk());
  }

  @Test
  void anotherUserCannotUpdateCard() throws Exception {
    String body = """
            {
              "number": "5555555555554444",
              "holder": "BOB OTHER",
              "expirationDate": "%s"
            }
            """.formatted(LocalDate.now().plusYears(3));

    mockMvc.perform(put("/api/cards/{id}", card.getId())
            .with(jwt().jwt(token -> token
                    .claim("userId", user2.getId())
                    .claim("role", "USER"))
                .authorities(() -> "ROLE_USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void anotherUserCannotActivateCard() throws Exception {
    mockMvc.perform(patch("/api/cards/{id}/activate", card.getId())
            .with(jwt().jwt(token -> token
                    .claim("userId", user2.getId())
                    .claim("role", "USER"))
                .authorities(() -> "ROLE_USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void anotherUserCannotDeactivateCard() throws Exception {
    mockMvc.perform(patch("/api/cards/{id}/deactivate", card.getId())
            .with(jwt().jwt(token -> token
                    .claim("userId", user2.getId())
                    .claim("role", "USER"))
                .authorities(() -> "ROLE_USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void anotherUserCannotDeleteCard() throws Exception {
    mockMvc.perform(delete("/api/cards/{id}", card.getId())
            .with(jwt().jwt(token -> token
                    .claim("userId", user2.getId())
                    .claim("role", "USER"))
                .authorities(() -> "ROLE_USER")))
        .andExpect(status().isForbidden());
  }
}