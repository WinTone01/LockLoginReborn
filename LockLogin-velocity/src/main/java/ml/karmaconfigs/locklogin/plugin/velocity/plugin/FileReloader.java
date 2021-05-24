package ml.karmaconfigs.locklogin.plugin.velocity.plugin;

import com.velocitypowered.api.proxy.Player;
import ml.karmaconfigs.api.velocity.Console;
import ml.karmaconfigs.locklogin.api.modules.api.event.plugin.PluginStatusChangeEvent;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.plugin.velocity.permissibles.PluginPermission;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.Config;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.properties;
import static ml.karmaconfigs.locklogin.plugin.velocity.permissibles.PluginPermission.reload_config;
import static ml.karmaconfigs.locklogin.plugin.velocity.permissibles.PluginPermission.reload_messages;

public class FileReloader {

    /**
     * Reload the plugin files and other
     *
     * @param player the player that called the action
     */
    public static void reload(final Player player) {
        PluginStatusChangeEvent reload_start = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.RELOAD_START, null);
        JavaModuleManager.callEvent(reload_start);

        Message message = new Message();
        if (player != null) {
            User user = new User(player);

            if (user.hasPermission(reload_config()) || user.hasPermission(reload_messages())) {
                if (user.hasPermission(reload_config())) {
                    if (Config.manager.reload()) {
                        Config.manager.checkValues();

                        user.send(message.prefix() + properties.getProperty("reload_config", "&aReloaded config file"));
                    }
                }
                if (user.hasPermission(reload_messages())) {
                    if (Message.manager.reload()) {
                        user.send(message.prefix() + properties.getProperty("reload_messages", "&aReloaded messages file"));
                    }
                }

                if (user.hasPermission(PluginPermission.reload())) {
                    user.send(message.prefix() + properties.getProperty("restart_systems", "&7Restarting version checker and plugin alert systems"));

                    Manager.restartVersionChecker();
                    Manager.restartAlertSystem();
                }
            } else {
                user.send(message.prefix() + message.permissionError(PluginPermission.reload()));
            }
        } else {
            if (Config.manager.reload()) {
                Config.manager.checkValues();

                Console.send(message.prefix() + properties.getProperty("reload_config", "&aReloaded config file"));
            }
            if (Message.manager.reload()) {
                Console.send(message.prefix() + properties.getProperty("reload_messages", "&aReloaded messages file"));
            }

            Console.send(message.prefix() + properties.getProperty("restart_systems", "&7Restarting version checker and plugin alert systems"));

            Manager.restartVersionChecker();
            Manager.restartAlertSystem();
        }

        PluginStatusChangeEvent reload_finish = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.RELOAD_END, null);
        JavaModuleManager.callEvent(reload_finish);
    }
}