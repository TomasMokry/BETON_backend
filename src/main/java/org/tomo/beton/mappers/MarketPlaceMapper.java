package org.tomo.beton.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.BeanMapping;
import org.tomo.beton.dtos.MarketPlaceDto;
import org.tomo.beton.dtos.MarketPlaceRequest;
import org.tomo.beton.entities.MarketPlace;

@Mapper(componentModel = "spring")
public interface MarketPlaceMapper {
    MarketPlaceDto toDto(MarketPlace marketPlace);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", source = "active", defaultValue = "true")
    MarketPlace toEntity(MarketPlaceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "address", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "startDate", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "endDate", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "notes", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void update(MarketPlaceRequest request, @MappingTarget MarketPlace marketPlace);
}
