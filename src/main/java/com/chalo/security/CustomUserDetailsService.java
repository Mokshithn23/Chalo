package com.chalo.security;

import com.chalo.model.User;
import com.chalo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tells Spring Security HOW TO FIND a user when somebody submits the login form.
 *
 * Login flow:
 *   1. Browser POSTs email + password to /login.
 *   2. Spring Security calls loadUserByUsername(email)  <- this class.
 *   3. We return a CustomUserDetails (containing the stored BCrypt hash).
 *   4. Spring Security hashes the typed password and compares it with the stored hash.
 *   5. Match -> logged in. No match / exception -> redirect to /login?error.
 *
 * Because it is a @Service and implements UserDetailsService, Spring Boot finds it
 * automatically; no extra configuration is needed.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    // Database access to the "users" table (injected by Spring through the Lombok constructor).
    private final UserRepository userRepository;

    /**
     * Called by Spring Security during form-login processing.
     * The "username" parameter is the email address because SecurityConfig
     * sets usernameParameter("email") on both form-login configurations.
     *
     * @param email what the user typed in the email box
     * @return the user's security details
     * @throws UsernameNotFoundException if no account has this email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Emails are stored lower-case and trimmed, so normalise before searching.
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() ->
                        new UsernameNotFoundException("No account found for: " + email));
        return new CustomUserDetails(user);
    }
}
