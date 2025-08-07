package com.ganchevdimitarg.bulgarianmountains.entites;

import lombok.Builder;

import java.util.List;

@Builder
public record Contact(
        List<String> phone,
        String email,
        String url,
        String website) {
}
