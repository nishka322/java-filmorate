package ru.yandex.practicum.filmorate.storage.film;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.practicum.filmorate.model.Film;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Integer, Film> films = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private static final Logger log = LoggerFactory.getLogger(InMemoryFilmStorage.class);

    @Override
    public List<Film> getAll() {
        log.debug("Получение всех фильмов из хранилища. Всего фильмов: {}", films.size());
        return new ArrayList<>(films.values());
    }

    @Override
    public Optional<Film> getById(int id) {
        log.debug("Поиск фильма по id: {}", id);
        Film film = films.get(id);
        if (film == null) {
            log.debug("Фильм с id {} не найден в хранилище", id);
        }
        return Optional.ofNullable(film);
    }

    @Override
    public Film create(Film film) {
        int id = nextId.getAndIncrement();
        film.setId(id);
        films.put(id, film);
        log.debug("Фильм добавлен в хранилище: {} (id: {})", film.getName(), id);
        return film;
    }

    @Override
    public Film update(Film film) {
        films.put(film.getId(), film);
        log.debug("Фильм обновлен в хранилище: {} (id: {})", film.getName(), film.getId());
        return film;
    }

    @Override
    public void delete(int id) {
        Film removedFilm = films.remove(id);
        if (removedFilm != null) {
            log.debug("Фильм удален из хранилища: {} (id: {})", removedFilm.getName(), id);
        } else {
            log.debug("Попытка удаления несуществующего фильма с id: {}", id);
        }
    }

    @Override
    public boolean exists(int id) {
        boolean exists = films.containsKey(id);
        log.debug("Проверка существования фильма с id {}: {}", id, exists);
        return exists;
    }
}