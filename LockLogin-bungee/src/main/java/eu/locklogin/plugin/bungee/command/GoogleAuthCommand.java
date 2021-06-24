package eu.locklogin.plugin.bungee.command;

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

import eu.locklogin.plugin.bungee.permissibles.PluginPermission;
import eu.locklogin.plugin.bungee.util.files.Message;
import eu.locklogin.plugin.bungee.util.files.data.ScratchCodes;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.encryption.CryptoUtil;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.javamodule.JavaModuleManager;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.command.util.SystemCommand;
import eu.locklogin.plugin.bungee.plugin.sender.DataSender;
import eu.locklogin.plugin.bungee.util.player.User;
import eu.locklogin.api.common.security.GoogleAuthFactory;
import eu.locklogin.api.common.session.SessionDataContainer;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.plugin.ComponentFactory;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.List;

import static eu.locklogin.plugin.bungee.LockLogin.fromPlayer;
import static eu.locklogin.plugin.bungee.LockLogin.properties;

@SystemCommand(command = "2fa")
public final class GoogleAuthCommand extends Command {

    /**
     * Construct a new command with no permissions or aliases.
     *
     * @param name the name of this command
     */
    public GoogleAuthCommand(String name) {
        super(name);
    }

    /**
     * Execute this command with the specified sender and arguments.
     *
     * @param sender the executor of this command
     * @param args   arguments used to invoke this command
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        Message messages = new Message();

        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            User user = new User(player);

            ClientSession session = user.getSession();
            AccountManager manager = user.getManager();
            if (session.isValid()) {
                if (config.enable2FA()) {
                    if (args.length == 0) {
                        user.send(messages.prefix() + messages.gAuthUsages());
                    } else {
                        switch (args[0].toLowerCase()) {
                            case "setup":
                                if (session.isCaptchaLogged() && session.isLogged() && session.isPinLogged()) {
                                    if (args.length == 2) {
                                        if (manager.has2FA()) {
                                            user.send(messages.prefix() + messages.gAuthSetupAlready());
                                        } else {
                                            String password = args[1];

                                            CryptoUtil util = CryptoUtil.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                            if (util.validate()) {
                                                String token = manager.getGAuth();

                                                if (token.replaceAll("\\s", "").isEmpty()) {
                                                    GoogleAuthFactory factory = user.getTokenFactory();
                                                    token = factory.generateToken();

                                                    user.send(messages.prefix() + messages.gAuthInstructions());

                                                    String name = config.serverName();
                                                    if (name.replaceAll("\\s", "").isEmpty())
                                                        name = "LockLogin";

                                                    String token_url = StringUtils.formatString("https://karmaconfigs.ml/qr/?{0}%20{1}?{2}", StringUtils.stripColor(player.getDisplayName()), StringUtils.formatString("({0})", name), token);

                                                    ComponentFactory c_factory = new ComponentFactory(messages.gAuthLink()).hover(properties.getProperty("command_gauth_hover", "&eClick here to scan the QR code!")).click(ClickEvent.Action.OPEN_URL, token_url);
                                                    user.send(c_factory.get());

                                                    List<Integer> scratch_codes = factory.getRecoveryCodes();
                                                    user.send(messages.gAuthScratchCodes(scratch_codes));

                                                    ScratchCodes codes = new ScratchCodes(player);
                                                    codes.store(scratch_codes);

                                                    manager.setGAuth(token);
                                                } else {
                                                    user.send(messages.prefix() + messages.gAuthEnabled());
                                                }

                                                session.set2FALogged(false);
                                                manager.set2FA(true);
                                            } else {
                                                user.send(messages.prefix() + messages.gAuthToggleError());
                                            }
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.gAuthSetupUsage());
                                    }
                                }
                                break;
                            case "remove":
                                if (user.hasPermission(PluginPermission.forceFA())) {
                                    user.send(messages.prefix() + messages.gauthLocked());
                                } else {
                                    if (session.isCaptchaLogged() && session.isLogged() && session.isTempLogged()) {
                                        if (args.length >= 3) {
                                            if (manager.has2FA()) {
                                                String password = args[1];

                                                StringBuilder codeBuilder = new StringBuilder();
                                                for (int i = 2; i < args.length; i++)
                                                    codeBuilder.append(args[i]);

                                                try {
                                                    int code = Integer.parseInt(codeBuilder.toString());

                                                    CryptoUtil util = CryptoUtil.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                                    GoogleAuthFactory factory = user.getTokenFactory();

                                                    if (util.validate() && factory.validate(manager.getGAuth(), code)) {
                                                        manager.set2FA(false);
                                                        manager.setGAuth(null);

                                                        user.send(messages.prefix() + messages.gAuthDisabled());
                                                    } else {
                                                        user.send(messages.prefix() + messages.gAuthToggleError());
                                                    }
                                                } catch (Throwable ex) {
                                                    user.send(messages.prefix() + messages.gAuthIncorrect());
                                                }
                                            } else {
                                                user.send(messages.prefix() + messages.gAuthNotEnabled());
                                            }
                                        } else {
                                            user.send(messages.prefix() + messages.gAuthRemoveUsage());
                                        }
                                    }
                                }
                                break;
                            default:
                                if (manager.has2FA()) {
                                    if (!session.is2FALogged()) {
                                        StringBuilder codeBuilder = new StringBuilder();
                                        for (String arg : args) codeBuilder.append(arg);

                                        try {
                                            int code = Integer.parseInt(codeBuilder.toString());

                                            GoogleAuthFactory factory = user.getTokenFactory();
                                            ScratchCodes codes = new ScratchCodes(player);

                                            if (factory.validate(manager.getGAuth(), code) || codes.validate(code)) {
                                                DataSender.send(player, DataSender.getBuilder(DataType.GAUTH, DataSender.CHANNEL_PLAYER, player).build());

                                                session.set2FALogged(true);
                                                session.setPinLogged(true);

                                                UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.FA_2, UserAuthenticateEvent.Result.SUCCESS, fromPlayer(player), messages.gAuthCorrect(), null);
                                                JavaModuleManager.callEvent(event);

                                                user.send(messages.prefix() + event.getAuthMessage());

                                                if (codes.needsNew()) {
                                                    List<Integer> newCodes = GoogleAuthFactory.ScratchGenerator.generate();
                                                    user.send(messages.gAuthScratchCodes(newCodes));

                                                    codes.store(newCodes);
                                                }

                                                SessionDataContainer.setLogged(SessionDataContainer.getLogged() + 1);
                                                user.checkServer(0);
                                            } else {
                                                UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.FA_2, UserAuthenticateEvent.Result.FAILED, fromPlayer(player), messages.gAuthIncorrect(), null);
                                                JavaModuleManager.callEvent(event);

                                                user.send(messages.prefix() + event.getAuthMessage());
                                            }
                                        } catch (Throwable ex) {
                                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.FA_2, UserAuthenticateEvent.Result.ERROR, fromPlayer(player), messages.gAuthIncorrect(), null);
                                            JavaModuleManager.callEvent(event);

                                            user.send(messages.prefix() + event.getAuthMessage());
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.gAuthAlready());
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.gAuthNotEnabled());
                                }
                                break;
                        }
                    }
                } else {
                    user.send(messages.prefix() + messages.gAuthServerDisabled());
                }
            } else {
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            Console.send(messages.prefix() + properties.getProperty("only_console_google", "&5&oThe console can't have 2FA enabled!"));
        }
    }
}