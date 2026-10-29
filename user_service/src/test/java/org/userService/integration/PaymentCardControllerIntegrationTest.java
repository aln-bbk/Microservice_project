package org.userService.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.userService.entity.User;
import org.userService.repository.PaymentCardRepository;
import org.userService.repository.UserRepository;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentCardControllerIntegrationTest
    extends AbstractIntegrationTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  UserRepository userRepository;

  @Autowired
  PaymentCardRepository cardRepository;

  @Test
  void createReadUpdateAndDeleteCardThroughHttp() throws Exception {
    User user = createUser();

    String number = uniqueCardNumber();
    Long cardId = createCard(
        user.getId(), number, "ANNA IVANOVA"
    );

    mockMvc.perform(get("/api/cards/{id}", cardId)
            .with(userJwt(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(number));

    String updatedNumber = uniqueCardNumber();

    mockMvc.perform(put("/api/cards/{id}", cardId)
            .with(userJwt(user.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardUpdateJson(updatedNumber, "ANNA I.")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(updatedNumber))
        .andExpect(jsonPath("$.holder").value("ANNA I."));

    mockMvc.perform(delete("/api/cards/{id}", cardId)
            .with(userJwt(user.getId())))
        .andExpect(status().isNoContent());

    assertThat(cardRepository.findById(cardId)).isEmpty();
  }

  @Test
  void invalidCardNumberReturnsBadRequest() throws Exception {
    User user = createUser();

    mockMvc.perform(post("/api/cards")
            .with(userJwt(user.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(
                user.getId(), "123", "ANNA IVANOVA"
            )))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void sixthCardForUserReturnsConflictAndDoesNotPersist()
      throws Exception {

    User user = createUser();

    for (int i = 0; i < 5; i++) {
      createCard(
          user.getId(),
          uniqueCardNumber(),
          "ANNA IVANOVA"
      );
    }

    mockMvc.perform(post("/api/cards")
            .with(userJwt(user.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(
                user.getId(),
                uniqueCardNumber(),
                "ANNA IVANOVA"
            )))
        .andExpect(status().isConflict());

    assertThat(cardRepository.countByUserId(user.getId()))
        .isEqualTo(5);
  }

  @Test
  void cardForMissingUserReturnsNotFound() throws Exception {
    Long missingUserId = 999999999L;

    mockMvc.perform(post("/api/cards")
            .with(userJwt(missingUserId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(
                missingUserId,
                uniqueCardNumber(),
                "ANNA IVANOVA"
            )))
        .andExpect(status().isNotFound());
  }

  private RequestPostProcessor userJwt(Long userId) {
    return jwt()
        .jwt(token -> token
            .claim("userId", userId)
            .claim("role", "USER")
            .claim("tokenType", "access"))
        .authorities(
            new SimpleGrantedAuthority("ROLE_USER")
        );
  }

  private User createUser() {
    User user = new User();
    user.setName("Anna");
    user.setSurname("Ivanova");
    user.setBirthDate(LocalDate.of(2000, 1, 1));
    user.setEmail(
        "card-test-" + UUID.randomUUID() + "@example.com"
    );

    return userRepository.saveAndFlush(user);
  }

  private Long createCard(
      Long userId,
      String number,
      String holder
  ) throws Exception {

    mockMvc.perform(post("/api/cards")
            .with(userJwt(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(userId, number, holder)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber());

    return cardRepository.findAllByUserId(userId).stream()
        .filter(card -> card.getNumber().equals(number))
        .findFirst()
        .orElseThrow()
        .getId();
  }

  private String cardUpdateJson(String number, String holder) {
    return """
            {
              "number": "%s",
              "holder": "%s",
              "expirationDate": "2030-12-31"
            }
            """.formatted(number, holder);
  }

  private String cardJson(
      Long userId,
      String number,
      String holder
  ) {
    return """
            {
              "userId": %d,
              "number": "%s",
              "holder": "%s",
              "expirationDate": "2030-12-31"
            }
            """.formatted(userId, number, holder);
  }

  private String uniqueCardNumber() {
    return "4" + String.format(
        "%015d",
        Math.abs(UUID.randomUUID().getLeastSignificantBits())
            % 1_000_000_000_000_000L
    );
  }
}