package com.ishwinder.expense_tracker.category;

import com.ishwinder.expense_tracker.category.dto.CategoryRequest;
import com.ishwinder.expense_tracker.category.dto.CategoryResponse;
import com.ishwinder.expense_tracker.common.ConflictException;
import com.ishwinder.expense_tracker.common.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private CategoryRepository repository;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        // the lambda is our fake CurrentUserProvider: always user 1
        service = new CategoryService(repository, () -> USER_ID);
    }

    @Test
    void create_savesCategoryWithTrimmedName_whenNameIsFree() {
        // Arrange
        when(repository.existsByUserIdAndNameIgnoreCase(USER_ID, "Food")).thenReturn(false);
        when(repository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CategoryResponse response = service.create(new CategoryRequest("  Food  ", "#FF8800"));

        // Assert
        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Food");
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(response.name()).isEqualTo("Food");
    }

    @Test
    void create_throwsConflict_whenNameAlreadyExists() {
        when(repository.existsByUserIdAndNameIgnoreCase(USER_ID, "Food")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CategoryRequest("Food", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(repository, never()).save(any());
    }

    @Test
    void update_renamesCategory_whenNameIsFree() {
        Category existing = new Category(USER_ID, "Food", "#FF8800");
        when(repository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(existing));
        when(repository.existsByUserIdAndNameIgnoreCaseAndIdNot(USER_ID, "Groceries", 2L))
                .thenReturn(false);

        CategoryResponse response = service.update(2L, new CategoryRequest("Groceries", "#00AA55"));

        assertThat(response.name()).isEqualTo("Groceries");
        assertThat(response.color()).isEqualTo("#00AA55");
    }

    @Test
    void update_throwsNotFound_whenCategoryDoesNotBelongToUser() {
        when(repository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(5L, new CategoryRequest("X", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_throwsConflict_whenNameTakenByAnotherCategory() {
        Category existing = new Category(USER_ID, "Travel", null);
        when(repository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(existing));
        when(repository.existsByUserIdAndNameIgnoreCaseAndIdNot(USER_ID, "Food", 2L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update(2L, new CategoryRequest("Food", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void delete_throwsConflict_whenCategoryIsUsedByExpenses() {
        Category existing = new Category(USER_ID, "Food", null);
        when(repository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(existing));
        doThrow(new DataIntegrityViolationException("foreign key constraint"))
                .when(repository).flush();

        assertThatThrownBy(() -> service.delete(2L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cannot be deleted");
    }

    @Test
    void list_returnsCategoriesOfCurrentUser() {
        when(repository.findByUserIdOrderByNameAsc(USER_ID))
                .thenReturn(List.of(new Category(USER_ID, "Food", null),
                                    new Category(USER_ID, "Travel", null)));

        List<CategoryResponse> result = service.list();

        assertThat(result).extracting(CategoryResponse::name).containsExactly("Food", "Travel");
    }
}