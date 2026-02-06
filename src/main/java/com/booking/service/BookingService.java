package com.booking.service;

import com.booking.dto.booking.BookingFilterRequest;
import com.booking.dto.booking.BookingResponse;
import com.booking.dto.booking.CreateBookingRequest;
import com.booking.dto.common.PageResponse;
import com.booking.entity.Booking;
import com.booking.entity.BookingStatus;
import com.booking.entity.Resource;
import com.booking.entity.Role;
import com.booking.entity.User;
import com.booking.exception.BookingConflictException;
import com.booking.exception.BookingValidationException;
import com.booking.exception.ForbiddenException;
import com.booking.exception.ResourceNotFoundException;
import com.booking.repository.BookingRepository;
import com.booking.repository.BookingSpecifications;
import com.booking.repository.ResourceRepository;
import com.booking.repository.UserRepository;
import com.booking.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    @Value("${app.booking.min-duration-minutes:15}")
    private int minDurationMinutes;

    @Value("${app.booking.max-duration-hours:8}")
    private int maxDurationHours;

    public BookingService(BookingRepository bookingRepository,
                          ResourceRepository resourceRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a new booking with conflict check
     */
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, UserPrincipal principal) {
        // Validate time constraints
        validateBookingTime(request.getStartAt(), request.getEndAt());

        // Find resource
        Resource resource = resourceRepository.findByIdAndIsActiveTrue(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", request.getResourceId()));

        // Check for overlapping bookings
        if (bookingRepository.existsOverlappingBooking(
                request.getResourceId(), 
                request.getStartAt(), 
                request.getEndAt())) {
            throw new BookingConflictException(request.getResourceId());
        }

        // Find user
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));

        // Create booking
        Booking booking = new Booking();
        booking.setResource(resource);
        booking.setUser(user);
        booking.setStartAt(request.getStartAt());
        booking.setEndAt(request.getEndAt());
        booking.setDescription(request.getDescription());
        booking.setStatus(BookingStatus.CREATED);

        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    /**
     * Get bookings with filters (USER sees only own, ADMIN sees all)
     */
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getBookings(BookingFilterRequest filter, 
                                                      Pageable pageable,
                                                      UserPrincipal principal) {
        Specification<Booking> spec = Specification.where(null);

        // USER can only see their own bookings
        if (principal.getRole() != Role.ADMIN) {
            spec = spec.and(BookingSpecifications.hasUserId(principal.getId()));
        }

        // Apply filters
        if (filter.getResourceId() != null) {
            spec = spec.and(BookingSpecifications.hasResourceId(filter.getResourceId()));
        }
        if (filter.getStatus() != null) {
            spec = spec.and(BookingSpecifications.hasStatus(filter.getStatus()));
        }
        if (filter.getDateFrom() != null) {
            spec = spec.and(BookingSpecifications.startsAfter(filter.getDateFrom()));
        }
        if (filter.getDateTo() != null) {
            spec = spec.and(BookingSpecifications.endsBefore(filter.getDateTo()));
        }

        Page<Booking> page = bookingRepository.findAll(spec, pageable);

        return PageResponse.<BookingResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * Get booking by ID (owner or ADMIN only)
     */
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, UserPrincipal principal) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));

        // Check access
        if (!canAccessBooking(booking, principal)) {
            throw new ForbiddenException("You don't have permission to view this booking");
        }

        return toResponse(booking);
    }

    /**
     * Cancel a booking (owner or ADMIN only)
     */
    @Transactional
    public BookingResponse cancelBooking(Long id, UserPrincipal principal) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));

        // Check access
        if (!canAccessBooking(booking, principal)) {
            throw new ForbiddenException("You don't have permission to cancel this booking");
        }

        // Check if already canceled
        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new BookingValidationException("Booking is already canceled");
        }

        // Check if booking is in the past
        if (booking.getStartAt().isBefore(LocalDateTime.now())) {
            throw new BookingValidationException("Cannot cancel a booking that has already started");
        }

        booking.setStatus(BookingStatus.CANCELED);
        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    /**
     * Validate booking time constraints
     */
    private void validateBookingTime(LocalDateTime startAt, LocalDateTime endAt) {
        // Start must be before end
        if (!startAt.isBefore(endAt)) {
            throw new BookingValidationException("Start time must be before end time");
        }

        // Must be in the future
        if (startAt.isBefore(LocalDateTime.now())) {
            throw new BookingValidationException("Booking must be in the future");
        }

        // Check duration
        Duration duration = Duration.between(startAt, endAt);
        long minutes = duration.toMinutes();

        if (minutes < minDurationMinutes) {
            throw new BookingValidationException(
                    "Booking duration must be at least " + minDurationMinutes + " minutes");
        }

        long maxMinutes = maxDurationHours * 60L;
        if (minutes > maxMinutes) {
            throw new BookingValidationException(
                    "Booking duration cannot exceed " + maxDurationHours + " hours");
        }
    }

    /**
     * Check if user can access the booking
     */
    private boolean canAccessBooking(Booking booking, UserPrincipal principal) {
        // ADMIN can access all
        if (principal.getRole() == Role.ADMIN) {
            return true;
        }
        // Owner can access their own
        return booking.getUser().getId().equals(principal.getId());
    }

    /**
     * Convert entity to response DTO
     */
    private BookingResponse toResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setResourceId(booking.getResource().getId());
        response.setResourceName(booking.getResource().getName());
        response.setUserId(booking.getUser().getId());
        response.setUserFullName(booking.getUser().getFullName());
        response.setStartAt(booking.getStartAt());
        response.setEndAt(booking.getEndAt());
        response.setStatus(booking.getStatus());
        response.setDescription(booking.getDescription());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }
}
