package org.tomo.beton.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tomo.beton.dtos.CartDto;
import org.tomo.beton.dtos.CartItemDto;
import org.tomo.beton.entities.Cart;
import org.tomo.beton.entities.CartItem;

@Mapper(
        componentModel = "spring"
)
public interface CartMapper {
    @Mapping(target = "items", source = "items")
    @Mapping(target = "subtotalPrice", expression = "java(cart.getSubtotalPrice())")
    @Mapping(target = "discountAmount", expression = "java(cart.getDiscountAmount())")
    @Mapping(target = "totalPrice", expression = "java(cart.getTotalPrice())")
    CartDto toDto(Cart cart);

    @Mapping(target = "subtotalPrice", expression = "java(cartItem.getSubtotalPrice())")
    @Mapping(target = "totalPrice", expression = "java(cartItem.getTotalPrice())")
    CartItemDto toDto(CartItem cartItem);
}
