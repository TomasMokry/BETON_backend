package org.tomo.beton.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tomo.beton.dtos.CheckoutRequest;
import org.tomo.beton.dtos.CheckoutResponse;
import org.tomo.beton.dtos.ErrorDto;
import org.tomo.beton.excetions.CartEmptyException;
import org.tomo.beton.excetions.CartNotFoundException;
import org.tomo.beton.excetions.ProductNotFoundException;
import org.tomo.beton.excetions.ProductOutOfStockException;
import org.tomo.beton.service.CheckoutService;

@RestController
@AllArgsConstructor
@RequestMapping("/checkout")
@CrossOrigin(origins = "https://beton-hazel.vercel.app")
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request){
        return checkoutService.checkout(request);
    }

    @ExceptionHandler({CartNotFoundException.class, CartEmptyException.class})
    public ResponseEntity<ErrorDto> handleCartExceptions(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler({ProductNotFoundException.class, ProductOutOfStockException.class})
    public ResponseEntity<ErrorDto> handleProductExceptions(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorDto(ex.getMessage()));
    }
}
