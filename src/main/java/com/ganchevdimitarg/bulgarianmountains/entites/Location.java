package com.ganchevdimitarg.bulgarianmountains.entites;

import lombok.Builder;

@Builder
public record Location(
        String altitude,
        String coordinates,
        String description) {
}
