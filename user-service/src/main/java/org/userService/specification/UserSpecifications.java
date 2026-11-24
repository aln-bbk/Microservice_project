package org.userService.specification;

import org.userService.entity.User;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UserSpecifications {

  private UserSpecifications() {}

  public static Specification<User> filterByNameAndSurname(String name, String surname) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (name != null && !name.isBlank()) {
        predicates.add(
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get("name")),
                "%" + name.trim().toLowerCase(Locale.ROOT) + "%"
            )
        );
      }

      if (surname != null && !surname.isBlank()) {
        predicates.add(
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get("surname")),
                "%" + surname.trim().toLowerCase(Locale.ROOT) + "%"
            )
        );
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
