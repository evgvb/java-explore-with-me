package ru.practicum.ewm.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper mapper;

    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto dto) {
        log.info("Добавление новой категории: {}", dto.getName());

        checkCategoryNameUnique(dto.getName());
        Category category = mapper.toEntity(dto);
        Category saved = categoryRepository.save(category);
        log.debug("Категория добавлена с id={}", saved.getId());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Удаление категории id={}", catId);
        Category category = getCategoryById(catId);

        // проверяем, есть ли события, связанные с категорией
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Существуют события, связанные с категорией: id=" + catId);
        }
        categoryRepository.deleteById(catId);
        log.debug("Категория удалена");
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto dto) {
        log.info("Обновление категории id={}", catId);
        Category category = getCategoryById(catId);

        if (!category.getName().equals(dto.getName())) {
            checkCategoryNameUnique(dto.getName());
        }

        category.setName(dto.getName());
        Category updated = categoryRepository.save(category);
        log.debug("Категория обновлена");
        return mapper.toDto(updated);
    }

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        log.info("Получение списка категорий: from={}, size={}", from, size);
        return categoryRepository.findAll(PageRequest.of(from / size, size)).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        log.info("Получение категории id={}", catId);
        Category category = getCategoryById(catId);
        return mapper.toDto(category);
    }

    private void checkCategoryNameUnique(String name) {
        if (categoryRepository.existsByName(name)) {
            log.warn("Категория с именем '{}' уже существует", name);
            throw new ConflictException("Категория с именем '" + name + "' уже существует");
        }
    }

    private Category getCategoryById(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Категория не найдена: id=" + id));
    }
}