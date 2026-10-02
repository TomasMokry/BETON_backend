package org.tomo.beton.excetions;

public class MarketPlaceArchivedException extends RuntimeException {
    public MarketPlaceArchivedException() {
        super("Market place is archived");
    }
}
