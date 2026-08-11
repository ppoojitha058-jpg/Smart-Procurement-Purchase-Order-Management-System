package com.eps.controller;

import com.eps.dto.ApiResponseDto;
import com.eps.dto.CategoryDto;
import com.eps.entity.Category;
import com.eps.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService categoryService;

    private CategoryDto mapToDto(Category category) {
        if (category == null) return null;
        return new CategoryDto(category.getCategoryId(), category.getCategoryName());
    }

    private Category mapToEntity(CategoryDto dto) {
        if (dto == null) return null;
        Category category = new Category();
        category.setCategoryId(dto.getCategoryId());
        category.setCategoryName(dto.getCategoryName());
        return category;
    }

    @PostMapping
    public ResponseEntity<ApiResponseDto> createCategory(@RequestBody CategoryDto dto) {
        Category category = mapToEntity(dto);
        Category created = categoryService.createCategory(category);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.builder().success(true).message("Category created").data(mapToDto(created)).build());
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        List<CategoryDto> dtos = categories.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Categories fetched").data(dtos).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        if (category == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDto.builder().success(false).message("Category not found with id: " + id).build());
        }
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Category fetched").data(mapToDto(category)).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto> updateCategory(@PathVariable Long id, @RequestBody CategoryDto dto) {
        Category category = mapToEntity(dto);
        Category updated = categoryService.updateCategory(id, category);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Category updated").data(mapToDto(updated)).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponseDto.builder().success(true).message("Category deleted").build());
    }
}
