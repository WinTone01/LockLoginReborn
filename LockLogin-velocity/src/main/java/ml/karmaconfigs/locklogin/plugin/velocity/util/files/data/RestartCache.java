package ml.karmaconfigs.locklogin.plugin.velocity.util.files.data;

import com.velocitypowered.api.proxy.Player;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.velocity.Console;
import ml.karmaconfigs.api.velocity.KarmaFile;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.plugin.common.JarManager;
import ml.karmaconfigs.locklogin.plugin.velocity.listener.MessageListener;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.*;

public final class RestartCache {

    private final KarmaFile cache = new KarmaFile(plugin, "plugin.cache", "plugin", "updater", "cache");

    /**
     * Store the sessions into the cache file
     */
    public final void storeUserData() {
        if (!cache.exists())
            cache.create();

        Map<UUID, ClientSession> sessions = User.getSessionMap();
        String sessions_serialized = serialize(sessions);

        if (sessions_serialized != null) {
            cache.set("SESSIONS", sessions_serialized);
        } else {
            Console.send(plugin, properties.getProperty("plugin_error_cache_save", "Failed to save cache object {0} ( {1} )"), Level.GRAVE, "sessions", "sessions are null");
        }
    }

    /**
     * Store bungeecord key so a fake bungeecord server
     * won't be able to send a fake key
     */
    public final void storeBungeeKey() {
        if (!cache.exists())
            cache.create();

        try {
            Class<?> storagerClass = DataSender.class;
            Field keyField = storagerClass.getDeclaredField("key");

            String key = (String) keyField.get(null);
            cache.set("KEY", key);
        } catch (Throwable ignored) {
        }

        try {
            Class<?> listenerClass = MessageListener.class;
            Field tokenField = listenerClass.getDeclaredField("token");

            String token = (String) tokenField.get(null);
            cache.set("TOKEN", token);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Load the stored sessions
     */
    public final void loadUserData() {
        if (cache.exists()) {
            String sessions_serialized = cache.getString("SESSIONS", "");

            if (!sessions_serialized.replaceAll("\\s", "").isEmpty()) {
                Map<UUID, ClientSession> sessions = unSerializeMap(sessions_serialized);
                Map<UUID, ClientSession> fixedSessions = new HashMap<>();
                if (sessions != null) {
                    //Remove offline player sessions to avoid security issues
                    for (UUID id : sessions.keySet()) {
                        ClientSession session = sessions.getOrDefault(id, null);
                        if (session != null) {
                            Optional<Player> player = server.getPlayer(id);
                            if (player.isPresent() && player.get().isActive()) {
                                fixedSessions.put(id, session);
                            }
                        }
                    }

                    try {
                        JarManager.changeField(User.class, "sessions", true, fixedSessions);
                    } catch (Throwable ex) {
                        Console.send(plugin, properties.getProperty("plugin_error_cache_load", "Failed to load cache object {0} ( {1} )"), Level.GRAVE, "sessions", ex.fillInStackTrace());
                    }
                } else {
                    Console.send(plugin, properties.getProperty("plugin_error_cache_load", "Failed to load cache object {0} ( {1} )"), Level.GRAVE, "sessions", "session map is null");
                }
            }
        }
    }

    /**
     * Load the stored bungeecord key
     */
    public final void loadBungeeKey() {
        if (cache.exists()) {
            try {
                String key = cache.getString("KEY", "");

                if (!key.replaceAll("\\s", "").isEmpty()) {
                    JarManager.changeField(DataSender.class, "key", true, key);
                }
            } catch (Throwable ignored) {
            }

            try {
                String token = cache.getString("TOKEN", "");

                if (!token.replaceAll("\\s", "").isEmpty()) {
                    JarManager.changeField(MessageListener.class, "token", true, token);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Remove the cache file
     */
    public final void remove() {
        try {
            Files.delete(cache.getFile().toPath());
        } catch (Throwable ignored) {
        }
    }

    /**
     * Serialize the object into a string
     *
     * @param object the object to serialize
     * @return the serialized object
     */
    @Nullable
    private String serialize(final Object object) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ObjectOutputStream obj_out = new ObjectOutputStream(output);
            obj_out.writeObject(object);
            obj_out.flush();
            return Base64.getEncoder().encodeToString(output.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Un-serialize the object
     *
     * @param serialized the serialized object
     * @param <K>        the map key type
     * @param <V>        the map value type
     * @return the un-serialized object
     */
    @Nullable
    private <K, V> Map<K, V> unSerializeMap(String serialized) {
        try {
            serialized = new String(Base64.getDecoder().decode(serialized.getBytes(StandardCharsets.UTF_8)));
            ByteArrayInputStream input = new ByteArrayInputStream(serialized.getBytes(StandardCharsets.UTF_8));
            ObjectInputStream obj_input = new ObjectInputStream(input);
            Object obj = obj_input.readObject();

            if (obj instanceof Map) {
                Map<K, V> returnMap = new HashMap<>();
                Map<?, ?> map = (Map<?, ?>) obj;

                for (Object key : map.keySet()) {
                    returnMap.put((K) key, (V) map.get(key));
                }

                return returnMap;
            }

            return null;
        } catch (Throwable ex) {
            return null;
        }
    }
}