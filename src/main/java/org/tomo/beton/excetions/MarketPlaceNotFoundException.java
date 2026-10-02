package org.tomo.beton.excetions;

public class MarketPlaceNotFoundException extends RuntimeException {
    public MarketPlaceNotFoundException() {
        super("Market place not found");
    }
}
