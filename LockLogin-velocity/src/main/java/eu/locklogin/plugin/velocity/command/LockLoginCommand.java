package eu.locklogin.plugin.velocity.command;

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

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import eu.locklogin.plugin.velocity.command.util.BungeeLikeCommand;
import eu.locklogin.plugin.velocity.command.util.SystemCommand;
import eu.locklogin.plugin.velocity.permissibles.PluginPermission;
import eu.locklogin.plugin.velocity.plugin.FileReloader;
import eu.locklogin.plugin.velocity.util.player.User;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.Console;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.javamodule.JavaModuleLoader;
import eu.locklogin.api.module.plugin.javamodule.updater.JavaModuleVersion;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.velocity.util.ServerLifeChecker;
import eu.locklogin.plugin.velocity.util.files.Message;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.version.VersionCheckType;
import ml.karmaconfigs.api.common.version.VersionUpdater;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.Set;

import static eu.locklogin.plugin.velocity.LockLogin.*;

@SystemCommand(command = "locklogin")
public final class LockLoginCommand extends BungeeLikeCommand {

    /**
     * Initialize the bungee like command
     *
     * @param label the command label
     */
    public LockLoginCommand(String label) {
        super(label);
    }

    /**
     * Execute this command with the specified sender and arguments.
     *
     * @param sender the executor of this command
     * @param args   arguments used to invoke this command
     */
    @Override
    public void execute(CommandSource sender, String[] args) {
        Message messages = new Message();

        VersionUpdater updater = VersionUpdater.createNewBuilder(source).withVersionType(VersionCheckType.RESOLVABLE_ID).withVersionResolver(versionID).build();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            switch (args.length) {
                case 1:
                    switch (args[0].toLowerCase()) {
                        case "reload":
                            FileReloader.reload(player);
                            break;
                        case "modules":
                            if (user.hasPermission(PluginPermission.modules())) {
                                Set<PluginModule> modules = JavaModuleLoader.getModules();

                                TextComponent main = Component.text().content(StringUtils.toColor("&3Modules &8&o( &a" + modules.size() + " &8&o)&7: ")).build();

                                int id = 0;
                                for (PluginModule module : modules) {
                                    JavaModuleVersion version = module.getManager().getVersionManager();

                                    Component factory = Component.text().content("&e" + StringUtils.stripColor(module.name()) + (id == modules.size() - 1 ? "" : "&7, ")).build();

                                    String hoverText = "\n&7Owner(s): &e" + module.singleAuthors() + "\n&7Version: &e" + module.version() + "\n&7Description: &e" + module.description();

                                    try {
                                        if (version.updaterEnabled().get()) {
                                            hoverText = hoverText + "\n&7Latest: &e" + version.getLatest() + "\n\n&7Click me to download the latest version!";

                                            ClickEvent click = ClickEvent.openUrl(version.getDownloadURL());
                                            factory = factory.clickEvent(click);
                                        } else {
                                            hoverText = hoverText + "\n&7Latest: &e" + version.getLatest() + "\n\n&7You are using the latest known module version!";
                                        }
                                    } catch (Throwable ex) {
                                        hoverText = hoverText + "\n&7Latest: &e" + version.getLatest() + "\n\n&7You are using the latest known module version!";
                                    }

                                    HoverEvent<Component> hover = HoverEvent.showText(Component.text().content(hoverText).build());

                                    main.append(factory.hoverEvent(hover));
                                    id++;
                                }

                                user.send(main);
                            } else {
                                user.send(messages.prefix() + messages.permissionError(PluginPermission.modules()));
                            }
                            break;
                        case "version":
                            if (user.hasPermission(PluginPermission.version())) {
                                user.send("&7Current version:&e " + updater.fromCache().resolve(VersionUpdater.VersionFetchResult.VersionType.CURRENT));
                                user.send("&7Latest version:&e " + updater.fromCache().resolve(VersionUpdater.VersionFetchResult.VersionType.LATEST));
                            } else {
                                user.send(messages.prefix() + messages.permissionError(PluginPermission.version()));
                            }
                            break;
                        case "changelog":
                            if (user.hasPermission(PluginPermission.changelog())) {
                                for (String str : updater.fromCache().getChangelog()) {
                                    user.send(str);
                                }
                            } else {
                                user.send(messages.prefix() + messages.permissionError(PluginPermission.changelog()));
                            }
                            break;
                        case "check":
                            if (user.hasPermission(PluginPermission.check())) {
                                updater.fetch(true);
                                user.send("Checked for updates successfully");
                            } else {
                                user.send(messages.prefix() + messages.permissionError(PluginPermission.check()));
                            }
                            break;
                        default:
                            user.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                            break;
                    }
                    break;
                case 3:
                    if (args[0].equalsIgnoreCase("modules")) {
                        String moduleName = args[2];
                        PluginModule module = JavaModuleLoader.getByName(moduleName);

                        if (module != null) {
                            switch (args[1].toLowerCase()) {
                                case "load":
                                    if (user.hasPermission(PluginPermission.loadModules())) {
                                        if (module.load()) {
                                            user.send("&aModule " + moduleName + " has been loaded successfully");
                                        } else {
                                            user.send("&cModule " + moduleName + " failed to load, maybe is already loaded?");
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(PluginPermission.loadModules()));
                                    }
                                    break;
                                case "unload":
                                    if (user.hasPermission(PluginPermission.unloadModules())) {
                                        if (module.unload()) {
                                            user.send("&aModule " + moduleName + " has been unloaded successfully");
                                        } else {
                                            user.send("&cModule " + moduleName + " failed to unload, maybe is not loaded?");
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(PluginPermission.unloadModules()));
                                    }
                                    break;
                                case "reload":
                                    if (user.hasPermission(PluginPermission.reload())) {
                                        module.reload();
                                        user.send("&aModule " + moduleName + " has been reloaded, check console for more info");
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(PluginPermission.reload()));
                                    }
                                    break;
                                default:
                                    user.send("&5&oAvailable sub-commands:&7 /locklogin modules &e<load>&7, &e<unload>&7, &e<reload>&7 &e[module name]");
                                    break;
                            }
                        } else {
                            user.send("&cModule " + moduleName + " is not loaded or does not exist!");
                        }
                    } else {
                        user.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                    }
                    break;
                default:
                    user.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                    break;
            }
        } else {
            switch (args.length) {
                case 1:
                    switch (args[0].toLowerCase()) {
                        case "reload":
                            FileReloader.reload(null);
                            ServerLifeChecker.restart();
                            break;
                        case "modules":
                            Set<PluginModule> modules = JavaModuleLoader.getModules();

                            int id = 0;
                            StringBuilder builder = new StringBuilder();
                            builder.append("&3Modules &8&o( &a").append(modules.size()).append(" &8&o)&7: ");
                            for (PluginModule module : modules) {
                                JavaModuleVersion version = module.getManager().getVersionManager();
                                try {
                                    if (version.updaterEnabled().get()) {
                                        builder.append("&c").append(StringUtils.stripColor(module.name())).append(" &f( &e").append(version.getDownloadURL()).append(" &f)").append(id == modules.size() - 1 ? "" : "&7, ");
                                    } else {
                                        builder.append("&e").append(StringUtils.stripColor(module.name())).append(id == modules.size() - 1 ? "" : "&7, ");
                                    }
                                } catch (Throwable ignored) {}
                                id++;
                            }

                            Console.send(builder.toString());
                            break;
                        case "version":
                            Console.send("&7Current version:&e " + updater.fromCache().resolve(VersionUpdater.VersionFetchResult.VersionType.CURRENT));
                            Console.send("&7Latest version:&e " + updater.fromCache().resolve(VersionUpdater.VersionFetchResult.VersionType.LATEST));
                            break;
                        case "changelog":
                            for (String str : updater.fromCache().getChangelog())
                                Console.send(str);
                            break;
                        case "check":
                            updater.fetch(true);
                            Console.send(source, "Checked for updates successfully", Level.OK);
                            break;
                        default:
                            Console.send("&5&oAvailable sub-commands:&7 /locklogin &e<version>&7, &e<changelog>&7, &e<check>");
                            break;
                    }
                    break;
                case 3:
                    if (args[0].equalsIgnoreCase("modules")) {
                        String moduleName = args[2];
                        PluginModule module = JavaModuleLoader.getByName(moduleName);

                        if (module != null) {
                            switch (args[1].toLowerCase()) {
                                case "load":
                                    if (module.load()) {
                                        Console.send("&aModule " + moduleName + " has been loaded successfully");
                                    } else {
                                        Console.send("&cModule " + moduleName + " failed to load, maybe is already loaded?");
                                    }
                                    break;
                                case "unload":
                                    if (module.unload()) {
                                        Console.send("&aModule " + moduleName + " has been unloaded successfully");
                                    } else {
                                        Console.send("&cModule " + moduleName + " failed to unload, maybe is not loaded?");
                                    }
                                    break;
                                case "reload":
                                    module.reload();
                                    break;
                                default:
                                    Console.send("&5&oAvailable sub-commands:&7 /locklogin modules &e<load>&7, &e<unload>&7, &e<reload>&7 &e[module name]");
                                    break;
                            }
                        } else {
                            Console.send("&cModule " + moduleName + " is not loaded or does not exist!");
                        }
                    } else {
                        Console.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                    }
                    break;
                default:
                    Console.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                    break;
            }
        }
    }
}