# Persistent Authentication Attempts

## Intent

Keep the existing five-attempt, 30-second credential lockout effective across app-process restarts, restore remaining attempts and lockout time in the unlock UI, and keep Security dependent on persistence only through Domain contracts.

## Design

- Domain defines `AuthenticationAttemptState`, `AuthenticationAttemptStatus`, the atomic `AuthenticationAttemptStore.update` port, and `AuthenticationAttemptStatusProvider.status`.
- Data persists per-method state in the existing Security Preferences Proto DataStore. Updates are atomic; method keys are explicit stable values.
- Security owns the five-attempt policy. It durably reserves each submitted credential attempt before KDF verification, then clears the count on a verified credential or finalizes the lockout on a wrong credential. It persists wall-clock and elapsed-realtime lockout deadlines plus Android boot count. It uses elapsed realtime during the same boot and wall time after reboot. A storage error returns an authentication failure and never proceeds to KDF verification.
- Passwords and recovery codes are never persisted.
- The app unlock screen reloads per-method status, shows attempts remaining inline, and refreshes a lockout countdown once per second by re-reading the Domain status provider. It does not use transient Toasts for the primary status.

## Limits

This protects app-mediated attempts against process restarts. It cannot prevent an attacker with control of app-private storage or a copied encrypted vault from changing local state or guessing offline; the KDF remains the offline-guessing defense.

## Approaches

Use the existing Proto DataStore through a Domain port. A new Gradle module is unnecessary for this small state, and direct Security-to-Data dependencies violate the agreed boundary.
