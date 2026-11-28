package com.ishwinder.expense_tracker.category;

import com.ishwinder.expense_tracker.category.dto.CategoryRequest;
import com.ishwinder.expense_tracker.category.dto.CategoryResponse;
import com.ishwinder.expense_tracker.common.ConflictException;
import com.ishwinder.expense_tracker.common.CurrentUserProvider;
import com.ishwinder.expense_tracker.common.NotFoundException;

import com.ishwinder.expense_tracker.category.CategoryRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository repository;
    private final CurrentUserProvider currentUser;

    public CategoryService(CategoryRepository repository, CurrentUserProvider currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return repository.findByUserIdOrderByNameAsc(currentUser.currentUserId()).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getOwned(Long id) {
        return CategoryResponse.from(findOwned(id));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getOwnedByUserId(Long categoryId, Long userId) {
        Category category = repository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Category " + categoryId + " not found"));
        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public Map<Long, CategoryResponse> findByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return repository.findByUserIdAndIdIn(currentUser.currentUserId(), ids).stream()
                .collect(Collectors.toMap(Category::getId, CategoryResponse::from));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Long userId = currentUser.currentUserId();
        String name = request.name().trim();

        if (repository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }
        Category saved = repository.save(new Category(userId, name, request.color()));
        return CategoryResponse.from(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findOwned(id);
        String name = request.name().trim();

        if (repository.existsByUserIdAndNameIgnoreCaseAndIdNot(category.getUserId(), name, id)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }
        category.setName(name);
        category.setColor(request.color());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findOwned(id);
        try {
            repository.delete(category);
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Category is used by expenses and cannot be deleted");
        }
    }

    private Category findOwned(Long id) {
        return repository.findByIdAndUserId(id, currentUser.currentUserId())
                .orElseThrow(() -> new NotFoundException("Category " + id + " not found"));
    }
}