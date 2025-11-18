package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import ru.yandex.practicum.filmorate.storage.feed.FeedDbStorage;

@Data
public class Event {
    @JsonProperty("eventId")
    private int id;
    private int userId;
    private FeedDbStorage.EventType eventType;
    private FeedDbStorage.OperationType operation;
    private int entityId;
    private Long timestamp;
}
