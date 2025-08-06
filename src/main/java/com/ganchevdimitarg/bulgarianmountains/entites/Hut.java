package com.ganchevdimitarg.bulgarianmountains.entites;

import com.mongodb.internal.connection.Time;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "huts")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class Hut {
    @Id
    private String id;
    private String city;
    private String name;
    private String uri;
    private Location location;
    private String description;
    private String neighboringSites;
    private List<String> images;
    private boolean isActive;
    private boolean isUnderRepair;


    @Override
    public String toString() {
        return """
                City: %s,
                Name: %s,
                URI: %s
                """.formatted(city, name, uri);
    }
}
