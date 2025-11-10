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

        Film film1 = new Film();
        film1.setName("Крадущийся тигр");
        film1.setDescription("Боевик про тигра");
        film1.setReleaseDate(LocalDate.of(2000, 1, 1));
        film1.setDuration(120);
        film1.setMpa(mpa);
        filmStorage.create(film1);

        Film film2 = new Film();
        film2.setName("Крадущийся в ночи");
        film2.setDescription("Триллер");
        film2.setReleaseDate(LocalDate.of(2001, 1, 1));
        film2.setDuration(110);
        film2.setMpa(mpa);
        filmStorage.create(film2);

        Film film3 = new Film();
        film3.setName("Спящий дракон");
        film3.setDescription("Фэнтези");
        film3.setReleaseDate(LocalDate.of(2002, 1, 1));
        film3.setDuration(100);
        film3.setMpa(mpa);
        filmStorage.create(film3);

        Film film4 = new Film();
        film4.setName("КРАДУЩИЙСЯ ТИГР: ВОЗВРАЩЕНИЕ КОТА");
        film4.setDescription("Боевик в верхнем регистре");
        film4.setReleaseDate(LocalDate.of(2003, 1, 1));
        film4.setDuration(130);
        film4.setMpa(mpa);
        filmStorage.create(film4);
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

        MpaRating mpa = new MpaRating();
        mpa.setId(1);
        anotherFilm.setMpa(mpa);

        filmStorage.create(anotherFilm);

        List<Film> films = filmStorage.getAll();

        assertThat(films).hasSize(6);
        assertThat(films).extracting(Film::getName)
                .contains("Test Film", "Another Film");
        assertThat(films).extracting(Film::getName)
                .contains("Крадущийся тигр", "Крадущийся в ночи",
                        "Спящий дракон", "КРАДУЩИЙСЯ ТИГР: ВОЗВРАЩЕНИЕ КОТА");
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

    @Test
    public void testSearchFilmsByTitle() {
        List<Film> foundFilms = filmStorage.searchFilmsByTitle("крад");
        assertThat(foundFilms).hasSize(3);
        assertThat(foundFilms).extracting(Film::getName)
                .containsExactlyInAnyOrder("Крадущийся тигр", "Крадущийся в ночи", "КРАДУЩИЙСЯ ТИГР: ВОЗВРАЩЕНИЕ КОТА");

        List<Film> tigerFilms = filmStorage.searchFilmsByTitle("тигр");
        assertThat(tigerFilms).hasSize(2);
        assertThat(tigerFilms).extracting(Film::getName)
                .containsExactlyInAnyOrder("Крадущийся тигр", "КРАДУЩИЙСЯ ТИГР: ВОЗВРАЩЕНИЕ КОТА");

        List<Film> dragonFilms = filmStorage.searchFilmsByTitle("дракон");
        assertThat(dragonFilms).hasSize(1);
        assertThat(dragonFilms.getFirst().getName()).isEqualTo("Спящий дракон");

        List<Film> noFilms = filmStorage.searchFilmsByTitle("несуществующий");
        assertThat(noFilms).isEmpty();

        List<Film> allFilms = filmStorage.searchFilmsByTitle("");
        assertThat(allFilms).hasSize(4); // Только фильмы из setUp
        assertThat(allFilms).extracting(Film::getName)
                .containsExactlyInAnyOrder("Крадущийся тигр", "Крадущийся в ночи",
                        "Спящий дракон", "КРАДУЩИЙСЯ ТИГР: ВОЗВРАЩЕНИЕ КОТА");
    }
}