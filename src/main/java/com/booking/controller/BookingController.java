package com.booking.controller;

import com.booking.dto.booking.BookingFilterRequest;
import com.booking.dto.booking.BookingResponse;
import com.booking.dto.booking.CreateBookingRequest;
import com.booking.dto.common.PageResponse;
import com.booking.security.UserPrincipal;
import com.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Booking management")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Operation(summary = "Create a new booking", description = "Create a booking for a resource. Validates time constraints and checks for conflicts.")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        BookingResponse response = bookingService.createBooking(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get bookings", description = "Get bookings with filters. USER sees only own bookings, ADMIN sees all.")
    public ResponseEntity<PageResponse<BookingResponse>> getBookings(
            @ModelAttribute BookingFilterRequest filter,
            @PageableDefault(size = 20, sort = "startAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        PageResponse<BookingResponse> response = bookingService.getBookings(filter, pageable, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID", description = "Get booking details. Only owner or ADMIN can access.")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        BookingResponse response = bookingService.getBookingById(id, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking", description = "Cancel a booking. Only owner or ADMIN can cancel. Cannot cancel past bookings.")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        BookingResponse response = bookingService.cancelBooking(id, principal);
        return ResponseEntity.ok(response);
    }
}
