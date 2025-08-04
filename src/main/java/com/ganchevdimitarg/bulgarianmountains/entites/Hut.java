package com.ganchevdimitarg.bulgarianmountains.entites;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
}
