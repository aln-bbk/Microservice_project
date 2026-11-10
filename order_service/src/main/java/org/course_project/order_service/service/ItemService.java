package org.course_project.order_service.service;

import lombok.RequiredArgsConstructor;
import org.course_project.order_service.dto.ItemCreateRequest;
import org.course_project.order_service.dto.ItemResponse;
import org.course_project.order_service.dto.ItemUpdateRequest;
import org.course_project.order_service.entity.Item;
import org.course_project.order_service.exception.NotFoundException;
import org.course_project.order_service.mapper.ItemMapper;
import org.course_project.order_service.repository.ItemRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemService {
  private final ItemRepository itemRepository;
  private final ItemMapper itemMapper;

  @Transactional
  public ItemResponse create(ItemCreateRequest request) {
    return itemMapper.toResponse(itemRepository.save(itemMapper.toEntity(request)));
  }

  @Transactional(readOnly = true)
  public Page<ItemResponse> getAll(Pageable pageable) {
    return itemRepository.findAll(pageable).map(itemMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public ItemResponse getById(Long id) {
    return itemMapper.toResponse(findOrThrow(id));
  }

  @Transactional
  public ItemResponse update(Long id, ItemUpdateRequest request) {
    Item item = findOrThrow(id);
    itemMapper.updateEntity(request, item);
    return itemMapper.toResponse(itemRepository.save(item));
  }

  @Transactional
  public void delete(Long id) {
    Item item = findOrThrow(id);
    try {
      itemRepository.delete(item);
      itemRepository.flush();
    } catch (DataIntegrityViolationException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Item is used by an order and cannot be deleted", ex);
    }
  }

  private Item findOrThrow(Long id) {
    return itemRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Item not found: " + id));
  }
}
