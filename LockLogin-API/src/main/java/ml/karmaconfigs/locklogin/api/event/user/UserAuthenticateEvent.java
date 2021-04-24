package ml.karmaconfigs.locklogin.api.event.user;

import ml.karmaconfigs.locklogin.api.event.util.Event;

/**
 * This event is fired when an user auths.
 */
public final class UserAuthenticateEvent extends Event {

    private boolean handled = false;

    private final AuthType auth_type;
    private final Object player;
    private final Result auth_result;
    private final String auth_message;

    private final Object eventObj;

    /**
     * Initialize the player auth event
     *
     * @param _auth_type the auth type
     * @param _auth_result the auth result
     * @param _player the player
     * @param _auth_message the auth message
     * @param event the event in where this event is fired
     */
    public UserAuthenticateEvent(final AuthType _auth_type, final Result _auth_result, final Object _player, final String _auth_message, final Object event) {
        auth_type = _auth_type;
        auth_result = _auth_result;
        player = _player;
        auth_message = _auth_message;

        eventObj = event;
    }

    /**
     * Get the event player
     *
     * @return the event player
     */
    public final Object getPlayer() {
        return player;
    }

    /**
     * Get the auth type
     *
     * @return the auth type
     */
    public final AuthType getAuthType() {
        return auth_type;
    }

    /**
     * Get the auth result
     *
     * @return the auth result
     */
    public final Result getAuthResult() {
        return auth_result;
    }

    /**
     * Get the auth message
     *
     * @return the auth message
     */
    public final String getAuthMessage() {
        return auth_message;
    }

    /**
     * Set the event handle status
     *
     * @param status the handle status
     */
    @Override
    public final void setHandled(boolean status) {
        handled = status;
    }

    /**
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    @Override
    public final boolean isHandled() {
        return handled;
    }

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public final Object getEvent() {
        return eventObj;
    }

    /**
     * LockLogin available auth types
     */
    public enum AuthType {
        /**
         * LockLogin valid auth type
         */
        PASSWORD,
        /**
         * LockLogin valid auth type
         */
        PIN,
        /**
         * LockLogin valid auth type
         */
        FA_2,
        /**
         * LockLogin valid auth type
         */
        API
    }

    /**
     * LockLogin valid auth results
     * for this event
     */
    public enum Result {
        /**
         * LockLogin auth event result waiting for validation
         */
        WAITING,
        /**
         * LockLogin auth event result failed validation
         */
        FAILED,
        /**
         * LockLogin auth event result validation success
         */
        SUCCESS,
        /**
         * LockLogin auth event result validation success but has 2fa or pin
         */
        SUCCESS_TEMP,
        /**
         * LockLogin auth event result something went wrong
         */
        ERROR
    }
}
