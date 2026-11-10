package org.course_project.order_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.course_project.order_service.dto.ItemCreateRequest;
import org.course_project.order_service.dto.ItemResponse;
import org.course_project.order_service.dto.ItemUpdateRequest;
import org.course_project.order_service.service.ItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ItemController {
  private final ItemService itemService;

  @PostMapping
  public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(itemService.create(request));
  }

  @GetMapping
  public ResponseEntity<Page<ItemResponse>> getAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
    return ResponseEntity.ok(itemService.getAll(pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ItemResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(itemService.getById(id));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ItemResponse> update(@PathVariable Long id, @Valid @RequestBody ItemUpdateRequest request) {
    return ResponseEntity.ok(itemService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    itemService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
