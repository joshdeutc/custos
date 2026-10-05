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
}
