package org.tomo.beton.mappers;

import org.mapstruct.Mapper;
import org.tomo.beton.dtos.OrderDto;
import org.tomo.beton.entities.Order;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderDto toDto(Order order);
}
