package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import ru.yandex.practicum.filmorate.storage.feed.FeedDbStorage;

@Data
public class Event {
    private int id;
    private int userId;
    private FeedDbStorage.EventType eventType;
    private FeedDbStorage.OperationType operation;
    private int entityId;
    private Long timestamp;
}
