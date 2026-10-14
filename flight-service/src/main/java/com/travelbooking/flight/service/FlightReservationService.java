package com.travelbooking.flight.service;
import com.travelbooking.flight.model.dto.request.FlightReservationRequest;
import com.travelbooking.flight.model.dto.response.FlightReservationResponse;
import java.util.UUID;
public interface FlightReservationService {
    FlightReservationResponse bookFlight(FlightReservationRequest request);
    FlightReservationResponse getByBookingId(UUID bookingId);
    FlightReservationResponse cancelFlight(UUID bookingId);
}
