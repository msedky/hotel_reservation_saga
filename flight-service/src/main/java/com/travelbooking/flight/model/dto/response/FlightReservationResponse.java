package com.travelbooking.flight.model.dto.response;

import com.travelbooking.flight.model.enums.FlightReservationStatus;
import java.util.UUID;

public record FlightReservationResponse(
    UUID id, UUID bookingId, UUID sagaId, String flightOfferId,
    String providerReference, FlightReservationStatus status
) {}
