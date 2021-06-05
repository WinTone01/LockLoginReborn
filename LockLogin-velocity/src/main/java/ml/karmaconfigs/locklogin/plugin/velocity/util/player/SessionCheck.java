package ml.karmaconfigs.locklogin.plugin.velocity.util.player;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import com.velocitypowered.api.proxy.Player;
import ml.karmaconfigs.api.common.boss.BossColor;
import ml.karmaconfigs.api.common.boss.ProgressiveBar;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.velocity.makeiteasy.BossMessage;
import ml.karmaconfigs.api.velocity.timer.AdvancedPluginTimer;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.Message;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.plugin;

public final class SessionCheck implements Runnable {

    private final Player player;
    private final Set<UUID> under_check = new HashSet<>();
    private final Consumer<Player> authAction;
    private final Consumer<Player> failAction;
    private BossMessage boss;
    private String BAR_COLOR = "&a";

    /**
     * Initialize the status checker
     *
     * @param target the target over who
     *               perform the check
     * @param onAuth the action to perform when
     *               the player auths successfully
     * @param onFail the action to perform
     *               if the player doesn't auth at time
     */
    public SessionCheck(final Player target, final Consumer<Player> onAuth, final Consumer<Player> onFail) {
        player = target;
        authAction = onAuth;
        failAction = onFail;
    }

    /**
     * Check the user session status
     */
    @Override
    public final void run() {
        if (!under_check.contains(player.getUniqueId())) {
            under_check.add(player.getUniqueId());

            User user = new User(player);
            PluginConfiguration config = CurrentPlatform.getConfiguration();
            Message messages = new Message();

            int tmp_time = config.registerOptions().timeOut();
            if (user.isRegistered()) {
                tmp_time = config.loginOptions().timeOut();
                user.send(messages.prefix() + messages.login());

                if (config.loginOptions().hasBossBar())
                    boss = new BossMessage(plugin, messages.loginBar(BAR_COLOR, tmp_time), tmp_time).color(BossColor.GREEN).progress(ProgressiveBar.DOWN);
            } else {
                user.send(messages.prefix() + messages.register());

                if (config.registerOptions().hasBossBar())
                    boss = new BossMessage(plugin, messages.registerBar(BAR_COLOR, tmp_time), tmp_time).color(BossColor.GREEN).progress(ProgressiveBar.DOWN);
            }

            if (boss != null) {
                boss.scheduleBar(player);
            }

            int time = tmp_time;
            AdvancedPluginTimer timer = new AdvancedPluginTimer(plugin, time, false).setAsync(false);
            timer.addAction(() -> {
                ClientSession session = user.getSession();
                if (!session.isLogged() && player.isActive()) {
                    int timer_time = timer.getTime();

                    if (boss != null) {
                        if (timer_time == ((int) Math.round(((double) time / 2)))) {
                            boss.color(BossColor.YELLOW);
                            BAR_COLOR = "&e";
                        }

                        if (timer_time == ((int) Math.round(((double) time / 3)))) {
                            boss.color(BossColor.RED);
                            BAR_COLOR = "&c";
                        }

                        if (user.isRegistered()) {
                            boss.update(messages.loginBar(BAR_COLOR, timer_time), false);
                        } else {
                            boss.update(messages.registerBar(BAR_COLOR, timer_time), false);
                        }
                    }

                    if (user.isRegistered()) {
                        if (!StringUtils.isNullOrEmpty(messages.loginTitle(timer_time)) || !StringUtils.isNullOrEmpty(messages.loginSubtitle(timer_time)))
                            user.send(messages.loginTitle(timer_time), messages.loginSubtitle(timer_time));
                    } else {
                        if (!StringUtils.isNullOrEmpty(messages.registerTitle(timer_time)) || !StringUtils.isNullOrEmpty(messages.registerSubtitle(timer_time)))
                            user.send(messages.registerTitle(timer_time), messages.registerSubtitle(timer_time));
                    }
                } else {
                    timer.setCancelled();
                }
            }).addActionOnEnd(() -> {
                if (boss != null)
                    boss.cancel();

                ClientSession session = user.getSession();

                if (!session.isLogged())
                    user.kick((user.isRegistered() ? messages.loginTimeOut() : messages.registerTimeOut()));

                user.restorePotionEffects();

                under_check.remove(player.getUniqueId());

                if (failAction != null)
                    failAction.accept(player);
            }).addActionOnCancel(() -> {
                if (boss != null)
                    boss.cancel();

                user.restorePotionEffects();
                under_check.remove(player.getUniqueId());

                if (authAction != null)
                    authAction.accept(player);
            });

            timer.start();
            startMessageTask();
        }
    }

    /**
     * Initialize the session message
     * task
     */
    private void startMessageTask() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        Message messages = new Message();
        User user = new User(player);

        int time = config.registerOptions().getMessageInterval();
        if (user.isRegistered())
            time = config.loginOptions().getMessageInterval();

        AdvancedPluginTimer timer = new AdvancedPluginTimer(plugin, time, true);
        timer.addActionOnEnd(() -> {
            ClientSession session = user.getSession();
            if (!session.isLogged()) {
                if (user.isRegistered() && !session.isLogged())
                    user.send(messages.prefix() + messages.login());
                else if (!user.isRegistered())
                    user.send(messages.prefix() + messages.register());
            } else {
                timer.setCancelled();
            }
        });
        timer.start();
    }
}
