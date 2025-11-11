package ru.yandex.practicum.filmorate.storage.user;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Slf4j
@Repository
public class FeedDbStorage {
    @Autowired
    public FeedDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Getter
    public enum EventType {
        LIKE(1),
        REVIEW(2),
        FRIEND(3);

        private final int id;

        EventType(int id) {
            this.id = id;
        }

    }
    @Getter
    public enum OperationType {
        ADD(1),
        REMOVE(2),
        UPDATE(3);

        private final int id;
        OperationType(int id) {
            this.id = id;
        }

    }

    private final JdbcTemplate jdbc;


    public void createNewEvent(int userId, int entityId, EventType type, OperationType operation) {
        log.info("Data for new event: user_id = {}, entity_id = {}, type = {}, operation = {}", userId, entityId, type, operation);

        Timestamp createAt = Timestamp.from(Instant.now());
        String sql = "INSERT INTO events (user_id, type_id, operation_id, entity_id, create_at) " + "VALUES (?, ?, ?, ?, ?)";
        int i = jdbc.update(sql, userId, type.getId(), operation.getId(), entityId, createAt);
    }
}
