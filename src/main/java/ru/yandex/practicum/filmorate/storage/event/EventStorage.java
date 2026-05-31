package ru.yandex.practicum.filmorate.storage.event;

import ru.yandex.practicum.filmorate.model.Event;

import java.util.List;

public interface EventStorage {
    void addEvent(int userId, String eventType, String operation, int entityId);

    List<Event> findByUserId(int userId);
}
