package org.course_project.authenticationService.config;

import org.course_project.authenticationService.entity.Credential;
import org.course_project.authenticationService.entity.Role;
import org.course_project.authenticationService.repository.CredentialRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminRunner implements CommandLineRunner {

  private final CredentialRepository credentialRepository;
  private final PasswordEncoder passwordEncoder;
  private final Environment environment;

  public BootstrapAdminRunner(
      CredentialRepository credentialRepository,
      PasswordEncoder passwordEncoder,
      Environment environment
  ) {
    this.credentialRepository = credentialRepository;
    this.passwordEncoder = passwordEncoder;
    this.environment = environment;
  }

  @Override
  public void run(String... args) {
    boolean enabled = environment.getProperty(
        "app.bootstrap-admin.enabled",
        Boolean.class,
        false
    );

    if (!enabled) {
      return;
    }

    String login = environment.getProperty("app.bootstrap-admin.login");
    String password = environment.getProperty("app.bootstrap-admin.password");
    String userIdValue = environment.getProperty("app.bootstrap-admin.user-id");

    if (login == null || login.isBlank()
        || password == null || password.isBlank()
        || userIdValue == null || userIdValue.isBlank()) {
      throw new IllegalStateException(
          "Bootstrap admin is enabled, but login, password or user-id is missing"
      );
    }

    if (password.length() < 12) {
      throw new IllegalStateException(
          "Bootstrap admin password must contain at least 12 characters"
      );
    }

    long userId;
    try {
      userId = Long.parseLong(userIdValue);
    } catch (NumberFormatException exception) {
      throw new IllegalStateException(
          "Bootstrap admin user-id must be a number",
          exception
      );
    }

    var existingAdmin = credentialRepository.findByLogin(login);
    if (existingAdmin.isPresent()) {
      Credential existing = existingAdmin.get();
      if (existing.getRole() != Role.ADMIN || existing.getUserId() != userId) {
        throw new IllegalStateException(
            "Bootstrap admin login already exists with a different role or user-id"
        );
      }
      return;
    }

    if (credentialRepository.existsByUserId(userId)) {
      throw new IllegalStateException(
          "Credentials already exist for the configured admin user-id"
      );
    }

    Credential admin = new Credential(
        userId,
        login,
        passwordEncoder.encode(password),
        Role.ADMIN
    );

    credentialRepository.save(admin);
  }
}
