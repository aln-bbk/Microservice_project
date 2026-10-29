package org.course_project.authenticationService;

import org.springframework.boot.SpringApplication;

public class TestAuthenticationServiceApplication {

  public static void main(String[] args) {
    SpringApplication.from(AuthenticationServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
  }

}
