package ru.practicum.ewm.event.model;

public enum EventState {
    PENDING,    // ожидает модерации
    PUBLISHED,  // опубликовано
    CANCELED    // отменено
}