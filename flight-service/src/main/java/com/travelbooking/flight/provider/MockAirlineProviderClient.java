package com.travelbooking.flight.provider;

import com.travelbooking.flight.exception.FlightUnavailableException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockAirlineProviderClient implements AirlineProviderClient {

    @Override
    public String bookFlight(String flightOfferId) {

        if ("UNAVAILABLE".equalsIgnoreCase(flightOfferId)) {
            throw new FlightUnavailableException("Flight offer is no longer available");
        }

        return "AIR-" + UUID.randomUUID();
    }

    @Override
    public void cancelFlight(String providerReference) {
        // Simulate successful cancellation
    }
}
