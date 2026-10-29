package org.userService.repository;

import org.userService.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends
    JpaRepository<User, Long>,
    JpaSpecificationExecutor<User> {


  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  @Query("""
        SELECT u
        FROM User u
        WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))
          AND u.active = true
        """)
  List<User> findActiveByNameContaining(
      @Param("name") String name
  );

  @Query(value = """
        SELECT u.*
        FROM users u
        WHERE NOT EXISTS (
            SELECT 1
            FROM payment_cards pc
            WHERE pc.user_id = u.id
        )
        """, nativeQuery = true)
  List<User> findUsersWithoutCardsNative();


  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.id = :id")
  Optional<User> findByIdForUpdate(@Param("id") Long id);
}
