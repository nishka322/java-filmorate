package ru.yandex.practicum.filmorate.storage.feed;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.service.DirectorService;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.ReviewService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.DirectorDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.film.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@JdbcTest
@Import({User.class, Film.class, UserService.class, FilmService.class, Event.class, FeedDbStorage.class, UserDbStorage.class, FilmDbStorage.class, EventRowMapper.class, MpaDbStorage.class, GenreDbStorage.class, DirectorService.class, DirectorDbStorage.class, Review.class, ReviewDbStorage.class, ReviewService.class})
public class FeedDbStorageTest {
    @Autowired
    private UserService userService;
    @Autowired
    private FilmService filmService;
    @Autowired
    private ReviewService reviewService;

    @Test
    public void createEventAddDeleteLikeEvent() {
        List<User> users = generateUsers(1);
        List<Film> films = generateFilms(2);

        filmService.addLike(films.get(0).getId(), users.get(0).getId());
        filmService.addLike(films.get(1).getId(), users.get(0).getId());

        filmService.removeLike(films.get(1).getId(), users.get(0).getId());

        List<Event> events = userService.getUserFeed(users.get(0).getId());

        Assertions.assertEquals(3, events.size());

        Assertions.assertEquals(FeedDbStorage.EventType.LIKE, events.get(0).getEventType());
        Assertions.assertEquals(FeedDbStorage.EventType.LIKE, events.get(1).getEventType());
        Assertions.assertEquals(FeedDbStorage.EventType.LIKE, events.get(2).getEventType());

        Assertions.assertEquals(FeedDbStorage.OperationType.ADD, events.get(0).getOperation());
        Assertions.assertEquals(FeedDbStorage.OperationType.ADD, events.get(1).getOperation());
        Assertions.assertEquals(FeedDbStorage.OperationType.REMOVE, events.get(2).getOperation());

        Assertions.assertEquals(films.get(0).getId(), events.get(0).getEntityId());
        Assertions.assertEquals(films.get(1).getId(), events.get(1).getEntityId());
        Assertions.assertEquals(films.get(1).getId(), events.get(2).getEntityId());
    }

    @Test
    public void createEventAddDeleteFriendEvent() {
        List<User> users = generateUsers(5);

        userService.addFriend(users.get(0).getId(), users.get(1).getId());
        userService.addFriend(users.get(0).getId(), users.get(2).getId());
        userService.addFriend(users.get(0).getId(), users.get(3).getId());
        userService.addFriend(users.get(0).getId(), users.get(4).getId());

        userService.removeFriend(users.get(0).getId(), users.get(2).getId());
        userService.removeFriend(users.get(0).getId(), users.get(3).getId());

        List<Event> events = userService.getUserFeed(users.get(0).getId());

        Assertions.assertEquals(6, events.size());

        Assertions.assertEquals(events.get(0).getEventType(), FeedDbStorage.EventType.FRIEND);
        Assertions.assertEquals(events.get(1).getEventType(), FeedDbStorage.EventType.FRIEND);
        Assertions.assertEquals(events.get(2).getEventType(), FeedDbStorage.EventType.FRIEND);
        Assertions.assertEquals(events.get(3).getEventType(), FeedDbStorage.EventType.FRIEND);
        Assertions.assertEquals(events.get(4).getEventType(), FeedDbStorage.EventType.FRIEND);
        Assertions.assertEquals(events.get(5).getEventType(), FeedDbStorage.EventType.FRIEND);

        Assertions.assertEquals(FeedDbStorage.OperationType.ADD, events.get(0).getOperation());
        Assertions.assertEquals(FeedDbStorage.OperationType.ADD, events.get(1).getOperation());
        Assertions.assertEquals(FeedDbStorage.OperationType.ADD, events.get(2).getOperation());
        Assertions.assertEquals(FeedDbStorage.OperationType.ADD, events.get(3).getOperation());
        Assertions.assertEquals(FeedDbStorage.OperationType.REMOVE, events.get(4).getOperation());
        Assertions.assertEquals(FeedDbStorage.OperationType.REMOVE, events.get(5).getOperation());

        Assertions.assertEquals(users.get(1).getId(), events.get(0).getEntityId());
        Assertions.assertEquals(users.get(2).getId(), events.get(1).getEntityId());
        Assertions.assertEquals(users.get(3).getId(), events.get(2).getEntityId());
        Assertions.assertEquals(users.get(4).getId(), events.get(3).getEntityId());
        Assertions.assertEquals(users.get(2).getId(), events.get(4).getEntityId());
        Assertions.assertEquals(users.get(3).getId(), events.get(5).getEntityId());
    }

    @Test
    public void createEventAddDeleteUpdateReviewsEvent() {
        List<User> users = generateUsers(1);
        List<Film> films = generateFilms(3);

        Review review1 = generateReview(users.get(0).getId(), films.get(0).getId());
        Review review2 = generateReview(users.get(0).getId(), films.get(1).getId());
        Review review3 = generateReview(users.get(0).getId(), films.get(2).getId());

        reviewService.updateReview(review2);

        reviewService.deleteReview(review3.getReviewId());

        List<Event> events = userService.getUserFeed(users.get(0).getId());

        Assertions.assertEquals(5, events.size());

        Assertions.assertEquals(events.get(0).getEventType(), FeedDbStorage.EventType.REVIEW);
        Assertions.assertEquals(events.get(1).getEventType(), FeedDbStorage.EventType.REVIEW);
        Assertions.assertEquals(events.get(2).getEventType(), FeedDbStorage.EventType.REVIEW);
        Assertions.assertEquals(events.get(3).getEventType(), FeedDbStorage.EventType.REVIEW);
        Assertions.assertEquals(events.get(4).getEventType(), FeedDbStorage.EventType.REVIEW);

        Assertions.assertEquals(FeedDbStorage.OperationType.ADD, events.get(0).getOperation());
        Assertions.assertEquals(FeedDbStorage.OperationType.ADD, events.get(1).getOperation());
        Assertions.assertEquals(FeedDbStorage.OperationType.ADD, events.get(2).getOperation());
        Assertions.assertEquals(FeedDbStorage.OperationType.UPDATE, events.get(3).getOperation());
        Assertions.assertEquals(FeedDbStorage.OperationType.REMOVE, events.get(4).getOperation());

        Assertions.assertEquals(review1.getReviewId(), events.get(0).getEntityId());
        Assertions.assertEquals(review2.getReviewId(), events.get(1).getEntityId());
        Assertions.assertEquals(review3.getReviewId(), events.get(2).getEntityId());
        Assertions.assertEquals(review2.getReviewId(), events.get(3).getEntityId());
        Assertions.assertEquals(review3.getReviewId(), events.get(4).getEntityId());
    }

    private List<User> generateUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0;
             i < count;
             i++) {
            User user = new User();
            user.setName("Name " + i);
            user.setLogin("name_" + i);
            user.setEmail("name_" + i + "@ya.ru");
            user.setBirthday(LocalDate.now());

            users.add(userService.createUser(user));
        }
        return users;
    }

    private List<Film> generateFilms(int count) {
        List<Film> films = new ArrayList<>();
        for (int i = 0;
             i < count;
             i++) {
            Film film = new Film();
            film.setName("Title " + i);
            film.setDescription("Title " + i + " description");
            Genre genre = new Genre();
            genre.setId(1);
            film.setGenres(Set.of(genre));
            MpaRating mpa = new MpaRating();
            mpa.setId(2);
            film.setMpa(mpa);
            film.setDuration(120);
            film.setReleaseDate(LocalDate.now());

            films.add(filmService.createFilm(film));
        }
        return films;
    }

    private Review generateReview(int userId, int filmId) {
        Review review = new Review();
        review.setUseful(1);
        review.setUserId(userId);
        review.setFilmId(filmId);
        review.setContent("Some content");
        review.setIsPositive(true);

        return reviewService.createReview(review);
    }
}
