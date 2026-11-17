package com.chalo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC customisation class.
 *
 * WHAT IT DOES
 *   Registers our CsrfTokenHandlerInterceptor so it runs on every web request.
 *
 * @Configuration tells Spring: "this class contains setup code, load it at startup".
 * Implementing WebMvcConfigurer lets us hook into Spring MVC without replacing its defaults.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Spring calls this once at startup so we can add interceptors.
     *
     * @param registry the list of interceptors Spring MVC will run around each controller call
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Make sure the CSRF token is loaded before the page body starts streaming.
        registry.addInterceptor(new CsrfTokenHandlerInterceptor());
    }
}
