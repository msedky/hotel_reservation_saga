package com.travelbooking.flight.service.impl;
import com.travelbooking.flight.exception.FlightUnavailableException;
import com.travelbooking.flight.model.dto.request.FlightReservationRequest;
import com.travelbooking.flight.model.dto.response.FlightReservationResponse;
import com.travelbooking.flight.model.entity.FlightReservation;
import com.travelbooking.flight.model.enums.FlightReservationStatus;
import com.travelbooking.flight.provider.AirlineProviderClient;
import com.travelbooking.flight.repository.FlightReservationRepository;
import com.travelbooking.flight.service.FlightReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class FlightReservationServiceImpl implements FlightReservationService {
    private final FlightReservationRepository repository;
    private final AirlineProviderClient airlineProviderClient;
    @Override
    public FlightReservationResponse bookFlight(FlightReservationRequest request) {
        return repository.findByBookingId(request.bookingId())
            .map(this::toResponse)
            .orElseGet(() -> createReservation(request));
    }
    private FlightReservationResponse createReservation(FlightReservationRequest request) {
        FlightReservation reservation = new FlightReservation();
        reservation.setBookingId(request.bookingId());
        reservation.setSagaId(request.sagaId());
        reservation.setFlightOfferId(request.flightOfferId());
        reservation.setStatus(FlightReservationStatus.PENDING);
        reservation = repository.saveAndFlush(reservation);
        try {
            String ref = airlineProviderClient.bookFlight(request.flightOfferId());
            reservation.setProviderReference(ref);
            reservation.setStatus(FlightReservationStatus.CONFIRMED);
        } catch (FlightUnavailableException ex) {
            reservation.setStatus(FlightReservationStatus.FAILED);
        }
        return toResponse(repository.save(reservation));
    }
    @Override
    public FlightReservationResponse getByBookingId(UUID bookingId) {
        return toResponse(find(bookingId));
    }
    @Override
    public FlightReservationResponse cancelFlight(UUID bookingId) {
        FlightReservation reservation = find(bookingId);
        if (reservation.getStatus() == FlightReservationStatus.CANCELLED ||
            reservation.getStatus() == FlightReservationStatus.FAILED) {
            return toResponse(reservation);
        }
        if (reservation.getStatus() != FlightReservationStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation is not confirmed");
        }
        airlineProviderClient.cancelFlight(reservation.getProviderReference());
        reservation.setStatus(FlightReservationStatus.CANCELLED);
        return toResponse(repository.save(reservation));
    }
    private FlightReservation find(UUID bookingId) {
        return repository.findByBookingId(bookingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Reservation not found"));
    }
    private FlightReservationResponse toResponse(FlightReservation r) {
        return new FlightReservationResponse(r.getId(),r.getBookingId(),r.getSagaId(),
            r.getFlightOfferId(),r.getProviderReference(),r.getStatus());
    }
}
