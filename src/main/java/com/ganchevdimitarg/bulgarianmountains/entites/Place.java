package com.ganchevdimitarg.bulgarianmountains.entites;

import lombok.Builder;

import java.util.Map;

@Builder
public record Place (String city, Map<String, String> description, String uri) {
}
