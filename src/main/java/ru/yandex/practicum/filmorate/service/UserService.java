package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    public void addFriend(int userId, int friendId) {
        log.debug("Добавление в друзья: пользователь {} добавляет пользователя {}", userId, friendId);
        User user = getUserById(userId);
        User friend = getUserById(friendId);

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        userStorage.update(user);
        userStorage.update(friend);
        log.info("Пользователь {} и пользователь {} теперь друзья", userId, friendId);
    }

    public void removeFriend(int userId, int friendId) {
        log.debug("Удаление из друзей: пользователь {} удаляет пользователя {}", userId, friendId);
        User user = getUserById(userId);
        User friend = getUserById(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        userStorage.update(user);
        userStorage.update(friend);
        log.info("Пользователь {} и пользователь {} больше не друзья", userId, friendId);
    }

    public List<User> getFriends(int userId) {
        log.debug("Получение списка друзей для пользователя {}", userId);
        User user = getUserById(userId);
        List<User> friends = user.getFriends().stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
        log.debug("Найдено {} друзей для пользователя {}", friends.size(), userId);
        return friends;
    }

    public List<User> getCommonFriends(int userId1, int userId2) {
        log.debug("Поиск общих друзей между пользователем {} и пользователем {}", userId1, userId2);
        User user1 = getUserById(userId1);
        User user2 = getUserById(userId2);

        Set<Integer> commonFriendIds = new HashSet<>(user1.getFriends());
        commonFriendIds.retainAll(user2.getFriends());

        List<User> commonFriends = commonFriendIds.stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
        log.debug("Найдено {} общих друзей между пользователем {} и пользователем {}",
                commonFriends.size(), userId1, userId2);
        return commonFriends;
    }

    public User getUserById(int id) {
        log.debug("Поиск пользователя с id {}", id);
        return userStorage.getById(id)
                .orElseThrow(() -> {
                    log.error("Пользователь с id {} не найден", id);
                    return new IllegalArgumentException("Пользователь с id " + id + " не найден");
                });
    }

    public User createUser(User user) {
        log.debug("Создание нового пользователя: {}", user.getLogin());
        User createdUser = userStorage.create(user);
        log.info("Создан новый пользователь: {} (id: {})", createdUser.getLogin(), createdUser.getId());
        return createdUser;
    }

    public User updateUser(User user) {
        log.debug("Обновление пользователя с id {}", user.getId());
        User updatedUser = userStorage.update(user);
        log.info("Пользователь с id {} обновлен", user.getId());
        return updatedUser;
    }

    public boolean userExists(int id) {
        log.debug("Проверка существования пользователя с id {}", id);
        return userStorage.exists(id);
    }
}