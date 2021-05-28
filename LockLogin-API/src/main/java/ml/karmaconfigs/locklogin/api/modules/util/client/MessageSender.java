package ml.karmaconfigs.locklogin.api.modules.util.client;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

/**
 * ModulePlayer message sender, this
 * contains the module player who should see
 * the message, and the message
 */
public class MessageSender {

    private final ModulePlayer target;
    private final String message;

    /**
     * Initialize the message sender
     *
     * @param tar the target
     * @param msg the message
     */
    public MessageSender(final ModulePlayer tar, final String msg) {
        target = tar;
        message = msg;
    }

    /**
     * Get the message target
     *
     * @return the message player
     */
    public final ModulePlayer getPlayer() {
        return target;
    }

    /**
     * Get the message
     *
     * @return the message
     */
    public final String getMessage() {
        return message;
    }
}
