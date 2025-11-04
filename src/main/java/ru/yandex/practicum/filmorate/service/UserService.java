package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserStorage userStorage;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public List<User> getAllUsers() {
        log.debug("Получение списка всех пользователей");
        List<User> users = userStorage.getAll();
        log.debug("Получено {} пользователей", users.size());
        return users;
    }

    public User getUserById(int id) {
        log.debug("Поиск пользователя с id {}", id);
        User user = userStorage.getById(id)
                .orElseThrow(() -> {
                    log.error("Пользователь с id {} не найден", id);
                    return new IllegalArgumentException("Пользователь с id " + id + " не найден");
                });
        return user;
    }

    public User createUser(User user) {
        log.debug("Создание нового пользователя: {}", user.getLogin());
        User createdUser = userStorage.create(user);
        log.info("Создан новый пользователь: {} (id: {})", createdUser.getLogin(), createdUser.getId());
        return createdUser;
    }

    public User updateUser(User user) {
        log.debug("Обновление пользователя с id {}", user.getId());
        getUserById(user.getId());
        User updatedUser = userStorage.update(user);
        log.info("Пользователь с id {} обновлен", user.getId());
        return updatedUser;
    }

    public void addFriend(int userId, int friendId) {
        log.debug("Добавление в друзья: пользователь {} отправляет запрос пользователю {}", userId, friendId);
        getUserById(userId);
        getUserById(friendId);

        UserDbStorage userDbStorage = (UserDbStorage) userStorage;
        userDbStorage.addFriend(userId, friendId, FriendshipStatus.PENDING);
        log.info("Пользователь {} отправил запрос на дружбу пользователю {}", userId, friendId);
    }

    public void confirmFriend(int userId, int friendId) {
        log.debug("Подтверждение дружбы: пользователь {} подтверждает запрос от пользователя {}", userId, friendId);
        getUserById(userId);
        getUserById(friendId);

        UserDbStorage userDbStorage = (UserDbStorage) userStorage;
        userDbStorage.updateFriendshipStatus(friendId, userId, FriendshipStatus.CONFIRMED);
        log.info("Дружба между пользователем {} и пользователем {} подтверждена", userId, friendId);
    }

    public void removeFriend(int userId, int friendId) {
        log.debug("Удаление из друзей: пользователь {} удаляет пользователя {}", userId, friendId);
        getUserById(userId);
        getUserById(friendId);

        UserDbStorage userDbStorage = (UserDbStorage) userStorage;
        userDbStorage.removeFriend(userId, friendId);
        log.info("Пользователь {} удалил пользователя {} из друзей", userId, friendId);
    }

    public List<User> getFriends(int userId) {
        log.debug("Получение списка друзей для пользователя {}", userId);
        UserDbStorage userDbStorage = (UserDbStorage) userStorage;
        return userDbStorage.getFriends(userId);
    }

    public List<User> getFriendRequests(int userId) {
        log.debug("Получение входящих запросов на дружбу для пользователя {}", userId);
        UserDbStorage userDbStorage = (UserDbStorage) userStorage;
        return userDbStorage.getFriendRequests(userId);
    }

    public List<User> getCommonFriends(int userId1, int userId2) {
        log.debug("Поиск общих друзей между пользователем {} и пользователем {}", userId1, userId2);
        List<User> friends1 = getFriends(userId1);
        List<User> friends2 = getFriends(userId2);

        List<User> commonFriends = friends1.stream()
                .filter(friends2::contains)
                .collect(Collectors.toList());

        log.debug("Найдено {} общих друзей между пользователем {} и пользователем {}",
                commonFriends.size(), userId1, userId2);
        return commonFriends;
    }

    public boolean userExists(int id) {
        log.debug("Проверка существования пользователя с id {}", id);
        return userStorage.exists(id);
    }
}