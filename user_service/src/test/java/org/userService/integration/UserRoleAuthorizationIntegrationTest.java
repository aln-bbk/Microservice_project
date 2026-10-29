package org.userService.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.userService.entity.User;
import org.userService.repository.UserRepository;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserRoleAuthorizationIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  UserRepository userRepository;

  private User user1;
  private User user2;

  @BeforeEach
  void setUp() {
    user1 = createUser("Alice", "Owner");
    user2 = createUser("Bob", "Other");
  }

  @Test
  void userCanGetOwnProfile() throws Exception {
    mockMvc.perform(get("/api/users/{id}", user1.getId())
            .with(userJwt(user1.getId())))
        .andExpect(status().isOk());
  }

  @Test
  void userCannotGetAnotherUsersProfile() throws Exception {
    mockMvc.perform(get("/api/users/{id}", user2.getId())
            .with(userJwt(user1.getId())))
        .andExpect(status().isForbidden());
  }

  @Test
  void userCanUpdateOwnProfile() throws Exception {
    mockMvc.perform(put("/api/users/{id}", user1.getId())
            .with(userJwt(user1.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson(
                "Alice Updated",
                "Owner",
                uniqueEmail()
            )))
        .andExpect(status().isOk());
  }

  @Test
  void userCannotUpdateAnotherUsersProfile() throws Exception {
    mockMvc.perform(put("/api/users/{id}", user2.getId())
            .with(userJwt(user1.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson(
                "Changed",
                "Other",
                uniqueEmail()
            )))
        .andExpect(status().isForbidden());
  }

  @Test
  void userCannotActivateUser() throws Exception {
    mockMvc.perform(patch("/api/users/{id}/activate", user2.getId())
            .with(userJwt(user1.getId())))
        .andExpect(status().isForbidden());
  }

  @Test
  void userCannotDeactivateUser() throws Exception {
    mockMvc.perform(patch("/api/users/{id}/deactivate", user2.getId())
            .with(userJwt(user1.getId())))
        .andExpect(status().isForbidden());
  }

  @Test
  void userCannotDeleteUser() throws Exception {
    mockMvc.perform(delete("/api/users/{id}", user2.getId())
            .with(userJwt(user1.getId())))
        .andExpect(status().isForbidden());
  }

  @Test
  void userCannotGetActiveUsers() throws Exception {
    mockMvc.perform(get("/api/users/active")
            .param("name", "Alice")
            .with(userJwt(user1.getId())))
        .andExpect(status().isForbidden());
  }

  @Test
  void userCannotGetUsersWithoutCards() throws Exception {
    mockMvc.perform(get("/api/users/without-cards")
            .with(userJwt(user1.getId())))
        .andExpect(status().isForbidden());
  }

  @Test
  void userCannotSearchUserByEmail() throws Exception {
    mockMvc.perform(get("/api/users/by-email")
            .param("email", user2.getEmail())
            .with(userJwt(user1.getId())))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanGetAnotherUsersProfile() throws Exception {
    mockMvc.perform(get("/api/users/{id}", user2.getId())
            .with(adminJwt()))
        .andExpect(status().isOk());
  }

  @Test
  void adminCanListUsers() throws Exception {
    mockMvc.perform(get("/api/users")
            .with(adminJwt()))
        .andExpect(status().isOk());
  }

  @Test
  void adminCanGetUsersWithoutCards() throws Exception {
    mockMvc.perform(get("/api/users/without-cards")
            .with(adminJwt()))
        .andExpect(status().isOk());
  }

  private RequestPostProcessor userJwt(Long userId) {
    return jwt()
        .jwt(token -> token
            .claim("userId", userId)
            .claim("role", "USER")
            .claim("tokenType", "access"))
        .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  private RequestPostProcessor adminJwt() {
    return jwt()
        .jwt(token -> token
            .claim("userId", user1.getId())
            .claim("role", "ADMIN")
            .claim("tokenType", "access"))
        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  private User createUser(String name, String surname) {
    User user = new User();
    user.setName(name);
    user.setSurname(surname);
    user.setEmail(uniqueEmail());
    user.setBirthDate(LocalDate.of(1995, 1, 1));
    user.setActive(true);

    return userRepository.saveAndFlush(user);
  }

  private String uniqueEmail() {
    return "role-test-" + UUID.randomUUID() + "@example.com";
  }

  private String userJson(String name, String surname, String email) {
    return """
            {
              "name": "%s",
              "surname": "%s",
              "birthDate": "1995-01-01",
              "email": "%s"
            }
            """.formatted(name, surname, email);
  }
}
