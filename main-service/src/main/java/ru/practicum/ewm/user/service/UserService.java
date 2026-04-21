package ru.practicum.ewm.user.service;

import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto registerUser(NewUserRequest request);
    void deleteUser(Long userId);
    List<UserDto> getUsers(List<Long> ids, int from, int size);
}