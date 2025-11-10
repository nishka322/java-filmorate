package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.film.DirectorDbStorage;

import java.util.List;

@Service
public class DirectorService {
    private final DirectorDbStorage directorStorage;
    private static final Logger log = LoggerFactory.getLogger(DirectorService.class);

    @Autowired
    public DirectorService(DirectorDbStorage directorStorage) {
        this.directorStorage = directorStorage;
    }

    public List<Director> getAllDirectors() {
        log.debug("Получение списка всех режиссеров");
        return directorStorage.getAll();
    }

    public Director getDirectorById(int id) {
        log.debug("Получение режиссера с id {}", id);
        return directorStorage.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Режиссер с id " + id + " не найден"));
    }

    public Director createDirector(Director director) {
        log.debug("Создание нового режиссера: {}", director.getName());
        Director createdDirector = directorStorage.create(director);
        log.info("Создан новый режиссер: '{}' (id: {})", createdDirector.getName(), createdDirector.getId());
        return createdDirector;
    }

    public boolean directorExists(int id) {
        log.debug("Проверка существования режиссера с id {}", id);
        return directorStorage.getById(id).isPresent();
    }
}
