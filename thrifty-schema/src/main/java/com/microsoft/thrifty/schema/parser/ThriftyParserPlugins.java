package com.microsoft.thrifty.schema.parser;

import java.util.UUID;

/**
 * Utility class to inject handlers to certain standard Thrifty operations.
 */
public final class ThriftyParserPlugins {

    private static final UUIDProvider DEFAULT_UUID_PROVIDER = UUID::randomUUID;
    private static volatile UUIDProvider uuidProvider = DEFAULT_UUID_PROVIDER;

    /**
     * Prevents changing the plugins.
     */
    private static volatile boolean lockdown;

    /**
     * Prevents changing the plugins from then on.
     * <p>
     * This allows container-like environments to prevent client messing with plugins.
     */
    public static void lockdown() {
        lockdown = true;
    }

    /**
     * Returns true if the plugins were locked down.
     *
     * @return true if the plugins were locked down
     */
    public static boolean isLockdown() {
        return lockdown;
    }

    /**
     * @param uuidProvider the provider to use for generating {@link UUID}s for elements.
     */
    public static void setUUIDProvider(UUIDProvider uuidProvider) {
        if (lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        }
        ThriftyParserPlugins.uuidProvider = uuidProvider;
    }

    /**
     * @return a {@link UUID} as dictated by {@link #uuidProvider}. Default is random UUIDs.
     */
    public static UUID createUUID() {
        return uuidProvider.call();
    }

    public static void reset() {
        uuidProvider = DEFAULT_UUID_PROVIDER;
    }

    private ThriftyParserPlugins() {
        // No instances.
    }

    /**
     * A simple provider interface for creating {@link UUID}s.
     */
    public interface UUIDProvider {

        /**
         * @return a {@link UUID}.
         */
        UUID call();
    }

}
