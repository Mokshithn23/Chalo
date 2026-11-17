package com.chalo.model;

/**
 * State of a request to join someone's adventure.
 *
 *   PENDING   - the traveller asked, the host has not answered yet (default).
 *   ACCEPTED  - the host approved; the traveller now counts towards the filled slots
 *               and may use the crew chat.
 *   REJECTED  - the host declined.
 *
 * Where used: JoinRequest.status, JoinRequestController (accept/reject),
 * JoinRequestRepository (count/filter queries), CrewChatService.canAccess().
 */
public enum JoinRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
