package com.ganchevdimitarg.bulgarianmountains.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class Config {

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

}
