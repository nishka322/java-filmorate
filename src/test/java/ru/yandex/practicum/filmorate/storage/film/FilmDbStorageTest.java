package ru.yandex.practicum.filmorate.storage.film;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.Genre;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({FilmDbStorage.class})
class FilmDbStorageTest {

    @Autowired
    private FilmDbStorage filmStorage;

    private Film testFilm;

    @BeforeEach
    public void setUp() {
        testFilm = new Film();
        testFilm.setName("Test Film");
        testFilm.setDescription("Test Description");
        testFilm.setReleaseDate(LocalDate.of(2020, 1, 1));
        testFilm.setDuration(120);

        MpaRating mpa = new MpaRating();
        mpa.setId(1);
        testFilm.setMpa(mpa);
    }

    @Test
    public void testCreateFilm() {
        Film createdFilm = filmStorage.create(testFilm);

        assertThat(createdFilm).isNotNull();
        assertThat(createdFilm.getId()).isPositive();
        assertThat(createdFilm.getName()).isEqualTo("Test Film");
        assertThat(createdFilm.getDuration()).isEqualTo(120);
    }

    @Test
    public void testGetFilmById() {
        Film createdFilm = filmStorage.create(testFilm);
        Optional<Film> foundFilm = filmStorage.getById(createdFilm.getId());

        assertThat(foundFilm).isPresent();
        assertThat(foundFilm.get().getName()).isEqualTo("Test Film");
    }

    @Test
    public void testGetAllFilms() {
        filmStorage.create(testFilm);

        Film anotherFilm = new Film();
        anotherFilm.setName("Another Film");
        anotherFilm.setDescription("Another Description");
        anotherFilm.setReleaseDate(LocalDate.of(2021, 1, 1));
        anotherFilm.setDuration(90);
        filmStorage.create(anotherFilm);

        List<Film> films = filmStorage.getAll();

        assertThat(films).hasSize(2);
        assertThat(films).extracting(Film::getName)
                .containsExactlyInAnyOrder("Test Film", "Another Film");
    }

    @Test
    public void testUpdateFilm() {
        Film createdFilm = filmStorage.create(testFilm);

        createdFilm.setName("Updated Film");
        createdFilm.setDuration(150);

        Film updatedFilm = filmStorage.update(createdFilm);

        assertThat(updatedFilm.getName()).isEqualTo("Updated Film");
        assertThat(updatedFilm.getDuration()).isEqualTo(150);

        Optional<Film> foundFilm = filmStorage.getById(createdFilm.getId());
        assertThat(foundFilm).isPresent();
        assertThat(foundFilm.get().getName()).isEqualTo("Updated Film");
    }

    @Test
    public void testFilmExists() {
        Film createdFilm = filmStorage.create(testFilm);

        boolean exists = filmStorage.exists(createdFilm.getId());
        boolean notExists = filmStorage.exists(999);

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    public void testDeleteFilm() {
        Film createdFilm = filmStorage.create(testFilm);

        boolean existsBefore = filmStorage.exists(createdFilm.getId());
        filmStorage.delete(createdFilm.getId());
        boolean existsAfter = filmStorage.exists(createdFilm.getId());

        assertThat(existsBefore).isTrue();
        assertThat(existsAfter).isFalse();
    }

    @Test
    public void testGetAllMpaRatings() {
        List<MpaRating> mpaRatings = filmStorage.getAllMpaRatings();

        assertThat(mpaRatings).isNotEmpty();
        assertThat(mpaRatings).extracting(MpaRating::getName)
                .contains("G", "PG", "PG-13", "R", "NC-17");
    }

    @Test
    public void testGetAllGenres() {
        List<Genre> genres = filmStorage.getAllGenres();

        assertThat(genres).isNotEmpty();
        assertThat(genres).extracting(Genre::getName)
                .contains("Комедия", "Драма", "Боевик");
    }
}