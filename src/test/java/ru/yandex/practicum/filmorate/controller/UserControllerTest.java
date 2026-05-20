package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserControllerTest {
    private final UserController controller = new UserController(new UserService(new InMemoryUserStorage()));

    @Test
    void shouldCreateUserWithValidData() {
        User user = makeValidUser();

        User createdUser = controller.create(user);

        assertEquals(1, createdUser.getId());
    }

    @Test
    void shouldUseLoginWhenNameIsEmpty() {
        User user = makeValidUser();
        user.setName("");

        User createdUser = controller.create(user);

        assertEquals(user.getLogin(), createdUser.getName());
    }

    @Test
    void shouldRejectUserWithEmptyEmail() {
        User user = makeValidUser();
        user.setEmail("");

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldRejectUserWithEmailWithoutAtSign() {
        User user = makeValidUser();
        user.setEmail("user.example.com");

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldRejectUserWithLoginContainingSpaces() {
        User user = makeValidUser();
        user.setLogin("user login");

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldRejectUserWithFutureBirthday() {
        User user = makeValidUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldRejectEmptyUserRequest() {
        assertThrows(ValidationException.class, () -> controller.create(null));
    }

    @Test
    void shouldFindCommonFriends() {
        User firstUser = controller.create(makeValidUser());
        User secondUser = makeValidUser();
        secondUser.setEmail("second@example.com");
        secondUser.setLogin("second");
        secondUser = controller.create(secondUser);
        User commonFriend = makeValidUser();
        commonFriend.setEmail("friend@example.com");
        commonFriend.setLogin("friend");
        commonFriend = controller.create(commonFriend);

        controller.addFriend(firstUser.getId(), commonFriend.getId());
        controller.addFriend(secondUser.getId(), commonFriend.getId());

        assertEquals(commonFriend.getId(), controller.findCommonFriends(firstUser.getId(), secondUser.getId())
                .getFirst()
                .getId());
    }

    private User makeValidUser() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("user");
        user.setName("User");
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }
}
