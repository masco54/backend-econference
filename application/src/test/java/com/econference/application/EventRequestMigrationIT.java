package com.econference.application;

import com.econference.EConferenceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = EConferenceApplication.class)
@ActiveProfiles("test")
class EventRequestMigrationIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateEventRequestTablesAndKeepUserOwnershipOutsideTheEventModule() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            assertThat(tableExists(metaData, "EVENT_REQUESTS")).isTrue();
            assertThat(tableExists(metaData, "EVENT_REQUEST_STATUS_HISTORY")).isTrue();

            Set<String> eventRequestForeignKeys = importedTables(metaData, "EVENT_REQUESTS");
            Set<String> eventRequestHistoryForeignKeys = importedTables(metaData, "EVENT_REQUEST_STATUS_HISTORY");

            assertThat(eventRequestForeignKeys).contains("EVENTS");
            assertThat(eventRequestForeignKeys).doesNotContain("USERS");
            assertThat(eventRequestHistoryForeignKeys).contains("EVENT_REQUESTS");
            assertThat(eventRequestHistoryForeignKeys).doesNotContain("USERS");
        }
    }

    @Test
    void shouldEnforceConstraintsAndRollbackFailedTransactionOnH2() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        insertEvent(eventId);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try (var insertRequest = connection.prepareStatement("""
                    insert into event_requests (
                        id, tracking_code, status, requester_first_name, requester_last_name, requester_name,
                        requester_email, event_name, event_theme, event_start_date, event_end_date,
                        event_location, event_type, has_registration, has_submission, has_site,
                        linked_event_id, created_at, updated_at
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                insertRequest.setObject(1, requestId);
                insertRequest.setString(2, "SOL-2026-H2A1");
                insertRequest.setString(3, "APPROVED");
                insertRequest.setString(4, "Ana");
                insertRequest.setString(5, "Silva");
                insertRequest.setString(6, "Ana Silva");
                insertRequest.setString(7, "ana.silva@example.com");
                insertRequest.setString(8, "Congresso Brasileiro de IA");
                insertRequest.setString(9, "Aplicações de IA na pesquisa científica");
                insertRequest.setObject(10, LocalDate.of(2026, 9, 10));
                insertRequest.setObject(11, LocalDate.of(2026, 9, 12));
                insertRequest.setString(12, "Florianópolis");
                insertRequest.setString(13, "conference");
                insertRequest.setBoolean(14, true);
                insertRequest.setBoolean(15, true);
                insertRequest.setBoolean(16, true);
                insertRequest.setObject(17, eventId);
                insertRequest.setTimestamp(18, Timestamp.from(Instant.now()));
                insertRequest.setTimestamp(19, Timestamp.from(Instant.now()));
                insertRequest.executeUpdate();
            }

            assertThatThrownBy(() -> {
                try (var invalidHistory = connection.prepareStatement("""
                        insert into event_request_status_history (
                            id, event_request_id, status, message, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?)
                        """)) {
                    invalidHistory.setObject(1, UUID.randomUUID());
                    invalidHistory.setObject(2, UUID.randomUUID());
                    invalidHistory.setString(3, "APPROVED");
                    invalidHistory.setString(4, "should fail");
                    invalidHistory.setTimestamp(5, Timestamp.from(Instant.now()));
                    invalidHistory.setTimestamp(6, Timestamp.from(Instant.now()));
                    invalidHistory.executeUpdate();
                }
            }).isInstanceOf(SQLException.class);

            connection.rollback();
        }

        Integer requestCount = jdbcTemplate.queryForObject(
                "select count(*) from event_requests where id = ?",
                Integer.class,
                requestId
        );
        assertThat(requestCount).isZero();

        UUID missingEventReference = UUID.randomUUID();
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into event_requests (
                    id, tracking_code, status, requester_first_name, requester_last_name, requester_name,
                    requester_email, event_name, event_theme, event_start_date, event_end_date,
                    event_location, event_type, has_registration, has_submission, has_site,
                    linked_event_id, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                "SOL-2026-H2B2",
                "APPROVED",
                "Bruno",
                "Costa",
                "Bruno Costa",
                "bruno@example.com",
                "Simpósio de Dados",
                "Dados para pesquisa aplicada",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                "Curitiba",
                "symposium",
                true,
                false,
                true,
                missingEventReference,
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now())
        )).hasMessageContaining("event_requests");

        UUID cascadeRequestId = UUID.randomUUID();
        insertEventRequest(cascadeRequestId, "SOL-2026-H2C3", eventId);
        jdbcTemplate.update("""
                insert into event_request_status_history (
                    id, event_request_id, status, message, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                cascadeRequestId,
                "REQUESTED",
                "submitted",
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now())
        );

        jdbcTemplate.update("delete from event_requests where id = ?", cascadeRequestId);

        Integer historyCount = jdbcTemplate.queryForObject(
                "select count(*) from event_request_status_history where event_request_id = ?",
                Integer.class,
                cascadeRequestId
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
                "Evento base para validar constraints da migração",
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

    private Set<String> importedTables(DatabaseMetaData metaData, String tableName) throws Exception {
        Set<String> importedTables = new HashSet<>();

        try (ResultSet resultSet = metaData.getImportedKeys(null, null, tableName)) {
            while (resultSet.next()) {
                importedTables.add(resultSet.getString("PKTABLE_NAME"));
            }
        }

        return importedTables;
    }
}