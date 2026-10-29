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

import org.userService.service.PaymentCardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

  @Mock
  private PaymentCardRepository cardRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private PaymentCardMapper cardMapper;

  @InjectMocks
  private PaymentCardService cardService;

  @Mock
  private CacheManager cacheManager;

  @Mock
  private Cache cache;

  @BeforeEach
  void setUp() {
    lenient()
        .when(cacheManager.getCache("userWithCards"))
        .thenReturn(cache);
  }

  // ---------- CREATE ----------

  @Test
  void create_shouldSaveCard_whenUserHasNoCards() {
    User user = user(1L);
    PaymentCardCreateRequest request = createRequest(1L);

    PaymentCard card = card(10L, user);
    PaymentCardResponse expected = response(card);

    when(userRepository.findByIdForUpdate(1L))
        .thenReturn(Optional.of(user));
    when(cardRepository.countByUserId(1L)).thenReturn(0L);
    when(cardMapper.toEntity(request)).thenReturn(card);
    when(cardRepository.save(card)).thenReturn(card);
    when(cardMapper.toResponse(card)).thenReturn(expected);

    PaymentCardResponse result = cardService.create(request);

    assertEquals(expected, result);
    assertSame(user, card.getUser());
    assertTrue(card.isActive());

    verify(cardRepository).countByUserId(1L);
    verify(cardRepository).save(card);

    verify(cacheManager).getCache("userWithCards");
    verify(cache).evict(1L);
  }

  @Test
  void create_shouldAllowFifthCard_whenUserHasFourCards() {
    User user = user(1L);
    PaymentCardCreateRequest request = createRequest(1L);
    PaymentCard card = card(15L, user);

    when(userRepository.findByIdForUpdate(1L))
        .thenReturn(Optional.of(user));
    when(cardRepository.countByUserId(1L)).thenReturn(4L);
    when(cardMapper.toEntity(request)).thenReturn(card);
    when(cardRepository.save(card)).thenReturn(card);
    when(cardMapper.toResponse(card)).thenReturn(response(card));

    PaymentCardResponse result = cardService.create(request);

    assertNotNull(result);
    verify(cardRepository).save(card);
    verify(cache).evict(1L);
  }

  @Test
  void create_shouldRejectSixthCard_whenUserAlreadyHasFiveCards() {
    User user = user(1L);
    PaymentCardCreateRequest request = createRequest(1L);

    when(userRepository.findByIdForUpdate(1L))
        .thenReturn(Optional.of(user));
    when(cardRepository.countByUserId(1L)).thenReturn(5L);

    assertThrows(
        CardLimitExceededException.class,
        () -> cardService.create(request)
    );

    verify(cardRepository, never()).save(any(PaymentCard.class));
    verify(cardMapper, never())
        .toEntity(any(PaymentCardCreateRequest.class));
    verify(cache, never()).evict(any());
  }

  @Test
  void create_shouldThrow_whenOwnerDoesNotExist() {
    PaymentCardCreateRequest request = createRequest(99L);

    when(userRepository.findByIdForUpdate(99L))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> cardService.create(request)
    );

    verify(cardRepository, never()).countByUserId(anyLong());
    verify(cardRepository, never()).save(any(PaymentCard.class));
  }

  // ---------- GET BY ID ----------

  @Test
  void getById_shouldReturnCard_whenFound() {
    PaymentCard card = card(10L, user(1L));
    PaymentCardResponse expected = response(card);

    when(cardRepository.findById(10L))
        .thenReturn(Optional.of(card));
    when(cardMapper.toResponse(card)).thenReturn(expected);

    assertEquals(expected, cardService.getById(10L));
  }

  @Test
  void getById_shouldThrow_whenCardDoesNotExist() {
    when(cardRepository.findById(99L))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> cardService.getById(99L)
    );

    verify(cardMapper, never()).toResponse(any(PaymentCard.class));
  }

  // ---------- GET ALL / PAGINATION ----------

  @Test
  void getAll_shouldReturnMappedPage() {
    Pageable pageable = PageRequest.of(0, 10);

    PaymentCard first = card(10L, user(1L));
    PaymentCard second = card(11L, user(2L));

    Page<PaymentCard> cards = new PageImpl<>(
        List.of(first, second), pageable, 2
    );

    when(cardRepository.findAll(pageable)).thenReturn(cards);
    when(cardMapper.toResponse(first)).thenReturn(response(first));
    when(cardMapper.toResponse(second)).thenReturn(response(second));

    Page<PaymentCardResponse> result = cardService.getAll(pageable);

    assertEquals(2, result.getTotalElements());
    assertEquals(2, result.getContent().size());
    assertEquals(0, result.getNumber());
    assertEquals(10, result.getSize());
  }

  @Test
  void getAll_shouldReturnEmptyPage_whenNoCardsExist() {
    Pageable pageable = PageRequest.of(0, 10);

    when(cardRepository.findAll(pageable))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    Page<PaymentCardResponse> result = cardService.getAll(pageable);

    assertTrue(result.isEmpty());
    assertEquals(0, result.getTotalElements());
  }

  // ---------- GET BY USER ID ----------

  @Test
  void getByUserId_shouldReturnUsersCards_whenUserExists() {
    PaymentCard first = card(10L, user(1L));
    PaymentCard second = card(11L, user(1L));

    when(userRepository.existsById(1L)).thenReturn(true);
    when(cardRepository.findAllByUserId(1L))
        .thenReturn(List.of(first, second));
    when(cardMapper.toResponse(first)).thenReturn(response(first));
    when(cardMapper.toResponse(second)).thenReturn(response(second));

    List<PaymentCardResponse> result = cardService.getByUserId(1L);

    assertEquals(2, result.size());
    verify(cardRepository).findAllByUserId(1L);
  }

  @Test
  void getByUserId_shouldReturnEmptyList_whenUserHasNoCards() {
    when(userRepository.existsById(1L)).thenReturn(true);
    when(cardRepository.findAllByUserId(1L)).thenReturn(List.of());

    List<PaymentCardResponse> result = cardService.getByUserId(1L);

    assertTrue(result.isEmpty());
  }

  @Test
  void getByUserId_shouldThrow_whenUserDoesNotExist() {
    when(userRepository.existsById(99L)).thenReturn(false);

    assertThrows(
        ResourceNotFoundException.class,
        () -> cardService.getByUserId(99L)
    );

    verify(cardRepository, never()).findAllByUserId(anyLong());
  }

  // ---------- UPDATE ----------

  @Test
  void update_shouldUpdateCard_whenCardExists() {
    User owner = user(1L);
    PaymentCard card = card(10L, owner);

    PaymentCardUpdateRequest request = new PaymentCardUpdateRequest(
        "5555444433332222",
        "ANNA IVANOVA",
        LocalDate.of(2030, 12, 31)
    );

    PaymentCardResponse expected = response(card);

    when(cardRepository.findById(10L))
        .thenReturn(Optional.of(card));
    when(cardMapper.toResponse(card)).thenReturn(expected);

    PaymentCardResponse result = cardService.update(10L, request);

    assertEquals(expected, result);
    verify(cardMapper).updateEntity(request, card);
    verify(cache).evict(1L);
  }

  @Test
  void update_shouldThrow_whenCardDoesNotExist() {
    PaymentCardUpdateRequest request = new PaymentCardUpdateRequest(
        "5555444433332222",
        "ANNA IVANOVA",
        LocalDate.of(2030, 12, 31)
    );

    when(cardRepository.findById(99L))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> cardService.update(99L, request)
    );

    verify(cardMapper, never()).updateEntity(
        any(PaymentCardUpdateRequest.class),
        any(PaymentCard.class)
    );
    verify(cache, never()).evict(any());
  }

  // ---------- ACTIVATE / DEACTIVATE ----------

  @Test
  void activate_shouldSetActiveToTrue() {
    PaymentCard card = card(10L, user(1L));
    card.setActive(false);

    when(cardRepository.findById(10L))
        .thenReturn(Optional.of(card));
    when(cardMapper.toResponse(card)).thenReturn(response(card));

    cardService.activate(10L);

    assertTrue(card.isActive());
    verify(cache).evict(1L);
  }

  @Test
  void deactivate_shouldSetActiveToFalse() {
    PaymentCard card = card(10L, user(1L));

    when(cardRepository.findById(10L))
        .thenReturn(Optional.of(card));
    when(cardMapper.toResponse(card)).thenReturn(response(card));

    cardService.deactivate(10L);

    assertFalse(card.isActive());
    verify(cache).evict(1L);
  }

  @Test
  void activate_shouldThrow_whenCardDoesNotExist() {
    when(cardRepository.findById(99L))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> cardService.activate(99L)
    );
    verify(cache, never()).evict(any());
  }

  // ---------- DELETE ----------

  @Test
  void delete_shouldDeleteCard_whenFound() {
    PaymentCard card = card(10L, user(1L));

    when(cardRepository.findById(10L))
        .thenReturn(Optional.of(card));

    cardService.delete(10L);

    verify(cardRepository).delete(card);
    verify(cache).evict(1L);
  }

  @Test
  void delete_shouldThrow_whenCardDoesNotExist() {
    when(cardRepository.findById(99L))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> cardService.delete(99L)
    );

    verify(cardRepository, never()).delete(any(PaymentCard.class));
    verify(cache, never()).evict(any());
  }

  // ---------- TEST HELPERS ----------

  private User user(Long id) {
    User user = new User();
    user.setId(id);
    user.setName("Anna");
    user.setSurname("Ivanova");
    user.setEmail("anna" + id + "@example.com");
    user.setActive(true);
    return user;
  }

  private PaymentCard card(Long id, User owner) {
    PaymentCard card = new PaymentCard();
    card.setId(id);
    card.setUser(owner);
    card.setNumber("4111111111111111");
    card.setHolder("ANNA IVANOVA");
    card.setExpirationDate(LocalDate.of(2030, 12, 31));
    card.setActive(true);
    return card;
  }

  private PaymentCardCreateRequest createRequest(Long userId) {
    return new PaymentCardCreateRequest(
        userId,
        "4111111111111111",
        "ANNA IVANOVA",
        LocalDate.of(2030, 12, 31)
    );
  }

  private PaymentCardResponse response(PaymentCard card) {
    return new PaymentCardResponse(
        card.getId(),
        card.getUser().getId(),
        card.getNumber(),
        card.getHolder(),
        card.getExpirationDate(),
        card.isActive(),
        null,
        null
    );
  }
}