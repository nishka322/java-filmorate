package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Event;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Slf4j
@Component
public class EventRowMapper implements RowMapper<Event> {
    @Override
    public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
        Event event = new Event();
        event.setId(rs.getInt("event_id"));
        event.setUserId(rs.getInt("user_id"));
        event.setType(FeedDbStorage.EventType.getByCode(rs.getInt("type_id")));
        event.setOperation(FeedDbStorage.OperationType.getByCode(rs.getInt("operation_id")));
        event.setEntityId(rs.getInt("entity_id"));
        event.setCreateAt(rs.getTimestamp("create_at").toInstant());
        return event;
    }
}
