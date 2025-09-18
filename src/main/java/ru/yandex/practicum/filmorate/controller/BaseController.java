package ru.yandex.practicum.filmorate.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseController<T> {

    protected ResponseEntity<Object> createErrorResponse(String message, HttpStatus status) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("status", status.toString());
        return ResponseEntity.status(status).body(errorResponse);
    }

    protected abstract ResponseEntity<Object> addEntity(T entity);
    protected abstract ResponseEntity<Object> updateEntity(T entity);
    protected abstract void validateEntity(T entity) throws ValidationException;
}