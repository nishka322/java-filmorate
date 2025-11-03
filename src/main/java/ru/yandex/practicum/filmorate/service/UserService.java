package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserStorage userStorage;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public List<User> getAllUsers() {
        log.debug("Получение списка всех пользователей");
        List<User> users = userStorage.getAll();

        users.forEach(this::loadUserFriends);

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

        loadUserFriends(user);
        return user;
    }

    public User createUser(User user) {
        log.debug("Создание нового пользователя: {}", user.getLogin());

        if (user.getFriends() == null) {
            user.setFriends(new HashMap<>());
        }

        User createdUser = userStorage.create(user);
        log.info("Создан новый пользователь: {} (id: {})", createdUser.getLogin(), createdUser.getId());
        return createdUser;
    }

    public User updateUser(User user) {
        log.debug("Обновление пользователя с id {}", user.getId());

        getUserById(user.getId());

        if (user.getFriends() == null) {
            user.setFriends(new HashMap<>());
        }

        User updatedUser = userStorage.update(user);
        log.info("Пользователь с id {} обновлен", user.getId());
        return updatedUser;
    }

    public void addFriend(int userId, int friendId) {
        log.debug("Добавление в друзья: пользователь {} отправляет запрос пользователю {}", userId, friendId);

        getUserById(userId);
        getUserById(friendId);

        if (userStorage instanceof UserDbStorage) {
            UserDbStorage userDbStorage = (UserDbStorage) userStorage;
            userDbStorage.addFriend(userId, friendId, FriendshipStatus.PENDING);
        } else {
            User user = getUserById(userId);
            user.getFriends().put(friendId, FriendshipStatus.PENDING);
            userStorage.update(user);
        }

        log.info("Пользователь {} отправил запрос на дружбу пользователю {}", userId, friendId);
    }

    public void confirmFriend(int userId, int friendId) {
        log.debug("Подтверждение дружбы: пользователь {} подтверждает запрос от пользователя {}", userId, friendId);

        getUserById(userId);
        getUserById(friendId);

        if (userStorage instanceof UserDbStorage) {
            UserDbStorage userDbStorage = (UserDbStorage) userStorage;
            userDbStorage.updateFriendshipStatus(friendId, userId, FriendshipStatus.CONFIRMED);
        } else {
            User user = getUserById(userId);
            User friend = getUserById(friendId);

            if (friend.getFriends().getOrDefault(userId, FriendshipStatus.PENDING) != FriendshipStatus.PENDING) {
                log.error("Запрос на дружбу от пользователя {} к пользователю {} не найден", friendId, userId);
                throw new IllegalArgumentException("Запрос на дружбу не найден");
            }

            user.getFriends().put(friendId, FriendshipStatus.CONFIRMED);
            friend.getFriends().put(userId, FriendshipStatus.CONFIRMED);

            userStorage.update(user);
            userStorage.update(friend);
        }

        log.info("Дружба между пользователем {} и пользователем {} подтверждена", userId, friendId);
    }

    public void removeFriend(int userId, int friendId) {
        log.debug("Удаление из друзей: пользователь {} удаляет пользователя {}", userId, friendId);

        getUserById(userId);
        getUserById(friendId);

        if (userStorage instanceof UserDbStorage) {
            UserDbStorage userDbStorage = (UserDbStorage) userStorage;
            userDbStorage.removeFriend(userId, friendId);
        } else {
            User user = getUserById(userId);
            User friend = getUserById(friendId);

            user.getFriends().remove(friendId);
            friend.getFriends().remove(userId);

            userStorage.update(user);
            userStorage.update(friend);
        }

        log.info("Пользователь {} удалил пользователя {} из друзей", userId, friendId);
    }

    public List<User> getFriends(int userId) {
        log.debug("Получение списка друзей для пользователя {}", userId);

        if (userStorage instanceof UserDbStorage) {
            UserDbStorage userDbStorage = (UserDbStorage) userStorage;
            return userDbStorage.getFriends(userId);
        } else {
            User user = getUserById(userId);
            return user.getFriends().entrySet().stream()
                    .filter(entry -> entry.getValue() == FriendshipStatus.CONFIRMED)
                    .map(entry -> getUserById(entry.getKey()))
                    .collect(Collectors.toList());
        }
    }

    public List<User> getFriendRequests(int userId) {
        log.debug("Получение входящих запросов на дружбу для пользователя {}", userId);

        if (userStorage instanceof UserDbStorage) {
            UserDbStorage userDbStorage = (UserDbStorage) userStorage;
            return userDbStorage.getFriendRequests(userId);
        } else {
            User user = getUserById(userId);
            return user.getFriends().entrySet().stream()
                    .filter(entry -> entry.getValue() == FriendshipStatus.PENDING)
                    .map(entry -> getUserById(entry.getKey()))
                    .collect(Collectors.toList());
        }
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

    public FriendshipStatus getFriendshipStatus(int userId, int friendId) {
        log.debug("Получение статуса дружбы между пользователем {} и пользователем {}", userId, friendId);
        User user = getUserById(userId);
        return user.getFriends().getOrDefault(friendId, null);
    }

    public boolean userExists(int id) {
        log.debug("Проверка существования пользователя с id {}", id);
        return userStorage.exists(id);
    }

    private void loadUserFriends(User user) {
        if (userStorage instanceof UserDbStorage) {
            UserDbStorage userDbStorage = (UserDbStorage) userStorage;

            List<User> friends = userDbStorage.getFriends(user.getId());
            user.getFriends().clear();
            friends.forEach(friend ->
                    user.getFriends().put(friend.getId(), FriendshipStatus.CONFIRMED));

            List<User> requests = userDbStorage.getFriendRequests(user.getId());
            requests.forEach(request ->
                    user.getFriends().put(request.getId(), FriendshipStatus.PENDING));
        }
    }
}