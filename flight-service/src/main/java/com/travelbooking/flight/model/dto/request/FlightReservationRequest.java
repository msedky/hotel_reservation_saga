package com.travelbooking.flight.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FlightReservationRequest(
    @NotNull UUID bookingId,
    @NotNull UUID sagaId,
    @NotBlank String flightOfferId
) {}
