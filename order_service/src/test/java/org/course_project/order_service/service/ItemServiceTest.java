package org.course_project.order_service.service;

import org.course_project.order_service.dto.*;
import org.course_project.order_service.entity.Item;
import org.course_project.order_service.exception.NotFoundException;
import org.course_project.order_service.mapper.ItemMapper;
import org.course_project.order_service.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {
    @Mock ItemRepository repository;
    @Mock ItemMapper mapper;
    @InjectMocks ItemService service;

    private Item item;
    private ItemResponse response;

    @BeforeEach
    void setUp() {
        item = new Item();
        item.setId(1L);
        item.setName("Keyboard");
        item.setPrice(new BigDecimal("25.50"));
        response = new ItemResponse(1L, "Keyboard", new BigDecimal("25.50"), null, null);
    }

    @Test
    void createMapsAndSavesItem() {
        ItemCreateRequest request = new ItemCreateRequest("Keyboard", new BigDecimal("25.50"));
        when(mapper.toEntity(request)).thenReturn(item);
        when(repository.save(item)).thenReturn(item);
        when(mapper.toResponse(item)).thenReturn(response);

        assertEquals(response, service.create(request));

        verify(mapper).toEntity(request);
        verify(repository).save(item);
        verify(mapper).toResponse(item);
    }

    @Test
    void getAllReturnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(item)));
        when(mapper.toResponse(item)).thenReturn(response);

        Page<ItemResponse> result = service.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(response, result.getContent().getFirst());
        verify(repository).findAll(pageable);
    }

    @Test
    void getByIdReturnsItem() {
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        when(mapper.toResponse(item)).thenReturn(response);

        assertEquals(response, service.getById(1L));
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getById(99L));
        verify(mapper, never()).toResponse(any());
    }

    @Test
    void updateChangesAndSavesItem() {
        ItemUpdateRequest request = new ItemUpdateRequest("Mouse", new BigDecimal("10.00"));
        ItemResponse updated = new ItemResponse(1L, "Mouse", new BigDecimal("10.00"), null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        doAnswer(inv -> {
            item.setName(request.name());
            item.setPrice(request.price());
            return null;
        }).when(mapper).updateEntity(request, item);
        when(repository.save(item)).thenReturn(item);
        when(mapper.toResponse(item)).thenReturn(updated);

        assertEquals(updated, service.update(1L, request));
        verify(mapper).updateEntity(request, item);
        verify(repository).save(item);
    }

    @Test
    void updateThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> service.update(99L, new ItemUpdateRequest("Mouse", BigDecimal.TEN)));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteRemovesItemAndFlushes() {
        when(repository.findById(1L)).thenReturn(Optional.of(item));

        service.delete(1L);

        verify(repository).delete(item);
        verify(repository).flush();
    }

    @Test
    void deleteThrowsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.delete(99L));
        verify(repository, never()).delete(any());
        verify(repository, never()).flush();
    }

    @Test
    void deleteConvertsForeignKeyViolationToConflict() {
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        doThrow(new DataIntegrityViolationException("foreign key"))
                .when(repository).flush();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.delete(1L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Item is used by an order and cannot be deleted", ex.getReason());
    }
}
