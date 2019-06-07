package com.zone24x7.wifi_settings;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

class WiFiConnector {

    private WifiManager manager;
    private String ssid;
    private String password;

    WiFiConnector(WifiManager manager) {
        this.manager = manager;
    }

    void setCredentials(String _ssid, String _password) throws Exception {
        if (_ssid == null || _ssid.equals("")) {
            throw new Exception(EC.SSID_IS_NULL_OR_EMPTY.toString());
        }
        if (_password == null) {
            _password = "";
        }
        this.ssid = _ssid;
        this.password = _password;
    }

    boolean connect() {

        try {

            log("Trying to connect to wifi...");
            // Check if the ssid is already known by the device.
            WifiConfiguration conf = this.checkIfPreConfigured();
            if (conf != null) {
                log("This network was pre-configured. Trying to connect now...");
                tryWithConfig(conf, true);
                return true;
            }

            // This a new network that we are dealing with
            ScanResult result = this.getLatestScanResult();
            if (result == null) {
                log("Couldn't reach the network");
                throw new RuntimeException(EC.NETWORK_NOT_VISIBLE.toString());
            }

            // Everything as expected. We can try to identify the
            // security protocols of the network and connect
            if (result.capabilities.toUpperCase().contains("WEP")) {
                tryWithConfig(this.wepNetworkConf(), false);
            } else if (result.capabilities.toUpperCase().contains("WPA")) {
                tryWithConfig(this.wpaNetworkConf(), false);
            } else {
                tryWithConfig(this.openNetworkConf(), false);
            }

            return true;

        } catch (RuntimeException e) {
            log("Exception: " + e.getMessage());
            return false;
        }

    }

    /**
     * Checks the network connect attempt configuration
     * in the latest scan results. Because if the network
     * is not visible or reachable no point of connecting
     * to that network.
     *
     * @return ScanResult
     */
    private ScanResult getLatestScanResult() {

        List<ScanResult> latest = manager().getScanResults();
        log("Scan Results size: " + latest.size());
        ScanResult matchingScanResult = null;

        for (ScanResult sc : latest) {
            log("- Comparing " + sc.SSID + " with " + ssid);
            if (sc.SSID.equals(ssid)) {
                matchingScanResult = sc;
                break;
            }
        }

        return matchingScanResult;

    }

    /**
     * Checks the network connect attempt configuration
     * in the configuration list. If so no need to create
     * and add configuration for the defaults list.
     *
     * @return WifiConfiguration
     */
    private WifiConfiguration checkIfPreConfigured() {

        WifiConfiguration foundConfig = null;
        List<WifiConfiguration> list = manager().getConfiguredNetworks();

        for (WifiConfiguration configuration : list) {
            if (configuration.SSID != null && configuration.SSID.equals(inDoubleQuotes(ssid))) {
                foundConfig = configuration;
                break;
            }
        }

        return foundConfig;

    }

    /**
     * For networks using WEP protocol with RC4 stream cipher
     * with 64bit or 128bit keys.
     *
     * @return WifiConfiguration
     */
    private WifiConfiguration wepNetworkConf() {

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = inDoubleQuotes(ssid);
        conf.status = WifiConfiguration.Status.ENABLED;

        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

        if (password.matches("^[0-9a-fA-F]+$")) {
            conf.wepKeys[0] = password;
        } else {
            conf.wepKeys[0] = inDoubleQuotes(password);
        }

        return conf;
    }

    /**
     * For networks using WPA/WPA2 protocol with RC4 stream cipher
     * with 256bit keys.
     *
     * @return WifiConfiguration
     */
    private WifiConfiguration wpaNetworkConf() {

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = inDoubleQuotes(ssid);
        conf.status = WifiConfiguration.Status.ENABLED;

        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

        conf.preSharedKey = inDoubleQuotes(password);

        return conf;

    }

    /**
     * For networks using no security protocols.
     *
     * @return WifiConfiguration
     */
    private WifiConfiguration openNetworkConf() {

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = inDoubleQuotes(ssid);
        conf.status = WifiConfiguration.Status.ENABLED;

        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        conf.allowedAuthAlgorithms.clear();
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

        return conf;

    }

    // Helpers

    private String inDoubleQuotes(String s) {
        return "\"" + s + "\"";
    }

    private WifiManager manager() {
        if (this.manager == null) {
            throw new RuntimeException(EC.WIFI_MANAGER_ERROR.toString());
        }
        return this.manager;
    }

    private void tryWithConfig(WifiConfiguration conf, boolean preConfigured) throws RuntimeException {

        if (preConfigured) {
            if (!manager().enableNetwork(conf.networkId, false)) {
                // In this case caller must handle the re-configuration part.
                log("Pre-configured but failed to enable network: " + conf.SSID);
                throw new RuntimeException(EC.FAILED_TO_ENABLE_NETWORK.toString());
            }
        } else {
            int networkId = manager().addNetwork(conf);
            if (!manager().enableNetwork(networkId, false)) {
                log("Was not pre-configured && failed to enable network: " + conf.SSID);
                throw new RuntimeException(EC.FAILED_TO_ENABLE_NETWORK.toString());
            }
        }

        log("Enabled network: " + conf.SSID);
        boolean reconnected = manager().reconnect();
        if (!reconnected) {
            log("Reconnection failed to network: " + conf.SSID);
            throw new RuntimeException(EC.RECONNECTION_FAILED.toString());
        }

    }

    private void log(Object o) {
        Log.d("WSP.WiFiConnector", o.toString());
    }


}
