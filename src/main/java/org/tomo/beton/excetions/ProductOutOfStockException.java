package org.tomo.beton.excetions;

public class ProductOutOfStockException extends RuntimeException {
    public ProductOutOfStockException() {
        super("Product is out of stock");
    }
}
