package ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.lock;

import ml.karmaconfigs.api.bukkit.KarmaFile;
import ml.karmaconfigs.locklogin.api.account.AccountID;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

public final class LockedAccount {

    private final KarmaFile lockedFile;

    /**
     * Initialize the locked account
     *
     * @param id the account id
     */
    public LockedAccount(final AccountID id) {
        File file = new File(plugin.getDataFolder() + File.separator + "data" + File.separator + "accounts", id.getId() + ".locked");
        lockedFile = new KarmaFile(file);
    }

    /**
     * Lock the account
     */
    public final void lock(final String administrator) {
        //Remove the lock file if has been created as directory
        File file = lockedFile.getFile();

        if (file.exists() && file.isDirectory()) {
            try {
                Files.delete(file.toPath());
            } catch (Throwable ex) {
                return;
            }
        }

        if (!file.exists()) {
            lockedFile.create();
        }

        lockedFile.set("ISSUER", administrator);
        lockedFile.set("DATE", Instant.now());
    }

    /**
     * Unlock the account
     */
    public final boolean unlock() {
        try {
            return Files.deleteIfExists(lockedFile.getFile().toPath());
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * Get the locked account data
     *
     * @return the locked account data
     */
    public final LockedData getData() {
        String administrator = lockedFile.getString("ISSUER", "NONE");
        Instant date = Instant.parse(lockedFile.getString("DATE", Instant.now().toString()));

        return new LockedData(administrator, date, (lockedFile.exists() && !lockedFile.getFile().isDirectory()));
    }
}
