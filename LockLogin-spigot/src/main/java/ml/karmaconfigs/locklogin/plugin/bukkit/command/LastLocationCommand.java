package ml.karmaconfigs.locklogin.plugin.bukkit.command;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.plugin.bukkit.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.LastLocation;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.properties;
import static ml.karmaconfigs.locklogin.plugin.bukkit.plugin.PluginPermission.account;
import static ml.karmaconfigs.locklogin.plugin.bukkit.plugin.PluginPermission.locations;

@SystemCommand(command = "lastloc", bungeecord = true)
public final class LastLocationCommand implements CommandExecutor {

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Message messages = new Message();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);
            ClientSession session = user.getSession();

            if (session.isValid()) {
                if (player.hasPermission(account())) {
                    if (args.length == 2) {
                        String target = args[0];
                        String action = args[1];
                        LastLocation location;

                        switch (target.toLowerCase()) {
                            case "@all":
                                switch (action.toLowerCase()) {
                                    case "remove":
                                        LastLocation.removeAll();
                                        user.send(messages.prefix() + messages.locationsReset());
                                        break;
                                    case "fix":
                                        LastLocation.fixAll();
                                        user.send(messages.prefix() + messages.locationsFixed());
                                        break;
                                    default:
                                        user.send(messages.prefix() + messages.resetLocUsage());
                                        break;
                                }
                                break;
                            case "@me":
                                location = new LastLocation(player);

                                switch (action.toLowerCase()) {
                                    case "remove":
                                        location.remove();
                                        user.send(messages.prefix() + messages.locationReset(StringUtils.stripColor(player.getDisplayName())));
                                        break;
                                    case "fix":
                                        location.fix();
                                        user.send(messages.prefix() + messages.locationFixed(StringUtils.stripColor(player.getDisplayName())));
                                        break;
                                    default:
                                        user.send(messages.prefix() + messages.resetLocUsage());
                                        break;
                                }
                                break;
                            default:
                                OfflineClient offline = new OfflineClient(target);
                                AccountManager manager = offline.getAccount();

                                if (manager != null) {
                                    location = new LastLocation(manager.getUUID());

                                    switch (action.toLowerCase()) {
                                        case "remove":
                                            location.remove();
                                            user.send(messages.prefix() + messages.locationReset(target));
                                            break;
                                        case "fix":
                                            location.fix();
                                            user.send(messages.prefix() + messages.locationFixed(target));
                                            break;
                                        default:
                                            user.send(messages.prefix() + messages.resetLocUsage());
                                            break;
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.neverPlayer(target));
                                }
                                break;
                        }
                    } else {
                        user.send(messages.prefix() + messages.resetLocUsage());
                    }
                } else {
                    user.send(messages.prefix() + messages.permissionError(locations()));
                }
            } else {
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            Console.send(messages.prefix() + properties.getProperty("console_is_restricted", "&5&oFor security reasons, this command is restricted to players only"));
        }

        return false;
    }
}