package com.ishwinder.expense_tracker.category;

import com.ishwinder.expense_tracker.category.dto.CategoryRequest;
import com.ishwinder.expense_tracker.category.dto.CategoryResponse;
import com.ishwinder.expense_tracker.common.ConflictException;
import com.ishwinder.expense_tracker.common.CurrentUserProvider;
import com.ishwinder.expense_tracker.common.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        return CategoryResponse.from(category);   // saved automatically at commit
    }

    @Transactional
    public void delete(Long id) {
        Category category = findOwned(id);
        try {
            repository.delete(category);
            repository.flush();    // run the DELETE now so a FK violation surfaces here
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Category is used by expenses and cannot be deleted");
        }
    }

    private Category findOwned(Long id) {
        return repository.findByIdAndUserId(id, currentUser.currentUserId())
                .orElseThrow(() -> new NotFoundException("Category " + id + " not found"));
    }
}