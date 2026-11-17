package com.chalo.model;

/**
 * Allowed values for a user's gender (stored as text in users.gender).
 *
 * Where used: User.gender. The registration form does not ask for it yet,
 * so it is currently always empty (null) - it is reserved for a future profile field.
 */
public enum Gender {
    MALE,
    FEMALE,
    OTHER,
    PREFER_NOT_TO_SAY
}
