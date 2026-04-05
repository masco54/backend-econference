package com.econference.application;

import com.econference.EConferenceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = EConferenceApplication.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class EventRequestMigrationPostgresIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("econference")
            .withUsername("econference")
            .withPassword("econference");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.h2.console.enabled", () -> false);
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyEventRequestMigrationWithConstraintsOnPostgres() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertThat(tableExists(metaData, "event_requests")).isTrue();
            assertThat(tableExists(metaData, "event_request_status_history")).isTrue();
        }

        UUID eventId = UUID.randomUUID();
        insertEvent(eventId);

        UUID firstRequestId = UUID.randomUUID();
        insertEventRequest(firstRequestId, "SOL-2026-PGA1", eventId);

        assertThatThrownBy(() -> insertEventRequest(UUID.randomUUID(), "SOL-2026-PGA1", eventId))
                .hasMessageContaining("event_requests");

        jdbcTemplate.update("""
                insert into event_request_status_history (
                    id, event_request_id, status, message, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                firstRequestId,
                "REQUESTED",
                "submitted",
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now())
        );

        jdbcTemplate.update("delete from event_requests where id = ?", firstRequestId);

        Integer historyCount = jdbcTemplate.queryForObject(
                "select count(*) from event_request_status_history where event_request_id = ?",
                Integer.class,
                firstRequestId
        );
        assertThat(historyCount).isZero();
    }

    private void insertEvent(UUID eventId) {
        jdbcTemplate.update("""
                insert into events (
                    id, name, acronym, description, start_date, end_date, location, organizer_id, status, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                eventId,
                "Evento Base",
                "BASE",
                "Evento base para validar compatibilidade PostgreSQL",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                "Florianópolis",
                UUID.randomUUID(),
                "DRAFT",
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now())
        );
    }

    private void insertEventRequest(UUID requestId, String trackingCode, UUID eventId) {
        jdbcTemplate.update("""
                insert into event_requests (
                    id, tracking_code, status, requester_first_name, requester_last_name, requester_name,
                    requester_email, event_name, event_theme, event_start_date, event_end_date,
                    event_location, event_type, has_registration, has_submission, has_site,
                    linked_event_id, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                requestId,
                trackingCode,
                "APPROVED",
                "Ana",
                "Silva",
                "Ana Silva",
                trackingCode.toLowerCase() + "@example.com",
                "Congresso Brasileiro de IA",
                "Aplicações de IA na pesquisa científica",
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12),
                "Florianópolis",
                "conference",
                true,
                true,
                true,
                eventId,
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now())
        );
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws Exception {
        try (ResultSet resultSet = metaData.getTables(null, null, tableName, null)) {
            return resultSet.next();
        }
    }
}