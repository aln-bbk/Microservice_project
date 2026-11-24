package org.course_project.order_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtConfig {
  @Bean
  public RSAPublicKey jwtPublicKey(
      @Value("${app.jwt.public-key-location}") Resource resource
  ) throws IOException {
    try (InputStream input = resource.getInputStream()) {
      return RsaKeyConverters.x509().convert(input);
    }
  }
}
