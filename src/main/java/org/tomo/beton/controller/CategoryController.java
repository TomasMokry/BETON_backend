package org.tomo.beton.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tomo.beton.dtos.CategoryDto;
import org.tomo.beton.mappers.CategoryMapper;
import org.tomo.beton.repositories.CategoryRepository;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @GetMapping
    public List<CategoryDto> getCategories() {
        return categoryRepository.findAll(Sort.by("name")).stream().map(categoryMapper::toDto).toList();
    }
}
