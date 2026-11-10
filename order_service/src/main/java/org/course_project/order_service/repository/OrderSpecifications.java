package org.course_project.order_service.repository;

import org.course_project.order_service.entity.Order;
import org.course_project.order_service.entity.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Set;

public final class OrderSpecifications {

  private OrderSpecifications() {
  }

  public static Specification<Order> notDeleted() {
    return (root, query, cb) ->
        cb.isFalse(root.get("deleted"));
  }

  public static Specification<Order> createdFrom(Instant from) {
    return (root, query, cb) ->
        from == null ? null :
            cb.greaterThanOrEqualTo(
                root.get("createdAt"), from
            );
  }

  public static Specification<Order> createdTo(Instant to) {
    return (root, query, cb) ->
        to == null ? null :
            cb.lessThanOrEqualTo(
                root.get("createdAt"), to
            );
  }

  public static Specification<Order> hasStatuses(
      Set<OrderStatus> statuses
  ) {
    return (root, query, cb) ->
        statuses == null || statuses.isEmpty()
            ? null
            : root.get("status").in(statuses);
  }

  public static Specification<Order> hasUserId(Long userId) {
    return (root, query, cb) ->
        cb.equal(root.get("userId"), userId);
  }
}
