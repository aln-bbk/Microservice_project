package org.userService.repository;

import org.userService.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {

  List<PaymentCard> findAllByUserId(Long userId);

  long countByUserId(Long userId);

  boolean existsByIdAndUser_Id(Long id, Long userId);
}
