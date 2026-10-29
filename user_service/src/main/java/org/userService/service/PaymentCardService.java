package org.userService.service;

import org.userService.exception.CardLimitExceededException;
import org.userService.exception.ResourceNotFoundException;
import org.userService.dto.card.PaymentCardCreateRequest;
import org.userService.dto.card.PaymentCardResponse;
import org.userService.dto.card.PaymentCardUpdateRequest;
import org.userService.entity.PaymentCard;
import org.userService.entity.User;
import org.userService.mapper.PaymentCardMapper;
import org.userService.repository.PaymentCardRepository;
import org.userService.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

@Service
public class PaymentCardService {

  private static final long MAX_CARDS_PER_USER = 5;

  private final PaymentCardRepository cardRepository;
  private final UserRepository userRepository;
  private final PaymentCardMapper cardMapper;
  private final CacheManager cacheManager;

  public PaymentCardService(
      PaymentCardRepository cardRepository,
      UserRepository userRepository,
      PaymentCardMapper cardMapper,
      CacheManager cacheManager
  ) {
    this.cardRepository = cardRepository;
    this.userRepository = userRepository;
    this.cardMapper = cardMapper;
    this.cacheManager = cacheManager;
  }

  @Transactional
  public PaymentCardResponse create(PaymentCardCreateRequest request) {
    User user = userRepository.findByIdForUpdate(request.userId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "User not found: " + request.userId()
        ));

    if (cardRepository.countByUserId(user.getId()) >= MAX_CARDS_PER_USER) {
      throw new CardLimitExceededException(
          "User cannot have more than 5 payment cards"
      );
    }

    PaymentCard card = cardMapper.toEntity(request);
    card.setUser(user);
    card.setActive(true);

    PaymentCard savedCard = cardRepository.save(card);
    evictUserWithCards(user.getId());

    return cardMapper.toResponse(savedCard);
  }

  @Transactional(readOnly = true)
  public PaymentCardResponse getById(Long id) {
    return cardMapper.toResponse(findCardOrThrow(id));
  }

  @Transactional(readOnly = true)
  public Page<PaymentCardResponse> getAll(Pageable pageable) {
    return cardRepository.findAll(pageable)
        .map(cardMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public List<PaymentCardResponse> getByUserId(Long userId) {
    if (!userRepository.existsById(userId)) {
      throw new ResourceNotFoundException("User not found: " + userId);
    }

    return cardRepository.findAllByUserId(userId)
        .stream()
        .map(cardMapper::toResponse)
        .toList();
  }

  @Transactional
  public PaymentCardResponse update(Long id, PaymentCardUpdateRequest request) {
    PaymentCard card = findCardOrThrow(id);
    cardMapper.updateEntity(request, card);
    cardRepository.flush();
    evictUserWithCards(card.getUser().getId());

    return cardMapper.toResponse(card);
  }

  @Transactional
  public PaymentCardResponse activate(Long id) {
    PaymentCard card = findCardOrThrow(id);
    card.setActive(true);
    cardRepository.flush();
    evictUserWithCards(card.getUser().getId());

    return cardMapper.toResponse(card);
  }

  @Transactional
  public PaymentCardResponse deactivate(Long id) {
    PaymentCard card = findCardOrThrow(id);
    card.setActive(false);
    cardRepository.flush();
    evictUserWithCards(card.getUser().getId());

    return cardMapper.toResponse(card);
  }

  @Transactional
  public void delete(Long id) {
    PaymentCard card = findCardOrThrow(id);
    Long userId = card.getUser().getId();

    cardRepository.delete(card);
    evictUserWithCards(userId);
  }

  private PaymentCard findCardOrThrow(Long id) {
    return cardRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Payment card not found: " + id));
  }

  private void evictUserWithCards(Long userId) {
    Cache cache = cacheManager.getCache("userWithCards");

    if (cache != null) {
      cache.evict(userId);
    }
  }
}