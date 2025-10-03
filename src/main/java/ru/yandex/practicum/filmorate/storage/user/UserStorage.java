package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;
import java.util.List;
import java.util.Optional;

public interface UserStorage {
    List<User> getAll();
    Optional<User> getById(int id);
    User create(User user);
    User update(User user);
    void delete(int id);
    boolean exists(int id);
}