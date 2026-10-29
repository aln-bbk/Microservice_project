package org.userService.service;

import org.userService.exception.EmailAlreadyExistsException;
import org.userService.exception.InvalidResourceStateException;
import org.userService.exception.ResourceNotFoundException;
import org.userService.dto.card.PaymentCardSummaryResponse;
import org.userService.dto.user.UserCreateRequest;
import org.userService.dto.user.UserResponse;
import org.userService.dto.user.UserUpdateRequest;
import org.userService.entity.User;
import org.userService.mapper.UserMapper;
import org.userService.repository.UserRepository;
import org.userService.specification.UserSpecifications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.userService.dto.user.UserWithCardsResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.userService.dto.user.UserRegistrationRequest;
import org.userService.integration.AuthCredentialsClient;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PaymentCardService paymentCardService;
  private final AuthCredentialsClient authCredentialsClient;

  public UserService(
      UserRepository userRepository,
      UserMapper userMapper,
      PaymentCardService paymentCardService,
      AuthCredentialsClient authCredentialsClient
  ) {
    this.userRepository = userRepository;
    this.userMapper = userMapper;
    this.paymentCardService = paymentCardService;
    this.authCredentialsClient = authCredentialsClient;
  }


  @Transactional
  public UserResponse create(UserCreateRequest request) {
    String email = EmailNormalizer.normalize(request.email());

    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException(email);
    }

    User user = userMapper.toEntity(request);
    user.setEmail(email);

    User savedUser = userRepository.save(user);
    return userMapper.toResponse(savedUser);
  }

  /**
   * Creates the user profile and stores login/password in Auth Service.
   * The password is never persisted by User Service.
   */
  @Transactional
  public UserResponse register(UserRegistrationRequest request) {
    String email = EmailNormalizer.normalize(request.email());

    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException(email);
    }

    User user = new User();
    user.setName(request.name());
    user.setSurname(request.surname());
    user.setBirthDate(request.birthDate());
    user.setEmail(email);

    User savedUser = userRepository.saveAndFlush(user);

    // Any client error is a RuntimeException and rolls back the User DB transaction.
    // Auth Service stores only a BCrypt hash of the password.
    authCredentialsClient.createCredentials(
        savedUser.getId(), email, request.password()
    );

    return userMapper.toResponse(savedUser);
  }

  @Transactional(readOnly = true)
  public UserResponse getByEmail(String email) {
    String normalizedEmail = EmailNormalizer.normalize(email);

    User user = userRepository.findByEmail(normalizedEmail)
        .orElseThrow(() -> new ResourceNotFoundException(
            "User not found with email: " + normalizedEmail
        ));

    return userMapper.toResponse(user);
  }

  @Transactional
  @CacheEvict(cacheNames = "userWithCards", key = "#id")
  public UserResponse update(Long id, UserUpdateRequest request) {
    User user = findUserOrThrow(id);
    String email = EmailNormalizer.normalize(request.email());

    if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException(email);
    }

    userMapper.updateEntity(request, user);
    user.setEmail(email);
    userRepository.flush();
    return userMapper.toResponse(user);
  }


  @Transactional(readOnly = true)
  public UserResponse getById(Long id) {
    User user = findUserOrThrow(id);
    return userMapper.toResponse(user);
  }

  @Transactional(readOnly = true)
  public Page<UserResponse> getAll(String name, String surname, Pageable pageable) {
    return userRepository.findAll(
        UserSpecifications.filterByNameAndSurname(name, surname),
        pageable
    ).map(userMapper::toResponse);
  }



  @Transactional
  @CacheEvict(cacheNames = "userWithCards", key = "#id")
  public UserResponse activate(Long id) {
    User user = findUserOrThrow(id);
    user.setActive(true);
    userRepository.flush();
    return userMapper.toResponse(user);
  }

  @Transactional
  @CacheEvict(cacheNames = "userWithCards", key = "#id")
  public UserResponse deactivate(Long id) {
    User user = findUserOrThrow(id);
    user.setActive(false);
    userRepository.flush();
    return userMapper.toResponse(user);
  }

  @Transactional
  @CacheEvict(cacheNames = "userWithCards", key = "#id")
  public void delete(Long id) {
    User user = findUserOrThrow(id);
    userRepository.delete(user);
  }

  private User findUserOrThrow(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() ->
            new ResourceNotFoundException("User not found: " + id)
        );
  }


  @Transactional(readOnly = true)
  public java.util.List<UserResponse> getActiveByName(String name) {
    if (name == null || name.isBlank()) {
      throw new InvalidResourceStateException("Name must not be blank");
    }

    return userRepository.findActiveByNameContaining(name.trim())
        .stream()
        .map(userMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public java.util.List<UserResponse> getUsersWithoutCards() {
    return userRepository.findUsersWithoutCardsNative()
        .stream()
        .map(userMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  @Cacheable(cacheNames = "userWithCards", key = "#id")
  public UserWithCardsResponse getWithCards(Long id) {
    User user = findUserOrThrow(id);

    UserResponse userResponse = userMapper.toResponse(user);

    var cards = paymentCardService.getByUserId(id)
        .stream()
        .map(card -> new PaymentCardSummaryResponse(
            card.id(),
            card.userId(),
            maskCardNumber(card.number()),
            card.holder(),
            card.expirationDate(),
            card.active(),
            card.createdAt(),
            card.updatedAt()
        ))
        .toList();

    return new UserWithCardsResponse(userResponse, cards);
  }

  private String maskCardNumber(String number) {
    if (number == null || number.isBlank()) {
      return "****";
    }

    String digits = number.replaceAll("\\D", "");

    if (digits.length() <= 4) {
      return "****";
    }

    return "**** **** **** " +
        digits.substring(digits.length() - 4);
  }

}