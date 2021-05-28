package ml.karmaconfigs.locklogin.plugin.common.utils.version;

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

import ml.karmaconfigs.api.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * LockLogin version id
 */
public final class VersionID {

    private final static Map<String, String> versionMap = new HashMap<>();
    private final String rootVersion;
    private final String keyName;

    /**
     * Initialize the version id
     *
     * @param root   the root version id
     * @param update the update name
     */
    public VersionID(final String root, final String update) {
        rootVersion = root;
        keyName = update;
    }

    /**
     * Transform the version into a version ID
     */
    public final VersionID generate() {
        if (!versionMap.containsKey(keyName)) {
            String[] version_data = rootVersion.split("\\.");
            String build = version_data[0];
            String build_number = version_data[1];
            String release_number = version_data[2];

            int sum = Integer.parseInt(build) + Integer.parseInt(build_number) + Integer.parseInt(release_number);

            build = String.valueOf(Math.abs(Integer.parseInt(build) + sum));
            build_number = String.valueOf(Math.abs(Integer.parseInt(build_number) + sum));
            release_number = String.valueOf(Math.abs(Integer.parseInt(release_number) + sum));

            String name_string = build + build_number + release_number;

            versionMap.put(keyName, keyName.substring(0, 2).toUpperCase() + ":" + name_string);
        }

        return this;
    }

    /**
     * Get the generated version ID
     *
     * @return the version iD
     */
    public final String get() {
        if (StringUtils.isNullOrEmpty(versionMap.getOrDefault(keyName, null)))
            return generate().get();
        else
            return versionMap.get(keyName);
    }
}
