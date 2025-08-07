package com.ganchevdimitarg.bulgarianmountains.entites;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "huts")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class Hut {
    @Id
    private String id;
    private String name;
    private String host;
    private String description;
    private String atticFloor;
    private String mountainRoute;
    private Contact contact;
    private Location location;
    private List<String> startingPoints;
    private List<String> neighboringSites;
    private boolean isActive;
    private boolean isUnderRepair;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("name: ").append(name).append("\n");
        builder.append("host: ").append(host).append("\n");
        builder.append("description: ").append(description).append("\n");
        builder.append("atticFloor: ").append(atticFloor).append("\n");
        builder.append("mountainRoute: ").append(mountainRoute).append("\n");
        builder.append("contact: ").append(contact).append("\n");
        builder.append("location: ").append(location).append("\n");
        builder.append("startingPoints: ").append("\n");
        for (String startingPoint : startingPoints) {
            builder.append("- ").append(startingPoint).append("\n");
        }
        builder.append("neighboringSites: ").append("\n");
        for (String neighboringSite : neighboringSites) {
            builder.append("- ").append(neighboringSite).append("\n");
        }
        builder.append("isActive: ").append(isActive).append("\n");
        builder.append("isUnderRepair: ").append(isUnderRepair).append("\n");
        return builder.toString();
    }
}
