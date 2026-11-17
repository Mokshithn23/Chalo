package com.chalo.service;

import com.chalo.dto.RegistrationForm;
import com.chalo.model.User;
import com.chalo.repository.UserRepository;
import com.chalo.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for creating a new account (sign-up).
 *
 * WHAT IS A SERVICE?
 *   Controllers handle web requests; repositories talk to the database; SERVICES sit in
 *   between and hold the "business rules" (e.g. "emails must be unique", "hash the password").
 *   Used by: AuthenticationController.processRegistration().
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    // Database access to the users table
    private final UserRepository        userRepository;
    // BCrypt hasher defined in SecurityConfig.passwordEncoder()
    private final BCryptPasswordEncoder passwordEncoder;

    // Instantiated directly — no Spring bean required for this single use case
    // Helper that knows how to store the "who is logged in" object inside the HTTP session.
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    // ── Registration ─────────────────────────────────────────────────────────

    /**
     * Validates, persists the new user, then immediately authenticates them
     * so the controller can redirect straight to /dashboard without a login hop.
     *
     * @Transactional = the database insert is all-or-nothing.
     *
     * @param registrationForm the already-validated sign-up data
     * @param request          current HTTP request (needed to create the session)
     * @param response         current HTTP response (needed to set the session cookie)
     * @throws EmailAlreadyInUseException if the email is already registered
     */
    @Transactional
    public void register(RegistrationForm registrationForm,
                         HttpServletRequest  request,
                         HttpServletResponse response) {

        // Normalise the email so "A@x.com " and "a@x.com" are the same account
        String email = registrationForm.getEmail().toLowerCase().trim();

        // Business rule: one account per email
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyInUseException(email);
        }

        // Build the entity. NEVER store the plain password: encode() hashes it with BCrypt.
        User user = User.builder()
                .name(registrationForm.getName().trim())
                .email(email)
                .age(registrationForm.getAge())
                .password(passwordEncoder.encode(registrationForm.getPassword()))
                .build();

        userRepository.save(user);
        // Log the new user in right away (nicer UX than asking them to type it again)
        autoLogin(user, request, response);
    }

    // ── Programmatic login (used after registration) ──────────────────────────

    /**
     * Builds a pre-authenticated token and stores it in the HTTP session.
     * Uses UsernamePasswordAuthenticationToken.authenticated() — the static
     * factory that marks the token as already authenticated, bypassing the
     * AuthenticationManager entirely (correct for post-registration auto-login).
     */
    private void autoLogin(User user,
                           HttpServletRequest  request,
                           HttpServletResponse response) {

        CustomUserDetails userDetails = new CustomUserDetails(user);

        UsernamePasswordAuthenticationToken authenticationToken =
                UsernamePasswordAuthenticationToken.authenticated(
                        userDetails,
                        null,                          // credentials cleared after auth
                        userDetails.getAuthorities()
                );

        // A SecurityContext is the "who is logged in" holder for the current request.
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authenticationToken);
        SecurityContextHolder.setContext(securityContext);

        // Persist the SecurityContext to the JSESSIONID session so subsequent
        // requests are recognised as authenticated.
        securityContextRepository.saveContext(securityContext, request, response);
    }

    // ── Exception ────────────────────────────────────────────────────────────

    /**
     * Thrown when someone registers with an email that already exists.
     * AuthenticationController catches it and shows "This email is already registered."
     */
    public static class EmailAlreadyInUseException extends RuntimeException {
        public EmailAlreadyInUseException(String email) {
            super("Email already registered: " + email);
        }
    }
}
