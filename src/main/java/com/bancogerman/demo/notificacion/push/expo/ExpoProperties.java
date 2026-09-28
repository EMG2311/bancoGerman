package com.bancogerman.demo.notificacion.push.expo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param url         base de la API de push de Expo
 * @param accessToken opcional; obligatorio solo si se activa "enhanced push security" en el panel de EAS
 */
@ConfigurationProperties("app.push.expo")
public record ExpoProperties(String url, String accessToken) {
}
