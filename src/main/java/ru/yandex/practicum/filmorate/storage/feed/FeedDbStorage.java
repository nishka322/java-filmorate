package ru.yandex.practicum.filmorate.storage.feed;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Event;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class FeedDbStorage {
    @Autowired
    public FeedDbStorage(JdbcTemplate jdbc, EventRowMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Getter
    public enum EventType {
        LIKE(1), REVIEW(2), FRIEND(3);

        private final int id;
        private static final Map<Integer, EventType> CODE_MAP = new HashMap<>();

        EventType(int id) {
            this.id = id;
        }

        static {
            for (EventType type : EventType.values()) {
                CODE_MAP.put(type.id, type);
            }
        }

        // Метод получения по коду
        public static EventType getByCode(int id) {
            return CODE_MAP.get(id);
        }

    }

    @Getter
    public enum OperationType {
        ADD(1), REMOVE(2), UPDATE(3);

        private final int id;
        private static final Map<Integer, OperationType> CODE_MAP = new HashMap<>();

        OperationType(int id) {
            this.id = id;
        }

        static {
            for (OperationType type : OperationType.values()) {
                CODE_MAP.put(type.id, type);
            }
        }

        // Метод получения по коду
        public static OperationType getByCode(int id) {
            return CODE_MAP.get(id);
        }
    }

    private final JdbcTemplate jdbc;
    private final EventRowMapper mapper;


    public void createNewEvent(int userId, int entityId, EventType type, OperationType operation) {
        log.info("Data for new event: user_id = {}, entity_id = {}, type = {}, operation = {}", userId, entityId, type, operation);

        Timestamp createAt = Timestamp.from(Instant.now());
        String sql = "INSERT INTO events (user_id, type_id, operation_id, entity_id, create_at) " + "VALUES (?, ?, ?, ?, ?)";
        jdbc.update(sql, userId, type.getId(), operation.getId(), entityId, createAt);
    }

    public List<Event> getUserFeed(int userId) {
        String sql = "SELECT * FROM events WHERE user_id = ?";
        List<Event> feed = new ArrayList<>();
        try {
            feed = jdbc.query(sql, mapper, userId);
        } catch (RuntimeException e) {
            log.error("Ошибка получения ленты событий.");
        }
        log.info("feed: {}", feed);
        return feed;
    }
}
