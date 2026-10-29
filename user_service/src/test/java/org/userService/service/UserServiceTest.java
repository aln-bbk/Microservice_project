package org.userService.service;

import org.userService.exception.EmailAlreadyExistsException;
import org.userService.exception.InvalidResourceStateException;
import org.userService.exception.ResourceNotFoundException;
import org.userService.dto.user.UserCreateRequest;
import org.userService.dto.user.UserResponse;
import org.userService.dto.user.UserUpdateRequest;
import org.userService.entity.User;
import org.userService.mapper.UserMapper;
import org.userService.repository.UserRepository;
import org.userService.dto.user.UserWithCardsResponse;
import org.userService.dto.card.PaymentCardResponse;

import org.userService.service.PaymentCardService;
import org.userService.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private PaymentCardService paymentCardService;

  @InjectMocks
  private UserService userService;

  // ---------- CREATE ----------

  @Test
  void create_shouldSaveUser_whenEmailIsUnique() {
    UserCreateRequest request = new UserCreateRequest(
        "Anna", "Ivanova", null, "anna@example.com"
    );

    User user = user(1L, "Anna", "Ivanova", "anna@example.com");
    UserResponse expected = response(user);

    when(userRepository.existsByEmail("anna@example.com"))
        .thenReturn(false);
    when(userMapper.toEntity(request)).thenReturn(user);
    when(userRepository.save(user)).thenReturn(user);
    when(userMapper.toResponse(user)).thenReturn(expected);

    UserResponse result = userService.create(request);

    assertEquals(expected, result);
    verify(userRepository).existsByEmail("anna@example.com");
    verify(userRepository).save(user);
  }

  @Test
  void create_shouldNormalizeEmail_beforeCheckingAndSaving() {
    UserCreateRequest request = new UserCreateRequest(
        "Anna", "Ivanova", null, "  ANNA@Example.COM  "
    );

    User user = user(1L, "Anna", "Ivanova", "old@example.com");

    when(userRepository.existsByEmail("anna@example.com"))
        .thenReturn(false);
    when(userMapper.toEntity(request)).thenReturn(user);
    when(userRepository.save(user)).thenReturn(user);
    when(userMapper.toResponse(user)).thenReturn(response(user));

    userService.create(request);

    assertEquals("anna@example.com", user.getEmail());
    verify(userRepository).existsByEmail("anna@example.com");
    verify(userRepository).save(user);
  }

  @Test
  void create_shouldThrow_whenEmailAlreadyExists() {
    UserCreateRequest request = new UserCreateRequest(
        "Anna", "Ivanova", null, "anna@example.com"
    );

    when(userRepository.existsByEmail("anna@example.com"))
        .thenReturn(true);

    assertThrows(
        EmailAlreadyExistsException.class,
        () -> userService.create(request)
    );

    verify(userRepository, never()).save(any(User.class));
    verify(userMapper, never()).toEntity(any(UserCreateRequest.class));
  }

  // ---------- GET BY ID ----------

  @Test
  void getById_shouldReturnUser_whenFound() {
    User user = user(1L, "Anna", "Ivanova", "anna@example.com");
    UserResponse expected = response(user);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userMapper.toResponse(user)).thenReturn(expected);

    assertEquals(expected, userService.getById(1L));
  }

  @Test
  void getById_shouldThrow_whenUserDoesNotExist() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> userService.getById(99L)
    );

    verify(userMapper, never()).toResponse(any(User.class));
  }

  // ---------- GET BY EMAIL ----------

  @Test
  void getByEmail_shouldNormalizeEmail_andReturnUser() {
    User user = user(1L, "Anna", "Ivanova", "anna@example.com");
    UserResponse expected = response(user);

    when(userRepository.findByEmail("anna@example.com"))
        .thenReturn(Optional.of(user));
    when(userMapper.toResponse(user)).thenReturn(expected);

    UserResponse result =
        userService.getByEmail("  ANNA@Example.COM ");

    assertEquals(expected, result);
    verify(userRepository).findByEmail("anna@example.com");
  }

  @Test
  void getByEmail_shouldThrow_whenUserDoesNotExist() {
    when(userRepository.findByEmail("missing@example.com"))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> userService.getByEmail("missing@example.com")
    );
  }

  // ---------- UPDATE ----------

  @Test
  void update_shouldUpdateUser_whenEmailIsUnchanged() {
    User user = user(1L, "Anna", "Ivanova", "anna@example.com");

    UserUpdateRequest request = new UserUpdateRequest(
        "Ann", "Ivanova", null, "anna@example.com"
    );

    UserResponse expected = response(user);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userMapper.toResponse(user)).thenReturn(expected);

    UserResponse result = userService.update(1L, request);

    assertEquals(expected, result);
    verify(userMapper).updateEntity(request, user);
    verify(userRepository, never()).existsByEmail(anyString());
  }

  @Test
  void update_shouldThrow_whenUserDoesNotExist() {
    UserUpdateRequest request = new UserUpdateRequest(
        "Anna", "Ivanova", null, "anna@example.com"
    );

    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> userService.update(99L, request)
    );

    verify(userMapper, never()).updateEntity(
        any(UserUpdateRequest.class), any(User.class)
    );
  }

  @Test
  void update_shouldThrow_whenNewEmailBelongsToAnotherUser() {
    User user = user(1L, "Anna", "Ivanova", "anna@example.com");

    UserUpdateRequest request = new UserUpdateRequest(
        "Anna", "Ivanova", null, "other@example.com"
    );

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.existsByEmail("other@example.com"))
        .thenReturn(true);

    assertThrows(
        EmailAlreadyExistsException.class,
        () -> userService.update(1L, request)
    );

    verify(userMapper, never()).updateEntity(
        any(UserUpdateRequest.class), any(User.class)
    );
  }

  // ---------- GET ALL / PAGINATION ----------

  @Test
  void getAll_shouldReturnMappedPage() {
    Pageable pageable = PageRequest.of(0, 10);

    User first = user(1L, "Anna", "Ivanova", "anna@example.com");
    User second = user(2L, "Ivan", "Petrov", "ivan@example.com");

    Page<User> users = new PageImpl<>(
        List.of(first, second), pageable, 2
    );

    when(userRepository.findAll(
        any(Specification.class), eq(pageable)
    )).thenReturn(users);

    when(userMapper.toResponse(first)).thenReturn(response(first));
    when(userMapper.toResponse(second)).thenReturn(response(second));

    Page<UserResponse> result =
        userService.getAll("Anna", null, pageable);

    assertEquals(2, result.getTotalElements());
    assertEquals(2, result.getContent().size());
    assertEquals("Anna", result.getContent().get(0).name());
    assertEquals(0, result.getNumber());
    assertEquals(10, result.getSize());
  }

  @Test
  void getAll_shouldReturnEmptyPage_whenNoUsersMatch() {
    Pageable pageable = PageRequest.of(0, 10);

    when(userRepository.findAll(
        any(Specification.class), eq(pageable)
    )).thenReturn(new PageImpl<>(List.of(), pageable, 0));

    Page<UserResponse> result =
        userService.getAll("Nobody", null, pageable);

    assertTrue(result.isEmpty());
    assertEquals(0, result.getTotalElements());
  }

  // ---------- ACTIVATE / DEACTIVATE ----------

  @Test
  void activate_shouldSetActiveToTrue() {
    User user = user(1L, "Anna", "Ivanova", "anna@example.com");
    user.setActive(false);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userMapper.toResponse(user)).thenReturn(response(user));

    userService.activate(1L);

    assertTrue(user.isActive());
  }

  @Test
  void deactivate_shouldSetActiveToFalse() {
    User user = user(1L, "Anna", "Ivanova", "anna@example.com");

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userMapper.toResponse(user)).thenReturn(response(user));

    userService.deactivate(1L);

    assertFalse(user.isActive());
  }

  @Test
  void activate_shouldThrow_whenUserDoesNotExist() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> userService.activate(99L)
    );
  }

  // ---------- DELETE ----------

  @Test
  void delete_shouldDeleteUser_whenFound() {
    User user = user(1L, "Anna", "Ivanova", "anna@example.com");

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    userService.delete(1L);

    verify(userRepository).delete(user);
  }

  @Test
  void delete_shouldThrow_whenUserDoesNotExist() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> userService.delete(99L)
    );

    verify(userRepository, never()).delete(any(User.class));
  }

  // ---------- ACTIVE USERS BY NAME ----------

  @Test
  void getActiveByName_shouldTrimName_andMapResults() {
    User user = user(1L, "Anna", "Ivanova", "anna@example.com");

    when(userRepository.findActiveByNameContaining("Anna"))
        .thenReturn(List.of(user));
    when(userMapper.toResponse(user)).thenReturn(response(user));

    List<UserResponse> result =
        userService.getActiveByName("  Anna  ");

    assertEquals(1, result.size());
    assertEquals("Anna", result.get(0).name());
    verify(userRepository).findActiveByNameContaining("Anna");
  }

  @Test
  void getActiveByName_shouldThrow_whenNameIsBlank() {
    assertThrows(
        InvalidResourceStateException.class,
        () -> userService.getActiveByName("  ")
    );

    verify(userRepository, never())
        .findActiveByNameContaining(anyString());
  }

  // ---------- USERS WITHOUT CARDS ----------

  @Test
  void getUsersWithoutCards_shouldReturnMappedUsers() {
    User user = user(1L, "Anna", "Ivanova", "anna@example.com");

    when(userRepository.findUsersWithoutCardsNative())
        .thenReturn(List.of(user));
    when(userMapper.toResponse(user)).thenReturn(response(user));

    List<UserResponse> result = userService.getUsersWithoutCards();

    assertEquals(1, result.size());
    assertEquals(1L, result.get(0).id());
  }

  @Test
  void getUsersWithoutCards_shouldReturnEmptyList_whenNoneFound() {
    when(userRepository.findUsersWithoutCardsNative())
        .thenReturn(List.of());

    List<UserResponse> result = userService.getUsersWithoutCards();

    assertTrue(result.isEmpty());
  }

  // ---------- USER GetWith ----------

  @Test
  void getWithCards_shouldReturnUserAndMaskedCards() {
    User user = user(1L, "Anna", "Ivanova", "anna@example.com");
    UserResponse userResponse = response(user);

    PaymentCardResponse card = new PaymentCardResponse(
        10L,
        1L,
        "4111111111111111",
        "ANNA IVANOVA",
        java.time.LocalDate.of(2030, 12, 31),
        true,
        null,
        null
    );

    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));
    when(userMapper.toResponse(user))
        .thenReturn(userResponse);
    when(paymentCardService.getByUserId(1L))
        .thenReturn(List.of(card));

    UserWithCardsResponse result = userService.getWithCards(1L);

    assertNotNull(result);
    assertEquals(userResponse, result.user());
    assertEquals(1, result.cards().size());
    assertEquals("**** **** **** 1111",
        result.cards().get(0).maskedNumber());
  }

  @Test
  void getWithCards_shouldReturnEmptyCards_whenUserHasNoCards() {
    User user = user(1L, "Anna", "Ivanova", "anna@example.com");
    UserResponse userResponse = response(user);

    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));
    when(userMapper.toResponse(user))
        .thenReturn(userResponse);
    when(paymentCardService.getByUserId(1L))
        .thenReturn(List.of());

    UserWithCardsResponse result = userService.getWithCards(1L);

    assertNotNull(result);
    assertEquals(userResponse, result.user());
    assertTrue(result.cards().isEmpty());
  }

  @Test
  void getWithCards_shouldThrow_whenUserDoesNotExist() {
    when(userRepository.findById(999L))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> userService.getWithCards(999L)
    );

    verify(paymentCardService, never()).getByUserId(anyLong());
  }
  // ---------- TEST HELPERS ----------

  private User user(
      Long id,
      String name,
      String surname,
      String email
  ) {
    User user = new User();
    user.setId(id);
    user.setName(name);
    user.setSurname(surname);
    user.setEmail(email);
    user.setActive(true);
    return user;
  }

  private UserResponse response(User user) {
    return new UserResponse(
        user.getId(),
        user.getName(),
        user.getSurname(),
        user.getBirthDate(),
        user.getEmail(),
        user.isActive(),
        null,
        null
    );
  }
}