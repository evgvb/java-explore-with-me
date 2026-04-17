package ru.practicum.ewm.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;

    @Override
    @Transactional
    public UserDto registerUser(NewUserRequest request) {
        log.info("Регистрация нового пользователя: email={}", request.getEmail());
        User user = mapper.toEntity(request);
        User saved = userRepository.save(user);
        log.debug("Пользователь зарегистрирован с id={}", saved.getId());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Удаление пользователя id={}", userId);
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        userRepository.deleteById(userId);
        log.debug("Пользователь удалён");
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        log.info("Получение пользователей: ids={}, from={}, size={}", ids, from, size);
        PageRequest page = PageRequest.of(from / size, size);
        List<User> users;
        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(page).getContent();
        } else {
            users = userRepository.findByIdIn(ids, page);
        }
        return users.stream().map(mapper::toDto).collect(Collectors.toList());
    }
}