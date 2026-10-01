package org.tomo.beton.excetions;

public class InvalidDiscountException extends RuntimeException {
    public InvalidDiscountException() {
        super("Invalid discount percent");
    }
}
