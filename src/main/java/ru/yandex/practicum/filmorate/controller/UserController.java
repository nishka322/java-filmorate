package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController extends BaseController<User> {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Object> createUser(@Valid @RequestBody User user) {
        return addEntity(user);
    }

    @PutMapping
    public ResponseEntity<Object> updateUser(@Valid @RequestBody User user) {
        return updateEntity(user);
    }

    @GetMapping
    public List<User> getAllUsers() {
        log.info("Получен запрос на получение всех пользователей. Количество пользователей: {}", userService.getAllUsers().size());
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getUser(@PathVariable int id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            log.error("Пользователь с id {} не найден", id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Object> addFriend(@PathVariable int id, @PathVariable int friendId) {
        try {
            userService.addFriend(id, friendId);
            log.info("Пользователь {} добавил в друзья пользователя {}", id, friendId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при добавлении в друзья: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Object> removeFriend(@PathVariable int id, @PathVariable int friendId) {
        try {
            userService.removeFriend(id, friendId);
            log.info("Пользователь {} удалил из друзей пользователя {}", id, friendId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при удалении из друзей: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}/friends")
    public ResponseEntity<Object> getFriends(@PathVariable int id) {
        try {
            userService.getUserById(id);

            List<User> friends = userService.getFriends(id);
            log.info("Получен запрос на получение друзей пользователя {}. Количество друзей: {}", id, friends.size());
            return ResponseEntity.ok(friends);
        } catch (IllegalArgumentException e) {
            log.error("Пользователь с id {} не найден", id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public ResponseEntity<Object> getCommonFriends(@PathVariable int id, @PathVariable int otherId) {
        try {
            List<User> commonFriends = userService.getCommonFriends(id, otherId);
            log.info("Получен запрос на получение общих друзей пользователей {} и {}. Количество общих друзей: {}",
                    id, otherId, commonFriends.size());
            return ResponseEntity.ok(commonFriends);
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при получении общих друзей: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Override
    protected ResponseEntity<Object> addEntity(User user) {
        log.info("Получен запрос на создание пользователя: {}", user);
        try {
            validateEntity(user);
            processUserName(user);
            User createdUser = userService.createUser(user);
            log.info("Пользователь успешно создан: {}", createdUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при создании пользователя: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    protected ResponseEntity<Object> updateEntity(User user) {
        log.info("Получен запрос на обновление пользователя: {}", user);
        try {
            validateEntity(user);
            processUserName(user);

            if (!userService.userExists(user.getId())) {
                log.error("Пользователь с id {} не найден для обновления", user.getId());
                return createErrorResponse("Пользователь с id " + user.getId() + " не найден", HttpStatus.NOT_FOUND);
            }

            User updatedUser = userService.updateUser(user);
            log.info("Пользователь успешно обновлен: {}", updatedUser);
            return ResponseEntity.ok(updatedUser);
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при обновлении пользователя: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    protected void validateEntity(User user) throws ValidationException {
        log.debug("Валидация пользователя: {}", user.getLogin());
    }

    private void processUserName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Имя пользователя пустое, установлен логин: {}", user.getLogin());
        }
    }
}