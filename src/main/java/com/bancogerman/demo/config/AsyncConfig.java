package com.bancogerman.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Async: los push se mandan fuera del request (la API no espera a Expo).
 * @Scheduled: revisión periódica de los recibos de Expo.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
