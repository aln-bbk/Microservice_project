package org.course_project.order_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/order_test",
    "spring.datasource.username=test",
    "spring.datasource.password=test",
    "user-service.url=http://localhost:8080"
})
class OrderServiceApplicationTests {

  @Test
  void contextLoads() {
  }

}
