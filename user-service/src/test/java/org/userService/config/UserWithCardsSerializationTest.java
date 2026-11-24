package org.userService.config;

import org.userService.dto.user.UserWithCardsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class UserWithCardsSerializationTest {

  @Test
  void shouldSerializeAndDeserializeUserWithCardsResponse() {
    var objectMapper = JsonMapper.builder().build();

    var serializer = new JacksonJsonRedisSerializer<>(
        objectMapper,
        UserWithCardsResponse.class
    );

    String json = """
        {
          "user": {
            "active": true
          },
          "cards": []
        }
        """;

    UserWithCardsResponse original =
        objectMapper.readValue(json, UserWithCardsResponse.class);

    byte[] serialized = serializer.serialize(original);
    UserWithCardsResponse restored = serializer.deserialize(serialized);


    assertNotNull(serialized);
    assertNotNull(restored);
    assertInstanceOf(UserWithCardsResponse.class, restored);
    assertEquals(original, restored);
  }
}
