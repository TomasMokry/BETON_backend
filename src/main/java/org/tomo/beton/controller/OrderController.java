package org.tomo.beton.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tomo.beton.dtos.CartDto;
import org.tomo.beton.dtos.ErrorDto;
import org.tomo.beton.dtos.OrderDto;
import org.tomo.beton.dtos.OrderSummaryDto;
import org.tomo.beton.dtos.ReopenOrderRequest;
import org.tomo.beton.excetions.CartNotFoundException;
import org.tomo.beton.excetions.OrderAccessDeniedException;
import org.tomo.beton.excetions.OrderNotFoundException;
import org.tomo.beton.service.OrderService;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    public List<OrderDto> getAllOrders(
            @RequestParam(name = "marketPlaceId", required = false) String marketPlaceId,
            @RequestParam(name = "userId", required = false) Long userId
    ) {
        return orderService.getAllOrders(marketPlaceId, userId);
    }

    @GetMapping("/summary")
    public List<OrderSummaryDto> getSummary(
            @RequestParam(name = "marketPlaceId", required = false) String marketPlaceId,
            @RequestParam(name = "userId", required = false) Long userId
    ) {
        return orderService.getSummary(marketPlaceId, userId);
    }

    @GetMapping("/{orderId}")
    public OrderDto getOrder(@PathVariable("orderId") Long orderId) {
        return orderService.getOrder(orderId);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable("orderId") Long orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/reopen")
    public CartDto reopenOrder(
            @PathVariable("orderId") Long orderId,
            @Valid @RequestBody ReopenOrderRequest request
    ) {
        return orderService.reopenOrder(orderId, request.getCartId());
    }

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ErrorDto> handleCartNotFound(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ErrorDto> handleInvalidMarketPlaceId() {
        return ResponseEntity.badRequest().body(new ErrorDto("Invalid marketPlaceId"));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Void> handleOrderNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(OrderAccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleAccessDenied(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorDto(ex.getMessage()));
    }
}
