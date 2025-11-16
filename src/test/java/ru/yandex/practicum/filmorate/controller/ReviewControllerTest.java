package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.filmorate.exception.GlobalExceptionHandler;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.ReviewService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.film.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.sql.Connection;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewControllerTest {

    private MockMvc mockMvc;
    private UserService userService;
    private FilmService filmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("password");

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new org.springframework.core.io.ClassPathResource("schema.sql"));
            ScriptUtils.executeSqlScript(connection, new org.springframework.core.io.ClassPathResource("data.sql"));
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        UserDbStorage userDbStorage = new UserDbStorage(jdbcTemplate);
        userService = new UserService(userDbStorage);

        MpaDbStorage mpaDbStorage = new MpaDbStorage(jdbcTemplate);
        GenreDbStorage genreDbStorage = new GenreDbStorage(jdbcTemplate);
        FilmDbStorage filmDbStorage = new FilmDbStorage(jdbcTemplate);
        filmService = new FilmService(filmDbStorage, userService, mpaDbStorage, genreDbStorage, jdbcTemplate);

        ReviewDbStorage reviewDbStorage = new ReviewDbStorage(jdbcTemplate);
        ReviewService reviewService = new ReviewService(reviewDbStorage, userService, filmService);

        ReviewController reviewController = new ReviewController(reviewService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldCreateAndReturnReviewById() throws Exception {
        User user = createUser();
        Film film = createFilm();

        String payload = createReviewPayload("This film is soo bad.", false, user.getId(), film.getId());

        MvcResult createResult = mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").exists())
                .andExpect(jsonPath("$.useful").value(0))
                .andReturn();

        Review created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Review.class);
        assertThat(created.getReviewId()).isNotNull();

        mockMvc.perform(get("/reviews/{id}", created.getReviewId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(created.getReviewId()))
                .andExpect(jsonPath("$.content").value("This film is soo bad."))
                .andExpect(jsonPath("$.isPositive").value(false))
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.filmId").value(film.getId()))
                .andExpect(jsonPath("$.useful").value(0));
    }

    @Test
    void shouldReturn404WhenUserIdIsNotPositive() throws Exception {
        Film film = createFilm();

        String payload = createReviewPayload("Bad film.", false, -1, film.getId());

        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Необходимо указать идентификаторы пользователя и фильма"));
    }

    @Test
    void shouldReturn404WhenUserDoesNotExist() throws Exception {
        Film film = createFilm();
        int missingUserId = 999_999;

        String payload = createReviewPayload("User not found.", true, missingUserId, film.getId());

        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Пользователь с id " + missingUserId + " не найден"));
    }

    @Test
    void shouldReturn404WhenFilmDoesNotExist() throws Exception {
        User user = createUser();
        int missingFilmId = 888_888;

        String payload = createReviewPayload("Film not found.", true, user.getId(), missingFilmId);

        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Фильм с id " + missingFilmId + " не найден"));
    }

    private User createUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("tester" + System.nanoTime());
        user.setName("Tester");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userService.createUser(user);
    }

    private Film createFilm() {
        Film film = new Film();
        film.setName("Film " + System.nanoTime());
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        MpaRating rating = filmService.getMpaRatingById(1);
        film.setMpa(rating);
        return filmService.createFilm(film);
    }

    private String createReviewPayload(String content, boolean isPositive, int userId, int filmId) {
        return """
                {
                  \"content\": \"%s\",
                  \"isPositive\": %s,
                  \"userId\": %d,
                  \"filmId\": %d
                }
                """.formatted(content, isPositive, userId, filmId);
    }
}
