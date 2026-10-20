package com.mediturn.patient.security;

/** Supplies the authenticated account ID; the authentication module provides its implementation. */
public interface CurrentUserProvider {

    Long getCurrentUserId();
}
