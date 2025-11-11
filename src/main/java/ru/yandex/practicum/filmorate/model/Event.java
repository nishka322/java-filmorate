package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import ru.yandex.practicum.filmorate.storage.user.FeedDbStorage;

import java.time.Instant;

@Data
public class Event {
    private int id;
    private int userId;
    private FeedDbStorage.EventType type;
    private FeedDbStorage.OperationType operation;
    private int entityId;
    private Instant createAt;
}
