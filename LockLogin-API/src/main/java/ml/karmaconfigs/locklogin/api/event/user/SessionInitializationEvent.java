package ml.karmaconfigs.locklogin.api.event.user;

import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.event.util.Event;
import org.bukkit.entity.Player;

public final class SessionInitializationEvent extends Event {

    private boolean handled = false;

    private final Player player;

    private final ClientSession session;
    private final Object event;

    /**
     * Initialize the session initialization event
     *
     * @param _player the player who is being initialized
     * @param _session the player session
     * @param _event the event owner
     */
    public SessionInitializationEvent(final Player _player, final ClientSession _session, final Object _event) {
        player = _player;
        session = _session;
        event = _event;
    }

    /**
     * Get the event player
     *
     * @return the event player
     */
    public final Player getPlayer() {
        return player;
    }

    /**
     * Get the player session
     *
     * @return the player session
     */
    public final ClientSession getSession() {
        return session;
    }

    /**
     * Set the event handle status
     *
     * @param status the handle status
     */
    @Override
    public void setHandled(boolean status) {
        handled = status;
    }

    /**
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    @Override
    public boolean isHandled() {
        return handled;
    }

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public Object getEvent() {
        return event;
    }
}
