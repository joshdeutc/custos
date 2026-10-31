package com.jo.custos.guardian;

interface IGuardianService {
    int getVersion();
    boolean isDeviceOwnerActive();

    long getQuarantineDelaySeconds();
    boolean isWhitelistEnabled();
    List<String> getAllowedPackages();
    List<String> getPendingPackages();
    List<String> getHiddenPackages();

    boolean requestAppAddition(String packageName);
    boolean cancelPendingRequest(String packageName);
    boolean removeAppFromWhitelist(String packageName);
    boolean requestChangeQuarantineDelay(int hours);
    boolean cancelPendingDelayChange();
    boolean requestDisableWhitelist();
    boolean cancelPendingDisableWhitelist();
    boolean setWhitelistEnabled(boolean enabled);

    void enforceLockdown();
    void setPackageSuspended(String packageName, boolean suspended);
    void setPackageHidden(String packageName, boolean hidden);

    boolean setPrivateDnsHost(String hostname);
    String getPrivateDnsHost();

    // --- Rules & Limits Engine ---
    String getConfigJson();
    boolean requestConfigChange(String candidateJson);
    String getDelayStateJson();
    boolean cancelPendingConfigChange();

    // --- Screen Rules (Reels, Actus, Shorts) ---
    String getScreenRulesJson();
    boolean saveScreenRulesJson(String json);

    // --- Realtime Usage & Block State ---
    List<String> getBlockedApps();
    String getUsageStatsJson();

    // --- Whitelist State ---
    String getWhitelistStateJson();
    boolean saveWhitelistStateJson(String json);

    // --- Notification Preferences ---
    boolean setNotificationMutePreference(String packageName, boolean mute);
    boolean getNotificationMutePreference(String packageName);
}
