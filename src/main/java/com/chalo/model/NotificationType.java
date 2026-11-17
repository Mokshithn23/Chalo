package com.chalo.model;

/**
 * The reason a Notification was created. Stored as text in notifications.type.
 *
 *   REQUEST_RECEIVED    - sent to the HOST when someone asks to join
 *                         (AdventureDetailsController.submitJoinRequest).
 *   REQUEST_ACCEPTED    - sent to the TRAVELLER when the host accepts
 *                         (JoinRequestController.accept).
 *   REQUEST_REJECTED    - sent to the TRAVELLER when the host rejects
 *                         (JoinRequestController.reject).
 *   ADVENTURE_CANCELLED - sent to every ACCEPTED participant when the host cancels
 *                         (AdventureController.cancelAdventure).
 *
 * The notifications.html page uses it to choose an icon/colour for each item.
 */
public enum NotificationType {
    REQUEST_RECEIVED,
    REQUEST_ACCEPTED,
    REQUEST_REJECTED,
    ADVENTURE_CANCELLED
}
