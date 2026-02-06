package com.booking.service;

import com.booking.dto.booking.BookingFilterRequest;
import com.booking.dto.booking.BookingResponse;
import com.booking.dto.booking.CreateBookingRequest;
import com.booking.dto.common.PageResponse;
import com.booking.entity.*;
import com.booking.event.BookingEventPublisher;
import com.booking.exception.BookingConflictException;
import com.booking.exception.BookingValidationException;
import com.booking.exception.ForbiddenException;
import com.booking.exception.ResourceNotFoundException;
import com.booking.repository.BookingRepository;
import com.booking.repository.ResourceRepository;
import com.booking.repository.UserRepository;
import com.booking.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingEventPublisher eventPublisher;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private User adminUser;
    private Resource testResource;
    private UserPrincipal userPrincipal;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        // Set config values
        ReflectionTestUtils.setField(bookingService, "minDurationMinutes", 15);
        ReflectionTestUtils.setField(bookingService, "maxDurationHours", 8);

        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("user@test.com");
        testUser.setFullName("Test User");
        testUser.setRole(Role.USER);

        // Create admin user
        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@test.com");
        adminUser.setFullName("Admin User");
        adminUser.setRole(Role.ADMIN);

        // Create test resource
        testResource = new Resource();
        testResource.setId(1L);
        testResource.setName("Meeting Room A");
        testResource.setLocation("Floor 1");
        testResource.setCapacity(10);
        testResource.setIsActive(true);

        // Create principals
        userPrincipal = UserPrincipal.create(testUser);
        adminPrincipal = UserPrincipal.create(adminUser);
    }

    @Nested
    @DisplayName("Create Booking")
    class CreateBookingTests {

        @Test
        @DisplayName("Should create booking successfully")
        void shouldCreateBookingSuccessfully() {
            // Given
            LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            LocalDateTime endAt = startAt.plusHours(1);

            CreateBookingRequest request = new CreateBookingRequest();
            request.setResourceId(1L);
            request.setStartAt(startAt);
            request.setEndAt(endAt);
            request.setDescription("Team meeting");

            when(resourceRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testResource));
            when(bookingRepository.existsOverlappingBooking(eq(1L), eq(startAt), eq(endAt))).thenReturn(false);
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
                Booking b = invocation.getArgument(0);
                b.setId(1L);
                b.setCreatedAt(LocalDateTime.now());
                return b;
            });

            // When
            BookingResponse response = bookingService.createBooking(request, userPrincipal);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getResourceId()).isEqualTo(1L);
            assertThat(response.getResourceName()).isEqualTo("Meeting Room A");
            assertThat(response.getStatus()).isEqualTo(BookingStatus.CREATED);
            assertThat(response.getDescription()).isEqualTo("Team meeting");

            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw 409 when booking overlaps")
        void shouldThrowConflictWhenOverlapping() {
            // Given
            LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            LocalDateTime endAt = startAt.plusHours(1);

            CreateBookingRequest request = new CreateBookingRequest();
            request.setResourceId(1L);
            request.setStartAt(startAt);
            request.setEndAt(endAt);

            when(resourceRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testResource));
            when(bookingRepository.existsOverlappingBooking(eq(1L), eq(startAt), eq(endAt))).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> bookingService.createBooking(request, userPrincipal))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessageContaining("overlap");
        }

        @Test
        @DisplayName("Should throw 409 when partial overlap (start inside existing)")
        void shouldThrowConflictOnPartialOverlapStart() {
            // Existing: 10:00 - 11:00
            // New:      10:30 - 11:30
            LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(30);
            LocalDateTime endAt = startAt.plusHours(1);

            CreateBookingRequest request = new CreateBookingRequest();
            request.setResourceId(1L);
            request.setStartAt(startAt);
            request.setEndAt(endAt);

            when(resourceRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testResource));
            when(bookingRepository.existsOverlappingBooking(eq(1L), eq(startAt), eq(endAt))).thenReturn(true);

            assertThatThrownBy(() -> bookingService.createBooking(request, userPrincipal))
                    .isInstanceOf(BookingConflictException.class);
        }

        @Test
        @DisplayName("Should throw 409 when partial overlap (end inside existing)")
        void shouldThrowConflictOnPartialOverlapEnd() {
            // Existing: 10:00 - 11:00
            // New:      09:30 - 10:30
            LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(9).withMinute(30);
            LocalDateTime endAt = startAt.plusHours(1);

            CreateBookingRequest request = new CreateBookingRequest();
            request.setResourceId(1L);
            request.setStartAt(startAt);
            request.setEndAt(endAt);

            when(resourceRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testResource));
            when(bookingRepository.existsOverlappingBooking(eq(1L), eq(startAt), eq(endAt))).thenReturn(true);

            assertThatThrownBy(() -> bookingService.createBooking(request, userPrincipal))
                    .isInstanceOf(BookingConflictException.class);
        }

        @Test
        @DisplayName("Should throw 409 when new booking contains existing")
        void shouldThrowConflictWhenContainsExisting() {
            // Existing: 10:00 - 11:00
            // New:      09:00 - 12:00 (contains existing)
            LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
            LocalDateTime endAt = startAt.plusHours(3);

            CreateBookingRequest request = new CreateBookingRequest();
            request.setResourceId(1L);
            request.setStartAt(startAt);
            request.setEndAt(endAt);

            when(resourceRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testResource));
            when(bookingRepository.existsOverlappingBooking(eq(1L), eq(startAt), eq(endAt))).thenReturn(true);

            assertThatThrownBy(() -> bookingService.createBooking(request, userPrincipal))
                    .isInstanceOf(BookingConflictException.class);
        }

        @Test
        @DisplayName("Should allow adjacent bookings (no gap)")
        void shouldAllowAdjacentBookings() {
            // Existing: 10:00 - 11:00
            // New:      11:00 - 12:00 (starts exactly when existing ends)
            LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);
            LocalDateTime endAt = startAt.plusHours(1);

            CreateBookingRequest request = new CreateBookingRequest();
            request.setResourceId(1L);
            request.setStartAt(startAt);
            request.setEndAt(endAt);

            when(resourceRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testResource));
            when(bookingRepository.existsOverlappingBooking(eq(1L), eq(startAt), eq(endAt))).thenReturn(false);
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
                Booking b = invocation.getArgument(0);
                b.setId(2L);
                b.setCreatedAt(LocalDateTime.now());
                return b;
            });

            BookingResponse response = bookingService.createBooking(request, userPrincipal);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should throw 404 when resource not found")
        void shouldThrowNotFoundWhenResourceMissing() {
            LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

            CreateBookingRequest request = new CreateBookingRequest();
            request.setResourceId(999L);
            request.setStartAt(startAt);
            request.setEndAt(startAt.plusHours(1));

            when(resourceRepository.findByIdAndIsActiveTrue(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.createBooking(request, userPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw 400 when duration too short")
        void shouldThrowValidationWhenDurationTooShort() {
            LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            LocalDateTime endAt = startAt.plusMinutes(10); // Only 10 minutes

            CreateBookingRequest request = new CreateBookingRequest();
            request.setResourceId(1L);
            request.setStartAt(startAt);
            request.setEndAt(endAt);

            assertThatThrownBy(() -> bookingService.createBooking(request, userPrincipal))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("15 minutes");
        }

        @Test
        @DisplayName("Should throw 400 when duration too long")
        void shouldThrowValidationWhenDurationTooLong() {
            LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);
            LocalDateTime endAt = startAt.plusHours(9); // 9 hours

            CreateBookingRequest request = new CreateBookingRequest();
            request.setResourceId(1L);
            request.setStartAt(startAt);
            request.setEndAt(endAt);

            assertThatThrownBy(() -> bookingService.createBooking(request, userPrincipal))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("8 hours");
        }

        @Test
        @DisplayName("Should throw 400 when start time in the past")
        void shouldThrowValidationWhenStartInPast() {
            LocalDateTime startAt = LocalDateTime.now().minusHours(1);
            LocalDateTime endAt = startAt.plusHours(1);

            CreateBookingRequest request = new CreateBookingRequest();
            request.setResourceId(1L);
            request.setStartAt(startAt);
            request.setEndAt(endAt);

            assertThatThrownBy(() -> bookingService.createBooking(request, userPrincipal))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("future");
        }

        @Test
        @DisplayName("Should throw 400 when end before start")
        void shouldThrowValidationWhenEndBeforeStart() {
            LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);
            LocalDateTime endAt = startAt.minusHours(1);

            CreateBookingRequest request = new CreateBookingRequest();
            request.setResourceId(1L);
            request.setStartAt(startAt);
            request.setEndAt(endAt);

            assertThatThrownBy(() -> bookingService.createBooking(request, userPrincipal))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("before end");
        }
    }

    @Nested
    @DisplayName("Get Bookings")
    class GetBookingsTests {

        @Test
        @DisplayName("USER should see only own bookings")
        void userShouldSeeOnlyOwnBookings() {
            // Given
            Booking booking = createTestBooking(1L, testUser, testResource);
            PageImpl<Booking> page = new PageImpl<>(List.of(booking));

            when(bookingRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            // When
            PageResponse<BookingResponse> response = bookingService.getBookings(
                    new BookingFilterRequest(), PageRequest.of(0, 20), userPrincipal);

            // Then
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getUserId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("ADMIN should see all bookings")
        void adminShouldSeeAllBookings() {
            // Given
            Booking booking1 = createTestBooking(1L, testUser, testResource);
            Booking booking2 = createTestBooking(2L, adminUser, testResource);
            PageImpl<Booking> page = new PageImpl<>(List.of(booking1, booking2));

            when(bookingRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            // When
            PageResponse<BookingResponse> response = bookingService.getBookings(
                    new BookingFilterRequest(), PageRequest.of(0, 20), adminPrincipal);

            // Then
            assertThat(response.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Cancel Booking")
    class CancelBookingTests {

        @Test
        @DisplayName("Owner can cancel own booking")
        void ownerCanCancelOwnBooking() {
            // Given
            Booking booking = createTestBooking(1L, testUser, testResource);
            booking.setStartAt(LocalDateTime.now().plusDays(1));

            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

            // When
            BookingResponse response = bookingService.cancelBooking(1L, userPrincipal);

            // Then
            assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELED);
        }

        @Test
        @DisplayName("ADMIN can cancel any booking")
        void adminCanCancelAnyBooking() {
            // Given
            Booking booking = createTestBooking(1L, testUser, testResource);
            booking.setStartAt(LocalDateTime.now().plusDays(1));

            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

            // When
            BookingResponse response = bookingService.cancelBooking(1L, adminPrincipal);

            // Then
            assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELED);
        }

        @Test
        @DisplayName("Non-owner cannot cancel booking")
        void nonOwnerCannotCancelBooking() {
            // Given
            User otherUser = new User();
            otherUser.setId(UUID.randomUUID());
            otherUser.setRole(Role.USER);
            UserPrincipal otherPrincipal = UserPrincipal.create(otherUser);

            Booking booking = createTestBooking(1L, testUser, testResource);
            booking.setStartAt(LocalDateTime.now().plusDays(1));

            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            // When/Then
            assertThatThrownBy(() -> bookingService.cancelBooking(1L, otherPrincipal))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Cannot cancel already canceled booking")
        void cannotCancelAlreadyCanceledBooking() {
            // Given
            Booking booking = createTestBooking(1L, testUser, testResource);
            booking.setStatus(BookingStatus.CANCELED);
            booking.setStartAt(LocalDateTime.now().plusDays(1));

            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            // When/Then
            assertThatThrownBy(() -> bookingService.cancelBooking(1L, userPrincipal))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("already canceled");
        }

        @Test
        @DisplayName("Cannot cancel past booking")
        void cannotCancelPastBooking() {
            // Given
            Booking booking = createTestBooking(1L, testUser, testResource);
            booking.setStartAt(LocalDateTime.now().minusHours(1));

            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            // When/Then
            assertThatThrownBy(() -> bookingService.cancelBooking(1L, userPrincipal))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("already started");
        }
    }

    private Booking createTestBooking(Long id, User user, Resource resource) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUser(user);
        booking.setResource(resource);
        booking.setStartAt(LocalDateTime.now().plusDays(1).withHour(10));
        booking.setEndAt(LocalDateTime.now().plusDays(1).withHour(11));
        booking.setStatus(BookingStatus.CREATED);
        booking.setCreatedAt(LocalDateTime.now());
        return booking;
    }
}
