package ml.karmaconfigs.locklogin.plugin.common.utils.plugin;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ServerDataStorager {

    private static Set<String> key_registered = new LinkedHashSet<>();
    private static Set<String> proxy_registered = new LinkedHashSet<>();

    /**
     * Add a server name to the list of servers
     * which already have proxy key
     *
     * @param server the server name
     */
    public static void setKeyRegistered(final String server) {
        key_registered.add(server);
    }

    /**
     * Add a server name to the list of servers
     * which already have this proxy id
     *
     * @param server the server name
     */
    public static void setProxyRegistered(final String server) {
        proxy_registered.add(server);
    }

    /**
     * Remove a server name to the list of servers
     * which already have proxy key
     *
     * @param server the server name
     */
    public static void removeProxyRegistered(final String server) {
        proxy_registered.remove(server);
    }

    /**
     * Remove a server name to the list of servers
     * which already have this proxy id
     *
     * @param server the server name
     */
    public static void removeKeyRegistered(final String server) {
        key_registered.remove(server);
    }

    /**
     * Get if the server needs to retrieve a proxy key
     *
     * @param server the server name
     * @return if the server needs to know about the proxy key
     */
    public static boolean needsRegister(final String server) {
        return !key_registered.contains(server);
    }

    /**
     * Get if the server needs to know about this proxy
     * instance
     *
     * @param server the server name
     * @return if the server needs to know about this proxy
     */
    public static boolean needsProxyKnowledge(final String server) {
        return !proxy_registered.contains(server);
    }
}
