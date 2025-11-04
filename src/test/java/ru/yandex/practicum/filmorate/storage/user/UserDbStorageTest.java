package ru.yandex.practicum.filmorate.storage.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({UserDbStorage.class})
class UserDbStorageTest {

    @Autowired
    private UserDbStorage userStorage;

    private User testUser;

    @BeforeEach
    public void setUp() {
        testUser = new User();
        testUser.setEmail("test@mail.ru");
        testUser.setLogin("testlogin");
        testUser.setName("Test User");
        testUser.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @Test
    public void testCreateUser() {
        User createdUser = userStorage.create(testUser);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isPositive();
        assertThat(createdUser.getEmail()).isEqualTo("test@mail.ru");
        assertThat(createdUser.getLogin()).isEqualTo("testlogin");
    }

    @Test
    public void testGetUserById() {
        User createdUser = userStorage.create(testUser);
        Optional<User> foundUser = userStorage.getById(createdUser.getId());

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@mail.ru");
    }

    @Test
    public void testGetAllUsers() {
        userStorage.create(testUser);

        User anotherUser = new User();
        anotherUser.setEmail("another@mail.ru");
        anotherUser.setLogin("anotherlogin");
        anotherUser.setName("Another User");
        anotherUser.setBirthday(LocalDate.of(1995, 1, 1));
        userStorage.create(anotherUser);

        List<User> users = userStorage.getAll();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getEmail)
                .containsExactlyInAnyOrder("test@mail.ru", "another@mail.ru");
    }

    @Test
    public void testUpdateUser() {
        User createdUser = userStorage.create(testUser);

        createdUser.setName("Updated Name");
        createdUser.setEmail("updated@mail.ru");

        User updatedUser = userStorage.update(createdUser);

        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@mail.ru");

        Optional<User> foundUser = userStorage.getById(createdUser.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("Updated Name");
    }

    @Test
    public void testUserExists() {
        User createdUser = userStorage.create(testUser);

        boolean exists = userStorage.exists(createdUser.getId());
        boolean notExists = userStorage.exists(999);

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    public void testDeleteUser() {
        User createdUser = userStorage.create(testUser);

        boolean existsBefore = userStorage.exists(createdUser.getId());
        userStorage.delete(createdUser.getId());
        boolean existsAfter = userStorage.exists(createdUser.getId());

        assertThat(existsBefore).isTrue();
        assertThat(existsAfter).isFalse();
    }
}