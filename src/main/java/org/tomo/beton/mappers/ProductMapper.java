package org.tomo.beton.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.tomo.beton.dtos.ProductDto;
import org.tomo.beton.entities.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "categoryId", source = "category.id")
    ProductDto toDto(Product product);
    Product toEntity(ProductDto productDto);
    @Mapping(target = "id", ignore = true)
    void update(ProductDto request, @MappingTarget Product product);
}
