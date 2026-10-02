package org.tomo.beton.excetions;

public class MarketPlaceInUseException extends RuntimeException {
    public MarketPlaceInUseException() {
        super("Market place has orders, archive it instead");
    }
}
