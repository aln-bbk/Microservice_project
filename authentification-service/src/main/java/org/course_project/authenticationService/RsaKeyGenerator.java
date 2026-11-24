package org.course_project.authenticationService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class RsaKeyGenerator {

  public static void main(String[] args) throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);

    KeyPair keyPair = generator.generateKeyPair();

    String privateKey = """
                -----BEGIN PRIVATE KEY-----
                %s
                -----END PRIVATE KEY-----
                """.formatted(
        Base64.getMimeEncoder(64, new byte[]{'\n'})
            .encodeToString(keyPair.getPrivate().getEncoded())
    );

    String publicKey = """
                -----BEGIN PUBLIC KEY-----
                %s
                -----END PUBLIC KEY-----
                """.formatted(
        Base64.getMimeEncoder(64, new byte[]{'\n'})
            .encodeToString(keyPair.getPublic().getEncoded())
    );

    Files.writeString(Path.of("private.pem"), privateKey);
    Files.writeString(Path.of("public.pem"), publicKey);

    System.out.println("RSA keys generated successfully.");
    System.out.println("Private key: private.pem");
    System.out.println("Public key: public.pem");
  }
}
