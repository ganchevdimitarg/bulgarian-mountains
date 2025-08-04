package com.ganchevdimitarg.bulgarianmountains.repositories;

import com.ganchevdimitarg.bulgarianmountains.entites.Hut;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HutRepository extends MongoRepository<Hut, String> {
    
}
