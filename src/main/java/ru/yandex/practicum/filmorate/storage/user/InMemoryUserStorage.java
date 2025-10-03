package ru.yandex.practicum.filmorate.storage.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.practicum.filmorate.model.User;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Integer, User> users = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private static final Logger log = LoggerFactory.getLogger(InMemoryUserStorage.class);

    @Override
    public List<User> getAll() {
        log.debug("Получение всех пользователей из хранилища. Всего пользователей: {}", users.size());
        return new ArrayList<>(users.values());
    }

    @Override
    public Optional<User> getById(int id) {
        log.debug("Поиск пользователя по id: {}", id);
        User user = users.get(id);
        if (user == null) {
            log.debug("Пользователь с id {} не найден в хранилище", id);
        }
        return Optional.ofNullable(user);
    }

    @Override
    public User create(User user) {
        int id = nextId.getAndIncrement();
        user.setId(id);
        users.put(id, user);
        log.debug("Пользователь добавлен в хранилище: {} (id: {})", user.getLogin(), id);
        return user;
    }

    @Override
    public User update(User user) {
        users.put(user.getId(), user);
        log.debug("Пользователь обновлен в хранилище: {} (id: {})", user.getLogin(), user.getId());
        return user;
    }

    @Override
    public void delete(int id) {
        User removedUser = users.remove(id);
        if (removedUser != null) {
            log.debug("Пользователь удален из хранилища: {} (id: {})", removedUser.getLogin(), id);
        } else {
            log.debug("Попытка удаления несуществующего пользователя с id: {}", id);
        }
    }

    @Override
    public boolean exists(int id) {
        boolean exists = users.containsKey(id);
        log.debug("Проверка существования пользователя с id {}: {}", id, exists);
        return exists;
    }
}