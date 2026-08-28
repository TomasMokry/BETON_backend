package org.tomo.beton.mappers;

import org.mapstruct.Mapper;
import org.tomo.beton.dtos.CategoryDto;
import org.tomo.beton.entities.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryDto toDto(Category category);
}
