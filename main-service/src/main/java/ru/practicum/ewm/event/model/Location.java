package ru.practicum.ewm.event.model;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Location {

    private Float lat;  // latitude широта
    private Float lon;  // longitude долгота
}