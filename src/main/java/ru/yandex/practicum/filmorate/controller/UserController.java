package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final FilmService filmService;

    public UserController(UserService userService, FilmService filmService) {
        this.userService = userService;
        this.filmService = filmService;
    }

    @PostMapping
    public User createUser(@Valid @RequestBody User user) {
        processUserName(user);
        User createdUser = userService.createUser(user);
        log.info("Создан пользователь с id: {}", createdUser.getId());
        return createdUser;
    }

    @GetMapping("/{id}/recommendations")
    public List<Film> getRecommendations(@PathVariable int id, @RequestParam(defaultValue = "10") int count) {
        log.info("Получен запрос на рекомендации для пользователя {}", id);
        return filmService.getRecomendation(id, count);
    }

    @PutMapping
    public User updateUser(@Valid @RequestBody User user) {
        processUserName(user);
        User updatedUser = userService.updateUser(user);
        log.info("Обновлен пользователь с id: {}", updatedUser.getId());
        return updatedUser;
    }

    @GetMapping
    public List<User> getAllUsers() {
        log.info("Получен запрос на получение всех пользователей");
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable int id) {
        log.info("Получен запрос на получение пользователя с id: {}", id);
        return userService.getUserById(id);
    }

    @PutMapping("/{id}/friends/{friendId}")
    public void addFriend(@PathVariable int id, @PathVariable int friendId) {
        userService.addFriend(id, friendId);
        log.info("Пользователь {} добавил в друзья пользователя {}", id, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public void removeFriend(@PathVariable int id, @PathVariable int friendId) {
        userService.removeFriend(id, friendId);
        log.info("Пользователь {} удалил из друзей пользователя {}", id, friendId);
    }

    @GetMapping("/{id}/friends")
    public List<User> getFriends(@PathVariable int id) {
        log.info("Получен запрос на получение друзей пользователя {}", id);
        return userService.getFriends(id);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable int id, @PathVariable int otherId) {
        log.info("Получен запрос на общих друзей пользователей {} и {}", id, otherId);
        return userService.getCommonFriends(id, otherId);
    }

    @GetMapping("/{id}/feed")
    public Object getUserFeed(@PathVariable int id) {
        log.info("Получен запрос на ленту пользователя {}", id);
        return userService.getUserFeed(id);
    }

    @DeleteMapping("/{userId}")
    public void removeUser(@PathVariable int userId) {
        userService.removeUser(userId);
        log.info("Удален пользователь с id: {}", userId);
    }

    private void processUserName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}