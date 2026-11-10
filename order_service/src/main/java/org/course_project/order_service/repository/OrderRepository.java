package org.course_project.order_service.repository;

import org.course_project.order_service.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OrderRepository extends
    JpaRepository<Order, Long>,
    JpaSpecificationExecutor<Order> {

  Optional<Order> findByIdAndDeletedFalse(Long id);
}
