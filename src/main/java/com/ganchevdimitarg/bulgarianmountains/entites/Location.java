package com.ganchevdimitarg.bulgarianmountains.entites;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "location")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class Location {
    @Id
    private String id;
    private String altitue;
    private String coordinates;
    private String description;
}
