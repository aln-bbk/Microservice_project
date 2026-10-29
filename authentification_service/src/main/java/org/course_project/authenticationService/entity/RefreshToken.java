package org.course_project.authenticationService.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "refresh_tokens",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_refresh_tokens_token_hash",
            columnNames = "token_hash"
        )
    },
    indexes = {
        @Index(
            name = "idx_refresh_tokens_user_id",
            columnList = "user_id"
        )
    }
)
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "revoked", nullable = false)
  private boolean revoked = false;

  protected RefreshToken() {
  }

  public RefreshToken(
      Long userId,
      String tokenHash,
      OffsetDateTime expiresAt
  ) {
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  @PrePersist
  protected void onCreate() {
    createdAt = OffsetDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public boolean isRevoked() {
    return revoked;
  }

  public void revoke() {
    this.revoked = true;
  }

  public boolean isExpired() {
    return !expiresAt.isAfter(OffsetDateTime.now());
  }
}
