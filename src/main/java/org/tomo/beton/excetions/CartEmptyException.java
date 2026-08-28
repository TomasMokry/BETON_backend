package org.tomo.beton.excetions;

public class CartEmptyException extends RuntimeException{
    public CartEmptyException() {
        super("Cart is empty");
    }
}
