package ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.modules.api.channel.ModuleMessageService;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee.data.AccountParser;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee.data.BungeeDataStorager;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.Config;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.LastLocation;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.Spawn;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.AltAccountsInventory;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.PinInventory;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.PlayersInfoInventory;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.ClientVisor;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.security.client.IpData;
import ml.karmaconfigs.locklogin.plugin.common.utils.DataType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.logger;
import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.plugin;

@SuppressWarnings("UnstableApiUsage")
public final class BungeeReceiver implements PluginMessageListener {

    /**
     * Listens for incoming plugin messages
     *
     * @param channel the channel
     * @param player  the player used to send the message
     * @param bytes   the message bytes
     */
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] bytes) {
        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
        try {
            DataType sub = DataType.valueOf(input.readUTF().toUpperCase());
            UUID id = UUID.fromString(input.readUTF());
            String key = input.readUTF();
            String clientUUID = input.readUTF();
            try {
                player = plugin.getServer().getPlayer(UUID.fromString(clientUUID));
            } catch (Throwable ignored) {}

            BungeeDataStorager storager = new BungeeDataStorager(key);

            if (!channel.equalsIgnoreCase("ll:access")) {
                if (storager.validate(id)) {
                    User user = new User(player);
                    ClientSession session = user.getSession();
                    PluginConfiguration config = CurrentPlatform.getConfiguration();

                    switch (channel.toLowerCase()) {
                        case "ll:account":
                            switch (sub) {
                                case JOIN:
                                    if (!user.isLockLoginUser()) {
                                        user.applyLockLoginUser();
                                    }
                                    session.validate();

                                    if (config.enableSpawn()) {
                                        Spawn spawn = new Spawn(player.getWorld());
                                        spawn.teleport(player);
                                    }

                                    session.setLogged(input.readBoolean());
                                    session.set2FALogged(input.readBoolean());
                                    session.setPinLogged(input.readBoolean());
                                    user.setRegistered(input.readBoolean());

                                    break;
                                case QUIT:
                                    InetSocketAddress ip = player.getAddress();
                                    if (user.isLockLoginUser()) {
                                        if (ip != null) {
                                            IpData data = new IpData(ip.getAddress());
                                            data.delClone();
                                        }

                                        //Last location will be always saved since if the server
                                        //owner wants to enable it, it would be good to see
                                        //the player last location has been stored to avoid
                                        //location problems
                                        LastLocation last_loc = new LastLocation(player);
                                        last_loc.save();

                                        session.invalidate();
                                        session.setLogged(false);
                                        session.setPinLogged(false);
                                        session.set2FALogged(false);

                                        user.removeLockLoginUser();
                                    }

                                    user.setTempSpectator(false);
                                    break;
                                case VALIDATION:
                                    session.validate();
                                    break;
                                case CAPTCHA:
                                    if (!session.isCaptchaLogged()) {
                                        session.setCaptchaLogged(true);
                                    }
                                    break;
                                case SESSION:
                                    if (!session.isLogged()) {
                                        session.setLogged(true);
                                    }

                                    if (config.takeBack()) {
                                        LastLocation location = new LastLocation(player);
                                        location.teleport();
                                    }
                                    break;
                                case PIN:
                                    String pin_sub = input.readUTF();

                                    switch (pin_sub.toLowerCase()) {
                                        case "open":
                                            if (!session.isPinLogged()) {
                                                PinInventory pin = new PinInventory(player);
                                                pin.open();
                                            }
                                            break;
                                        case "close":
                                            if (player.getOpenInventory().getTopInventory().getHolder() instanceof PinInventory) {
                                                PinInventory inventory = new PinInventory(player);
                                                inventory.close();
                                            }
                                            session.setPinLogged(true);
                                            break;
                                        default:
                                            logger.scheduleLog(Level.GRAVE, "Unknown pin sub channel: " + pin_sub);
                                            break;
                                    }
                                    break;
                                case GAUTH:
                                    session.set2FALogged(true);

                                    ClientVisor visor = new ClientVisor(player);
                                    visor.unVanish();
                                    visor.checkVanish();
                                    break;
                                case CLOSE:
                                    if (session.isLogged()) {
                                        session.setLogged(false);
                                        session.set2FALogged(false);
                                        session.setPinLogged(false);
                                    }

                                    break;
                                case EFFECTS:
                                    boolean status = input.readBoolean();

                                    if (status) {
                                        user.savePotionEffects();
                                        user.applySessionEffects();
                                    } else {
                                        user.restorePotionEffects();
                                    }

                                    break;
                                case INVALIDATION:
                                    session.invalidate();
                                    break;
                                default:
                                    logger.scheduleLog(Level.GRAVE, "Unknown account sub channel message: " + sub);
                                    break;
                            }
                            break;
                        case "ll:plugin":
                            switch (sub) {
                                case MESSAGES:
                                    String messageString = input.readUTF();
                                    Message.manager.loadBungee(messageString);
                                    break;
                                case CONFIG:
                                    String configString = input.readUTF();
                                    Config.manager.loadBungee(configString);
                                    break;
                                case LOGGED:
                                    storager.setLoggedAccounts(input.readInt());
                                    break;
                                case REGISTERED:
                                    storager.setRegisteredAccounts(input.readInt());
                                    break;
                                case INFOGUI:
                                    String infoMessage = input.readUTF();
                                    infoMessage = StringUtils.replaceLast(infoMessage.replaceFirst("AccountParser\\(", ""), "\\)", "");

                                    String[] infoData = infoMessage.split(";");
                                    Set<AccountManager> infoAccounts = new HashSet<>();
                                    for (String str : infoData) {
                                        AccountParser parser = new AccountParser(str);
                                        AccountManager manager = parser.parse();
                                        if (manager != null) {
                                            infoAccounts.add(parser.parse());
                                        }
                                    }

                                    new PlayersInfoInventory(infoAccounts, player);
                                    break;
                                case LOOKUPGUI:
                                    String lookupMessage = input.readUTF().replaceFirst("AccountParser\\(", "");
                                    lookupMessage = StringUtils.replaceLast(lookupMessage, "\\)", "");

                                    String[] lookupData = lookupMessage.split(";");
                                    Set<AccountManager> lookupAccounts = new HashSet<>();
                                    for (String str : lookupData) {
                                        AccountParser parser = new AccountParser(str);
                                        AccountManager manager = parser.parse();
                                        if (manager != null) {
                                            lookupAccounts.add(parser.parse());
                                        }
                                    }

                                    new AltAccountsInventory(lookupAccounts, player);
                                    break;
                                case PLAYER:
                                    String serialized = input.readUTF();
                                    BungeeSender.sendPlayerInstance(player, serialized, id.toString());
                                    break;
                                case MODULE:
                                    String name = input.readUTF();
                                    int byteLength = input.readInt();
                                    byte[] message = new byte[byteLength];

                                    int i = 0;
                                    while (i < byteLength) {
                                        message[i] = input.readByte();
                                        i++;
                                    }

                                    ModuleMessageService.listenMessage(name, message);
                                    break;
                                default:
                                    logger.scheduleLog(Level.GRAVE, "Unknown plugin sub channel message: " + sub);
                                    break;
                            }
                            break;
                    }
                } else {
                    logger.scheduleLog(Level.GRAVE, "Someone tried to access the plugin message channel using {0}'s identification ( INVALID KEY )", id.toString());
                }
            } else {
                String proxyKey = input.readUTF();
                String serverName = input.readUTF();

                logger.scheduleLog(Level.INFO, "Received server access plugin message from a proxy with id {0} in where this server is known as {1}", id.toString(), serverName);

                switch (sub) {
                    case KEY:
                        //This also checks if the proxy key is empty, retuning true if it is
                        //so we will just use it as a "emptyProxyOwner" check
                        if (storager.isProxyKey(proxyKey)) {
                            storager.setProxyKey(proxyKey);
                            BungeeSender.sendProxyStatus(player, id.toString(), serverName, sub.name().toLowerCase());

                            storager.setMultiBungee(input.readBoolean());
                        } else {
                            BungeeSender.sendProxyStatus(player, "invalid", serverName, sub.name().toLowerCase());
                            logger.scheduleLog(Level.GRAVE, "Proxy with id {0} tried to register a key but the key is already registered and the specified one is incorrect", id.toString());
                        }
                        break;
                    case REGISTER:
                        if (storager.isProxyKey(proxyKey) && storager.canRegister()) {
                            storager.addProxy(id);
                            BungeeSender.sendProxyStatus(player, id.toString(), serverName, sub.name().toLowerCase());
                        } else {
                            BungeeSender.sendProxyStatus(player, "invalid", serverName, sub.name().toLowerCase());
                            logger.scheduleLog(Level.GRAVE, "Proxy with id {0} to register itself with an invalid access key", id.toString());
                        }
                        break;
                    case REMOVE:
                        //Yes, when a bungeecord proxy goes down
                        //he must send this message, otherwise, when
                        //the proxy starts again, it will have another
                        //access key and won't be able to access anymore
                        if (storager.isProxyKey(proxyKey) && storager.validate(id)) {
                            storager.delProxy(id);
                            BungeeSender.sendProxyStatus(player, id.toString(), serverName, sub.name().toLowerCase());
                        } else {
                            BungeeSender.sendProxyStatus(player, "invalid", serverName, sub.name().toLowerCase());
                            logger.scheduleLog(Level.GRAVE, "Tried to remove proxy with id {0} using an invalid key", id.toString());
                        }
                        break;
                }
            }
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
            logger.scheduleLog(Level.INFO, "Failed to read bungeecord message");
        }
    }
}
