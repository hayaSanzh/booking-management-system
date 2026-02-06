package com.booking.integration;

import com.booking.dto.auth.LoginRequest;
import com.booking.dto.auth.RegisterRequest;
import com.booking.dto.booking.CreateBookingRequest;
import com.booking.entity.Resource;
import com.booking.entity.Role;
import com.booking.entity.User;
import com.booking.repository.BookingRepository;
import com.booking.repository.ResourceRepository;
import com.booking.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Booking API using Testcontainers.
 * These tests require Docker to be installed and running.
 * If Docker is not available, tests will be skipped automatically.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookingIntegrationTest {

    private static final boolean DOCKER_AVAILABLE = isDockerAvailable();

    static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable e) {
            System.out.println("Docker not available, skipping integration tests: " + e.getMessage());
            return false;
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = DOCKER_AVAILABLE 
            ? new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("booking_test")
                .withUsername("test")
                .withPassword("test")
            : null;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres != null && postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.flyway.enabled", () -> "true");
        }
    }

    @BeforeEach
    void checkDockerAvailable() {
        assumeTrue(DOCKER_AVAILABLE, "Docker is not available, skipping integration tests");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static String userToken;
    private static String adminToken;
    private static Long resourceId;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @BeforeEach
    void setUp() {
        // Clean up bookings before each test
        bookingRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Setup: Create admin user and resource")
    void setupTestData() throws Exception {
        // Create admin user
        User admin = new User();
        admin.setEmail("admin@integration.test");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setFullName("Integration Admin");
        admin.setRole(Role.ADMIN);
        admin.setIsActive(true);
        userRepository.save(admin);

        // Create regular user
        User user = new User();
        user.setEmail("user@integration.test");
        user.setPasswordHash(passwordEncoder.encode("user123"));
        user.setFullName("Integration User");
        user.setRole(Role.USER);
        user.setIsActive(true);
        userRepository.save(user);

        // Create resource
        Resource resource = new Resource();
        resource.setName("Test Room");
        resource.setLocation("Floor 1");
        resource.setCapacity(10);
        resource.setIsActive(true);
        resource = resourceRepository.save(resource);
        resourceId = resource.getId();

        // Login as admin
        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setEmail("admin@integration.test");
        adminLogin.setPassword("admin123");

        MvcResult adminResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode adminJson = objectMapper.readTree(adminResult.getResponse().getContentAsString());
        adminToken = adminJson.get("accessToken").asText();

        // Login as user
        LoginRequest userLogin = new LoginRequest();
        userLogin.setEmail("user@integration.test");
        userLogin.setPassword("user123");

        MvcResult userResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode userJson = objectMapper.readTree(userResult.getResponse().getContentAsString());
        userToken = userJson.get("accessToken").asText();
    }

    @Test
    @Order(2)
    @DisplayName("POST /bookings - Should create booking successfully")
    void shouldCreateBookingSuccessfully() throws Exception {
        LocalDateTime startAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endAt = startAt.plusHours(1);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setResourceId(resourceId);
        request.setStartAt(startAt);
        request.setEndAt(endAt);
        request.setDescription("Integration test meeting");

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.resourceId").value(resourceId))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.description").value("Integration test meeting"));
    }

    @Test
    @Order(3)
    @DisplayName("POST /bookings - Should return 409 on overlapping booking")
    void shouldReturn409OnOverlap() throws Exception {
        LocalDateTime startAt = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endAt = startAt.plusHours(1);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setResourceId(resourceId);
        request.setStartAt(startAt);
        request.setEndAt(endAt);

        // First booking - should succeed
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second booking - same time, should fail with 409
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value(containsString("overlap")));
    }

    @Test
    @Order(4)
    @DisplayName("POST /bookings - Should return 409 on partial overlap")
    void shouldReturn409OnPartialOverlap() throws Exception {
        LocalDateTime existingStart = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime existingEnd = existingStart.plusHours(2);

        // Create first booking (10:00 - 12:00)
        CreateBookingRequest first = new CreateBookingRequest();
        first.setResourceId(resourceId);
        first.setStartAt(existingStart);
        first.setEndAt(existingEnd);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        // Try overlapping booking (11:00 - 13:00) - should fail
        CreateBookingRequest overlapping = new CreateBookingRequest();
        overlapping.setResourceId(resourceId);
        overlapping.setStartAt(existingStart.plusHours(1)); // 11:00
        overlapping.setEndAt(existingEnd.plusHours(1)); // 13:00

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlapping)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(5)
    @DisplayName("POST /bookings - Should allow adjacent booking")
    void shouldAllowAdjacentBooking() throws Exception {
        LocalDateTime firstStart = LocalDateTime.now().plusDays(4).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime firstEnd = firstStart.plusHours(1);

        // First booking (10:00 - 11:00)
        CreateBookingRequest first = new CreateBookingRequest();
        first.setResourceId(resourceId);
        first.setStartAt(firstStart);
        first.setEndAt(firstEnd);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        // Adjacent booking (11:00 - 12:00) - should succeed
        CreateBookingRequest adjacent = new CreateBookingRequest();
        adjacent.setResourceId(resourceId);
        adjacent.setStartAt(firstEnd); // 11:00
        adjacent.setEndAt(firstEnd.plusHours(1)); // 12:00

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjacent)))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(6)
    @DisplayName("POST /bookings - Should return 400 on invalid duration")
    void shouldReturn400OnInvalidDuration() throws Exception {
        LocalDateTime startAt = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0).withSecond(0).withNano(0);

        // Too short (10 min)
        CreateBookingRequest tooShort = new CreateBookingRequest();
        tooShort.setResourceId(resourceId);
        tooShort.setStartAt(startAt);
        tooShort.setEndAt(startAt.plusMinutes(10));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooShort)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("15 minutes")));

        // Too long (9 hours)
        CreateBookingRequest tooLong = new CreateBookingRequest();
        tooLong.setResourceId(resourceId);
        tooLong.setStartAt(startAt);
        tooLong.setEndAt(startAt.plusHours(9));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("8 hours")));
    }

    @Test
    @Order(7)
    @DisplayName("POST /bookings/{id}/cancel - Should cancel and free slot")
    void shouldCancelAndFreeSlot() throws Exception {
        LocalDateTime startAt = LocalDateTime.now().plusDays(6).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endAt = startAt.plusHours(1);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setResourceId(resourceId);
        request.setStartAt(startAt);
        request.setEndAt(endAt);

        // Create booking
        MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long bookingId = json.get("id").asLong();

        // Cancel booking
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        // Book same slot again - should succeed
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(8)
    @DisplayName("GET /bookings - User sees only own bookings")
    void userSeesOnlyOwnBookings() throws Exception {
        LocalDateTime startAt = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);

        // User creates booking
        CreateBookingRequest userBooking = new CreateBookingRequest();
        userBooking.setResourceId(resourceId);
        userBooking.setStartAt(startAt);
        userBooking.setEndAt(startAt.plusHours(1));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userBooking)))
                .andExpect(status().isCreated());

        // Admin creates booking
        CreateBookingRequest adminBooking = new CreateBookingRequest();
        adminBooking.setResourceId(resourceId);
        adminBooking.setStartAt(startAt.plusHours(2));
        adminBooking.setEndAt(startAt.plusHours(3));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminBooking)))
                .andExpect(status().isCreated());

        // User should see only 1 booking (own)
        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        // Admin should see 2 bookings (all)
        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @Order(9)
    @DisplayName("Security: 401 without token, 403 for non-owner")
    void securityTests() throws Exception {
        // 401 - No token
        mockMvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // Create a booking as user
        LocalDateTime startAt = LocalDateTime.now().plusDays(8).withHour(10).withMinute(0).withSecond(0).withNano(0);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setResourceId(resourceId);
        request.setStartAt(startAt);
        request.setEndAt(startAt.plusHours(1));

        MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long bookingId = json.get("id").asLong();

        // Create another user
        RegisterRequest newUser = new RegisterRequest();
        newUser.setEmail("other@integration.test");
        newUser.setPassword("other123");
        newUser.setFullName("Other User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated());

        LoginRequest otherLogin = new LoginRequest();
        otherLogin.setEmail("other@integration.test");
        otherLogin.setPassword("other123");

        MvcResult otherResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String otherToken = objectMapper.readTree(otherResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        // 403 - Other user cannot cancel
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
