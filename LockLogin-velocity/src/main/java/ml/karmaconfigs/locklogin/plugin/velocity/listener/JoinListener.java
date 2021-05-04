package ml.karmaconfigs.locklogin.plugin.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.velocity.Console;
import ml.karmaconfigs.api.velocity.timer.AdvancedPluginTimer;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.modules.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.event.user.UserJoinEvent;
import ml.karmaconfigs.locklogin.api.modules.event.user.UserPostJoinEvent;
import ml.karmaconfigs.locklogin.api.modules.event.user.UserPreJoinEvent;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.common.security.BruteForce;
import ml.karmaconfigs.locklogin.plugin.common.security.client.AccountData;
import ml.karmaconfigs.locklogin.plugin.common.security.client.IpData;
import ml.karmaconfigs.locklogin.plugin.common.security.client.Name;
import ml.karmaconfigs.locklogin.plugin.common.security.client.Proxy;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import ml.karmaconfigs.locklogin.plugin.common.utils.InstantParser;
import ml.karmaconfigs.locklogin.plugin.common.utils.UUIDGen;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataType;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.client.OfflineClient;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.data.lock.LockedAccount;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.data.lock.LockedData;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.SessionCheck;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;
import net.kyori.adventure.text.Component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.velocity.permissibles.PluginPermission.altInfo;
import static ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender.*;

public final class JoinListener {

    private final static Map<InetAddress, String> verified = new HashMap<>();

    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    @Subscribe(order = PostOrder.LAST)
    public final void onPreLogin_APICall(PreLoginEvent e) {
        UserPreJoinEvent event = new UserPreJoinEvent(e.getConnection().getRemoteAddress().getAddress(), null, e.getUsername(), e);
        JavaModuleManager.callEvent(event);
    }

    @Subscribe(order = PostOrder.LAST)
    public final void onLogin_APICall(LoginEvent e) {
        UserJoinEvent event = new UserJoinEvent(e.getPlayer().getRemoteAddress().getAddress(), e.getPlayer().getUniqueId(), e.getPlayer().getUsername(), e);
        JavaModuleManager.callEvent(event);
    }

    @Subscribe(order = PostOrder.LAST)
    public final void onPostLogin_APICall(PostLoginEvent e) {
        UserPostJoinEvent event = new UserPostJoinEvent(fromPlayer(e.getPlayer()), e);
        JavaModuleManager.callEvent(event);
    }

    @Subscribe(order = PostOrder.FIRST)
    public final void onServerPing(ProxyPingEvent e) {
        verified.put(e.getConnection().getRemoteAddress().getAddress(), "");
    }

    @SuppressWarnings("all")
    @Subscribe(order = PostOrder.LAST)
    public final void onPreLogin(PreLoginEvent e) {
        Message messages = new Message();
        InetAddress ip = e.getConnection().getRemoteAddress().getAddress();

        String conn_name = e.getUsername();
        UUID tar_uuid = UUIDGen.getUUID(conn_name);

        String address = "null";
        try {
            address = ip.getHostAddress();
        } catch (Throwable ignored) {
        }
        if (!e.getResult().isAllowed()) {
            try {
                if (validateIP(ip)) {
                    PluginConfiguration config = CurrentPlatform.getConfiguration();

                    UUID gen_uuid = UUIDGen.getUUID(conn_name);

                    if (config.registerOptions().maxAccounts() > 0) {
                        AccountData data = new AccountData(ip, AccountID.fromUUID(tar_uuid));

                        if (data.allow(config.registerOptions().maxAccounts())) {
                            data.save();

                            int amount = data.getAlts().size();
                            if (amount > 2) {
                                for (Player online : server.getAllPlayers()) {
                                    User user = new User(online);

                                    if (user.hasPermission(altInfo())) {
                                        user.send(messages.prefix() + messages.altFound(conn_name, amount - 1));
                                    }
                                }

                                if (!messages.altFound(conn_name, amount).replaceAll("\\s", "").isEmpty())
                                    Console.send(messages.prefix() + messages.altFound(conn_name, amount - 1));
                            }
                        } else {
                            e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.maxRegisters())).build()));
                            return;
                        }
                    }

                    if (config.bruteForceOptions().getMaxTries() > 0) {
                        BruteForce protection = new BruteForce(ip);
                        if (protection.isBlocked()) {
                            e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.ipBlocked(protection.getBlockLeft()))).build()));
                            return;
                        }
                    }

                    if (config.antiBot()) {
                        if (verified.containsKey(ip)) {
                            String name = verified.getOrDefault(ip, "");

                            if (!name.replaceAll("\\s", "").isEmpty() && !name.equals(conn_name)) {
                                //The anti bot is like a whitelist, only players in a certain list can join, the difference with LockLogin is that players are
                                //assigned to an IP, so the anti bot security is reinforced
                                e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.antiBot())).build()));
                                return;
                            } else {
                                if (name.replaceAll("\\s", "").isEmpty())
                                    verified.put(ip, conn_name);
                            }
                        } else {
                            //The anti bot is like a whitelist, only players in a certain list can join, the difference with LockLogin is that players are
                            //assigned to an IP, so the anti bot security is reinforced
                            e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.antiBot())).build()));
                            return;
                        }
                    }

                    if (!gen_uuid.equals(tar_uuid)) {
                        e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.uuidFetchError())).build()));
                        return;
                    }

                    Name name = new Name(conn_name);
                    name.check();

                    if (name.notValid()) {
                        e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.illegalName(name.getInvalidChars()))).build()));
                        return;
                    }

                    OfflineClient offline = new OfflineClient(conn_name);
                    AccountManager manager = offline.getAccount();
                    if (manager != null) {
                        LockedAccount account = new LockedAccount(manager.getUUID());
                        LockedData data = account.getData();

                        if (data.isLocked()) {
                            String administrator = data.getAdministrator();
                            Instant date = data.getLockDate();
                            InstantParser parser = new InstantParser(date);
                            String dateString = parser.getDay() + " " + parser.getMonth() + " " + parser.getYear();

                            e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.forcedAccountRemoval(administrator + " [ " + dateString + " ]"))).build()));
                            logger.scheduleLog(Level.WARNING, "Client {0} tried to join, but his account was blocked by {1} on {2}", conn_name, administrator, dateString);
                            return;
                        }
                    }
                } else {
                    e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.ipProxyError())).build()));
                    logger.scheduleLog(Level.GRAVE, "Player {0}[{2}] tried to join with an invalid IP address ( {1} ), his connection got rejected with ip is proxy message", conn_name, address, tar_uuid);
                }
            } catch (Throwable ex) {
                logger.scheduleLog(Level.GRAVE, ex);
                e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.ipProxyError())).build()));
                logger.scheduleLog(Level.GRAVE, "Player {0}[{2}] tried to join with an invalid IP address ( {1} ), his connection got rejected with ip is proxy message", conn_name, address, tar_uuid);
            }
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public final void onLogin(LoginEvent e) {
        if (!e.getResult().isAllowed()) {
            Player player = e.getPlayer();
            Message messages = new Message();
            PluginConfiguration config = CurrentPlatform.getConfiguration();

            IpData data = new IpData(player.getRemoteAddress().getAddress());
            int amount = data.getClonesAmount();

            if (amount + 1 == config.accountsPerIP()) {
                e.setResult(ResultedEvent.ComponentResult.denied(Component.text().content(StringUtils.toColor(messages.maxIP())).build()));
                return;
            }
            data.addClone();
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public final void onPostLogin(PostLoginEvent e) {
        server.getScheduler().buildTask(plugin, () -> {
            PluginConfiguration config = CurrentPlatform.getConfiguration();

            Player player = e.getPlayer();
            InetSocketAddress ip = player.getRemoteAddress();
            User user = new User(player);

            DataSender.send(player, DataSender.getBuilder(DataType.MESSAGES, PLUGIN_CHANNEL).addTextData(Message.manager.getMessages()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.CONFIG, PLUGIN_CHANNEL).addTextData(Message.manager.getMessages()).build());

            MessageData validation = getBuilder(DataType.VALIDATION, DataSender.CHANNEL_PLAYER).build();
            DataSender.send(player, validation);

            Message messages = new Message();

            Proxy proxy = new Proxy(ip);
            if (proxy.isProxy()) {
                user.kick(messages.ipProxyError());
                return;
            }

            user.applySessionEffects();

            if (config.clearChat()) {
                for (int i = 0; i < 150; i++)
                    server.getScheduler().buildTask(plugin, () -> player.sendMessage(Component.text().content("").build()));
            }

            ClientSession session = user.getSession();
            session.validate();

            if (!config.captchaOptions().isEnabled())
                session.setCaptchaLogged(true);

            AdvancedPluginTimer tmp_timer = null;
            if (!session.isCaptchaLogged()) {
                tmp_timer = new AdvancedPluginTimer(plugin, 1, true);
                tmp_timer.addAction(() -> {
                    player.sendActionBar(Component.text().content(StringUtils.toColor(messages.captcha(session.getCaptcha()))).build());
                }).start();
            }

            MessageData join = DataSender.getBuilder(DataType.JOIN, CHANNEL_PLAYER)
                    .addBoolData(session.isLogged())
                    .addBoolData(session.is2FALogged())
                    .addBoolData(session.isPinLogged())
                    .addBoolData(user.isRegistered()).build();
            DataSender.send(player, join);

            AdvancedPluginTimer timer = tmp_timer;
            SessionCheck check = new SessionCheck(player, target -> {
                player.sendActionBar(Component.text().content("").build());
                if (timer != null)
                    timer.setCancelled();
            }, target -> {
                player.sendActionBar(Component.text().content("").build());
                if (timer != null)
                    timer.setCancelled();
            });

            server.getScheduler().buildTask(plugin, check).schedule();

            DataSender.send(player, DataSender.getBuilder(DataType.LOGGED, PLUGIN_CHANNEL).addIntData(SessionDataContainer.getLogged()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.REGISTERED, PLUGIN_CHANNEL).addIntData(SessionDataContainer.getRegistered()).build());
        }).delay(2, TimeUnit.SECONDS).schedule();
    }

    /**
     * Check if the ip is valid
     *
     * @param ip the ip address
     * @return if the ip is valid
     */
    private boolean validateIP(final InetAddress ip) {
        if (StringUtils.isNullOrEmpty(ip.getHostAddress())) {
            return false;
        }

        Matcher matcher = IPv4_PATTERN.matcher(ip.getHostAddress());
        return matcher.matches();
    }
}