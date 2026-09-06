package com.voiceshield.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class VoiceShieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoiceShieldApplication.class, args);
    }
}