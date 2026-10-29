package org.course_project.authenticationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.internal-api.key=test-internal-api-key"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CredentialsControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void createCredentials_shouldReturnCreated() throws Exception {
    String request = """
                {
                    "userId": 10001,
                    "login": "integration_test_user",
                    "password": "StrongPassword123!"
                }
                """;

    mockMvc.perform(post("/internal/credentials")
            .header("X-Internal-Api-Key", "test-internal-api-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
        .andDo(print())
        .andExpect(status().isCreated());
  }


  @Test
  void createCredentials_withoutInternalApiKey_shouldReturnUnauthorized()
      throws Exception {

    String request = """
        {
            "userId": 10002,
            "login": "missing_key_user",
            "password": "StrongPassword123!"
        }
        """;

    mockMvc.perform(post("/internal/credentials")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createCredentials_withInvalidInternalApiKey_shouldReturnUnauthorized()
      throws Exception {

    String request = """
        {
            "userId": 10003,
            "login": "invalid_key_user",
            "password": "StrongPassword123!"
        }
        """;

    mockMvc.perform(post("/internal/credentials")
            .header("X-Internal-Api-Key", "wrong-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createCredentials_withExistingUserId_shouldReturnConflict() throws Exception {
    String firstRequest = """
        {
            "userId": 918273645,
            "login": "unique_user_id_first",
            "password": "StrongPassword123!"
        }
        """;
    String secondRequest = """
        {
            "userId": 918273645,
            "login": "unique_user_id_second",
            "password": "StrongPassword123!"
        }
        """;

    mockMvc.perform(post("/internal/credentials")
            .header("X-Internal-Api-Key", "test-internal-api-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(firstRequest))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/internal/credentials")
            .header("X-Internal-Api-Key", "test-internal-api-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(secondRequest))
        .andExpect(status().isConflict());
  }

  @Test
  void createCredentials_withExistingLogin_shouldReturnConflict() throws Exception {
    String login = "duplicate_login_user_20261027_a7";

    String firstRequest = """
        {
            "userId": 91827401,
            "login": "%s",
            "password": "StrongPassword123!"
        }
        """.formatted(login);

    String secondRequest = """
        {
            "userId": 91827402,
            "login": "%s",
            "password": "StrongPassword123!"
        }
        """.formatted(login);

    mockMvc.perform(post("/internal/credentials")
            .header("X-Internal-Api-Key", "test-internal-api-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(firstRequest))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/internal/credentials")
            .header("X-Internal-Api-Key", "test-internal-api-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(secondRequest))
        .andExpect(status().isConflict());
  }
}
