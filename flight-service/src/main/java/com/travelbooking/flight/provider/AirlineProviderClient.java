package com.travelbooking.flight.provider;

public interface AirlineProviderClient {

    String bookFlight(String flightOfferId);

    void cancelFlight(String providerReference);
}
