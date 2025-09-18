package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController extends BaseController<User> {
    private final List<User> users = new ArrayList<>();
    private int nextId = 1;

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
        log.info("Получен запрос на получение всех пользователей. Количество пользователей: {}", users.size());
        return new ArrayList<>(users);
    }

    @Override
    protected ResponseEntity<Object> addEntity(User user) {
        log.info("Получен запрос на создание пользователя: {}", user);
        try {
            validateEntity(user);
            processUserName(user);
            user.setId(nextId++);
            users.add(user);
            log.info("Пользователь успешно создан: {}", user);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
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
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).getId() == user.getId()) {
                    users.set(i, user);
                    log.info("Пользователь успешно обновлен: {}", user);
                    return ResponseEntity.ok(user);
                }
            }
            log.error("Пользователь с id {} не найден для обновления", user.getId());
            return createErrorResponse("Пользователь с id " + user.getId() + " не найден", HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при обновлении пользователя: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    protected void validateEntity(User user) throws ValidationException {
    }

    private void processUserName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Имя пользователя пустое, установлен логин: {}", user.getLogin());
        }
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidationException(ValidationException e) {
        log.error("Ошибка валидации: {}", e.getMessage());
        return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception e) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);
        return createErrorResponse("Внутренняя ошибка сервера", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}