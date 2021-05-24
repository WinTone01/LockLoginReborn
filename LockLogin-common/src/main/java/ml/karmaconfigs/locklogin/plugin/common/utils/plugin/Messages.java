package ml.karmaconfigs.locklogin.plugin.common.utils.plugin;

import ml.karmaconfigs.api.common.utils.FileUtilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public final class Messages {

    public final String getProperty(final String name, final String def) {
        File propFile = new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin", "lang/plugin_messages.properties");
        propFile = FileUtilities.getFixedFile(propFile);
        try {
            if (propFile.exists()) {
                updatePropsVersion();
                FileInputStream in = new FileInputStream(propFile);

                Properties properties = new Properties();
                properties.load(in);

                in.close();

                return properties.getProperty(name, def);
            } else {
                InputStream in = getClass().getResourceAsStream("/lang/plugin_messages.properties");
                if (in != null) {
                    Files.copy(in, propFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    Properties properties = new Properties();
                    properties.load(in);

                    in.close();

                    return properties.getProperty(name, def);
                }
            }
        } catch (Throwable ignored) {
        }

        return "";
    }

    /**
     * Get file properties version and in-file properties
     * version to update if the version does not match
     *
     * @throws Throwable as part of properties#load(InputStream) and new FileInputStream(File)
     */
    protected final void updatePropsVersion() throws Throwable {
        String def_comment = "#HEY! IF YOU ARE MODIFYING THIS FILE FROM THE INTERNAL\n" +
                "#LOCKLOGIN JAR, STOP NOW, IT SHOULD BE MODIFIED FROM\n" +
                "#plugins/LockLogin/plugin_messages.properties.\n" +
                "#   ---------------------------------------\n" +
                "#        IMPORTANT ADVICE\n" +
                "#\n" +
                "#   PLEASE DO NOT MODIFY THIS FILE\n" +
                "#   UNLESS YOU KNOW EXACTLY WHAT YOU\n" +
                "#   ARE DOING. OTHERWISE YOU COULD CAUSE\n" +
                "#   PLUGIN MALFUNCTIONS OR BAD MESSAGE RESPONSES\n" +
                "#\n" +
                "#   PLEASE DO NOT MODIFY properties_lang_version VALUE";

        InputStream in = getClass().getResourceAsStream("/lang/plugin_messages.properties");
        File propFile = new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "lang", "plugin_messages.properties");
        propFile = FileUtilities.getFixedFile(propFile);
        if (in != null && propFile.exists()) {
            InputStream inFile = new FileInputStream(propFile);

            Properties inProps = new Properties();
            Properties outProps = new Properties();

            inProps.load(in);
            outProps.load(inFile);

            String inVersion = inProps.getProperty("properties_lang_version", "1.0.0");
            String outVersion = outProps.getProperty("properties_lang_version", "1.0.0");
            int inSize = textToInt(inVersion);
            int outSize = textToInt(outVersion);

            if (inSize != outSize) {
                if (inSize > outSize) {
                    File oldProp = new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "lang" + File.separator + "old" + File.separator + outVersion, "plugin_messages.properties");
                    oldProp = FileUtilities.getFixedFile(oldProp);

                    Files.move(propFile.toPath(), oldProp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Files.createFile(propFile.toPath());

                    InputStream inOldFile = new FileInputStream(oldProp);

                    //Update out props dest
                    outProps.load(inOldFile);

                    Properties newProps = new Properties();
                    newProps.load(inFile);
                    for (Object key : inProps.keySet()) {
                        if (!key.toString().equalsIgnoreCase("properties_lang_version")) {
                            String oldValue = outProps.getProperty(key.toString(), null);
                            if (oldValue != null) {
                                newProps.setProperty(key.toString(), oldValue);
                            } else {
                                newProps.setProperty(key.toString(), inProps.getProperty(key.toString(), "INVALID VALUE"));
                            }
                        } else {
                            newProps.setProperty("properties_lang_version", inProps.getProperty("properties_lang_version", "1.0.0"));
                        }
                    }

                    newProps.store(Files.newBufferedWriter(propFile.toPath(), StandardCharsets.UTF_8, StandardOpenOption.CREATE), def_comment);

                    inOldFile.close();
                }

                inFile.close();
            }

            in.close();
        }
    }

    /**
     * Get only the numbers of a string
     *
     * @param text the string
     * @return the string numbers
     */
    private int textToInt(final String text) {
        StringBuilder intBuilder = new StringBuilder();
        intBuilder.append(0);
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            if (Character.isDigit(character))
                intBuilder.append(character);
        }

        return Integer.parseInt(intBuilder.toString());
    }
}
