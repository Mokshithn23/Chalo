package com.chalo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SECURITY RULES of the whole application: "who may open which URL?"
 *
 * Spring Security puts a chain of filters in front of every request. This class
 * configures those filters. There are TWO separate chains:
 *
 *   Chain 1 (@Order(1)) -> only for URLs starting with /admin/**   (admins)
 *   Chain 2 (@Order(2)) -> every other URL                         (normal users + guests)
 *
 * Spring checks chain 1 first; if the URL does not start with /admin it falls to chain 2.
 *
 * ROLES
 *   A logged-in person gets exactly one role from CustomUserDetails.getAuthorities():
 *     ROLE_USER  - normal account
 *     ROLE_ADMIN - account with is_admin = true in the database
 *   In rules below, hasRole("USER") automatically means "ROLE_USER".
 *
 * IMPORTANT RULE OF SPRING SECURITY: rules are matched TOP TO BOTTOM and the FIRST
 * match wins. That is why specific URLs are written before general ones.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ── Shared beans ─────────────────────────────────────────────────────────

    /**
     * Password hasher. BCrypt turns "mypassword" into a long irreversible hash
     * (with a random "salt"), so even the database owner cannot read passwords.
     *
     * Used in: AuthenticationService.register() to hash a new password, and by
     * Spring Security automatically at login to compare the typed password with
     * the stored hash.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── Admin filter chain ───────────────────────────────────────────────────
    // Intercepts /admin/** only. Must be ordered before the user chain.
    // Requires ROLE_ADMIN on all routes except the login page itself.

    /**
     * Security rules for the admin area (/admin/**).
     *
     * NOTE: only the admin LOGIN page exists today (AuthenticationController +
     * templates/admin/login.html). The admin dashboard itself ("/admin") is not built yet.
     *
     * @param http Spring's builder object used to describe the rules
     * @return the finished filter chain
     * @throws Exception required by the HttpSecurity API
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            // This chain only applies to URLs that start with /admin/
            .securityMatcher("/admin/**")
            .authorizeHttpRequests(auth -> auth
                // Anyone (even not logged in) may see the admin login page
                .requestMatchers("/admin/login").permitAll()
                // Everything else under /admin needs ROLE_ADMIN
                .anyRequest().hasRole("ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/admin/login")              // our own login page (GET)
                .loginProcessingUrl("/admin/login")     // where the form POSTs; Spring handles it
                .usernameParameter("email")             // form field name for the username
                .passwordParameter("password")          // form field name for the password
                .defaultSuccessUrl("/admin", true)      // after success always go to /admin
                .failureUrl("/admin/login?error")       // after failure show the error message
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout")
                .invalidateHttpSession(true)            // destroy the server-side session
                .deleteCookies("JSESSIONID")            // remove the session cookie
            );
        return http.build();
    }

    // ── User filter chain ────────────────────────────────────────────────────
    // Handles all routes not matched by the admin chain.
    // Public discovery routes are explicitly permitted so non-logged-in users
    // can browse adventures (experience-first product philosophy).

    /**
     * Security rules for everything that is NOT /admin/**.
     *
     * @param http Spring's builder object used to describe the rules
     * @return the finished filter chain
     * @throws Exception required by the HttpSecurity API
     */
    @Bean
    @Order(2)
    public SecurityFilterChain userFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth

                // Static assets (css/js/images folders). Open to everybody.
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                // Auth pages: login + registration must be reachable when logged out
                .requestMatchers("/login", "/register").permitAll()

                // Public discovery — anyone can browse
                .requestMatchers("/", "/explore").permitAll()
                .requestMatchers("/api/locations/suggest", "/api/tags").permitAll()

                // Adventure management routes must be listed BEFORE the broad
                // GET permit below, because Spring Security matches in order.
                .requestMatchers(
                    "/adventures/new",
                    "/adventures/*/edit",
                    "/adventures/*/requests",
                    "/adventures/*/requests/**",
                    "/adventures/*/chat",
                    "/adventures/*/chat/**"
                ).hasRole("USER")

                // Join submission requires a USER session (must be before the broad permitAll below)
                .requestMatchers(HttpMethod.POST, "/adventures/*/join").hasRole("USER")

                // Adventure detail and host profile are public browsing
                // ("/adventures/*" = exactly one path segment, e.g. /adventures/5)
                .requestMatchers("/adventures/*", "/host/**").permitAll()

                // All personal / transactional sections require a USER session
                .requestMatchers(
                    "/dashboard/**",
                    "/profile/**",
                    "/my-adventures/**",
                    "/chat/**"
                ).hasRole("USER")

                // Catch-all: anything not explicitly permitted requires USER
                // (this also protects /my-crews, /notifications/**, /requests/**,
                //  POST /adventures, POST /adventures/{id}/cancel and /join-request)
                .anyRequest().hasRole("USER")
            )
            .formLogin(form -> form
                .loginPage("/login")                    // our own login page (GET /login)
                .loginProcessingUrl("/login")           // Spring handles POST /login itself
                .usernameParameter("email")             // users log in with email, not username
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)  // always land on the dashboard
                .failureUrl("/login?error")             // wrong credentials -> error banner
            )
            .logout(logout -> logout
                .logoutUrl("/logout")                   // must be a POST form (CSRF protected)
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            );
        return http.build();
    }
}
