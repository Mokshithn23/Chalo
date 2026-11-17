package com.chalo.security;

import com.chalo.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * The "logged-in person" object that Spring Security keeps in the HTTP session.
 *
 * Spring Security does not know our User entity. It only understands the interface
 * UserDetails (username, password, roles, enabled?). This class is the adapter that
 * translates our User into that interface.
 *
 * Stores only scalar fields from User so the HTTP session is safely
 * serializable — no JPA entity or lazy-loaded proxy is held in session.
 *
 * Where used:
 *   - CustomUserDetailsService creates it at login.
 *   - AuthenticationService.autoLogin() creates it after registration.
 *   - Every controller receives it via  @AuthenticationPrincipal CustomUserDetails principal
 *     and calls principal.getId() / principal.getName().
 */
public class CustomUserDetails implements UserDetails, Serializable {

    // Version number required for Java serialization (sessions can be written to disk).
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long   id;         // users.id
    private final String name;       // display name
    private final String email;      // login id
    private final String password;   // BCrypt hash
    private final boolean admin;     // true -> ROLE_ADMIN

    /** Copies the needed values out of the database entity. */
    public CustomUserDetails(User user) {
        this.id       = user.getId();
        this.name     = user.getName();
        this.email    = user.getEmail();
        this.password = user.getPassword();
        this.admin    = user.isAdmin();
    }

    // ── Convenience accessors for controllers ────────────────────────────────

    /** Database id of the logged-in user. */
    public Long getId()   { return id; }

    /** Display name of the logged-in user (shown in the header). */
    public String getName() { return name; }

    // ── UserDetails ──────────────────────────────────────────────────────────

    /** The role(s) of the user: ROLE_ADMIN for admins, ROLE_USER for everybody else. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = admin ? "ROLE_ADMIN" : "ROLE_USER";
        return List.of(new SimpleGrantedAuthority(role));
    }

    /** The stored BCrypt hash, which Spring Security compares with the typed password. */
    @Override
    public String getPassword() { return password; }

    /** Spring Security treats this as the unique login identifier. */
    @Override
    public String getUsername() { return email; }

    // We do not support expiring/locking/disabling accounts yet, so all of these are "true".
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
