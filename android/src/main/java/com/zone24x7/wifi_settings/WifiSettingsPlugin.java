package com.zone24x7.wifi_settings;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class WifiSettingsPlugin implements MethodCallHandler {

    private Activity activity;
    private WifiManager wifiManager;
    private LocationManager locationManager;

    @SuppressWarnings("WeakerAccess")
    public WifiSettingsPlugin(Activity activity) {
        this.activity = activity;
        // Get the _wifi manager instance from the activity context.
        this.wifiManager = (WifiManager) activity.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        this.locationManager = (LocationManager) activity.getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {

        final MethodChannel channel = new MethodChannel(registrar.messenger(), "wifi_settings");
        channel.setMethodCallHandler(new WifiSettingsPlugin(registrar.activity()));

        // noinspection Convert2Lambda
        registrar.addRequestPermissionsResultListener(new PluginRegistry.RequestPermissionsResultListener() {
            @Override
            public boolean onRequestPermissionsResult(int i, String[] requestedPermissions, int[] ints) {
                return false;
            }
        });

    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "connectToNetwork":
                this.connect(call, result);
                break;
            case "listWifiNetworks":
                this.listWifiNetworks(result);
                break;
            case "disconnect":
                this.disconnect(result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }


    /**
     * Get a list of access points found in the most recent scan.
     * This will not resolve success until _permissions(), _wifi() and
     * _location() returns true. Because all 3 of them is required to
     * do this operation.
     *
     * @param result {@link Result}
     */
    private void listWifiNetworks(Result result) {

        try {

            _isReady();
            log("listWifiNetworks() called");
            List<Map<String, String>> list = new ArrayList<>();

            for (ScanResult sr : wifiManager.getScanResults()) {
                Map<String, String> m = new HashMap<>();
                m.put("ssid", sr.SSID);
                m.put("bssid", sr.BSSID);
                m.put("capabilities", sr.capabilities);
                m.put("level", String.valueOf(sr.level));
                m.put("frequency", String.valueOf(sr.frequency));
                list.add(m);
            }

            log("Latest scan results " + list.size());
            result.success(list);

        } catch (Exception e) {
            result.error(null, e.getMessage(), e);
        }

    }

    /**
     * Connects to a specified wifi network with {ssid, password}
     * Before connecting to a WIFI network we need to check the
     * security type of the WIFI network. ScanResult class
     * has the #capabilities{{@link ScanResult}} field that gives
     * the type of network.
     *
     * @param call   {@link MethodCall}
     * @param result {@link Result}
     * @link https://developer.android.com/reference/android/net/_wifi/ScanResult.html#capabilities
     * @link https://stackoverflow.com/questions/8818290/how-do-i-connect-to-a-specific-wi-fi-network-in-android-programmatically
     */
    private void connect(MethodCall call, Result result) {

        try {

            _isReady();

            log("connectToNetwork() called");
            String _ssid = call.argument("ssid");
            String _password = call.argument("password");

            WiFiConnector connector = new WiFiConnector(this.wifiManager);
            connector.setCredentials(_ssid, _password);
            result.success(connector.connect());

        } catch (Exception e) {
            result.error(e.getMessage(), null, e);
        }

    }

    private void disconnect(Result result) {
        try {
            _isReady();
            log("disconnect() called");
            result.success(this.wifiManager.disconnect());
        } catch (RuntimeException e) {
            result.error(e.getMessage(), null, e);
        }
    }

    // Internal methods only

    private boolean _permissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            int accessCoarseLocation = activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            int accessFineLocation = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            int accessNetworkState = activity.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE);
            int accessWiFiState = activity.checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE);
            int changeWiFiState = activity.checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE);
            int changeNetworkState = activity.checkSelfPermission(Manifest.permission.CHANGE_NETWORK_STATE);

            boolean perms = accessCoarseLocation == PackageManager.PERMISSION_GRANTED &&
                    accessFineLocation == PackageManager.PERMISSION_GRANTED &&
                    accessNetworkState == PackageManager.PERMISSION_GRANTED &&
                    accessWiFiState == PackageManager.PERMISSION_GRANTED &&
                    changeWiFiState == PackageManager.PERMISSION_GRANTED &&
                    changeNetworkState == PackageManager.PERMISSION_GRANTED;

            // Revoke for access
            if (!perms) {
                activity.requestPermissions(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.CHANGE_NETWORK_STATE,
                }, 1);
                return false;
            }

            return true;
        }

        return true;

    }

    private boolean _wifi() {
        if (!wifiManager.isWifiEnabled()) {
            log("WiFi services was disabled. Turing on the _wifi...");
            return this.wifiManager.setWifiEnabled(true);
        }

        return true;
    }

    private boolean _location() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.getApplicationContext().startActivity(intent);
            return false;
        }
        return true;
    }

    private void log(Object o) {
        Log.d("WifiSettingsPlugin: ", o.toString());
    }

    private void _isReady() {

        if (!_permissions()) {
            throw new RuntimeException(EC.PERMISSIONS_NOT_GRANTED.toString());
        }

        if (!_wifi()) {
            throw new RuntimeException(EC.WIFI_DISABLED.toString());
        }

        if (!_location()) {
            throw new RuntimeException(EC.LOCATION_DISABLED.toString());
        }

    }

}
//
///**
// * Uppsala University
// *
// * Project CS course, Fall 2012
// *
// * Projekt DV/Project CS, is a course in which the students develop software for
// * distributed systems. The aim of the course is to give insights into how a big
// * project is run (from planning to realization), how to construct a complex
// * distributed system and to give hands-on experience on modern construction
// * principles and programming methods.
// *
// * All rights reserved.
// *
// * Copyright (C) 2012 LISA team
// */
//package project.cs.lisa.networksettings;
//
//        import java.util.List;
//
//        import android.content.Context;
//        import android.net.wifi.ScanResult;
//        import android.net.wifi.WifiConfiguration;
//        import android.net.wifi.WifiInfo;
//        import android.net.wifi.WifiManager;
//        import android.util.Log;
//
//public class WifiHandler {
//    //constants
//    public static final int WEP = 1;
//    public static final int WAP = 2;
//    public static final int OPEN_NETWORK = 3;
//
//    public static final String TAG = "LISA_Network";
//
//    /** dfsdfsdfsdf. */
//    WifiConfiguration wifiConf;             /* WifiConfiguration object */
//
//    /** dfsdfsdfsdf. */
//    WifiManager wifiMgr;                            /* WifiManager object */
//
//    /** dfsdfsdfsdf. */
//    WifiInfo wifiInfo;                              /* WifiInfo object */
//
//    /** dfsdfsdfsdf. */
//    List<ScanResult> wifiScan;              /* List of ScanResult objects */
//
//    /**
//     * Constructor initializes WifiManager and WifiInfo.
//     * @param context
//     */
//    public WifiHandler(Context context) {
//        wifiMgr  = getWifiManager(context);     // gets wifiMgr in the current context
//        wifiInfo = getWifiInfo(context);            // gets wifiInfo in the current context
//        wifiConf = getWifiConf(context);            // gets wifiConf in the current context
//        wifiScan = getWifiInRange();                    // gets wifiScan in the current context
//    }
//
//    /**
//     * Function checkWifiEnabled checks if the WiFi connection
//     * is enabled on the device.
//     * @param wifiMgr
//     * @return true  if the WiFi connection is enabled,
//     *               false if the WiFi connection is disabled
//     */
//    public boolean checkWifiEnabled() {
//        // checks if WiFi is enabled
//        return (wifiMgr != null && wifiMgr.isWifiEnabled());
//    }
//
//    /**
//     * Function enableWifi enables WiFi connection on the device.
//     * @param wifiMgr
//     * @return true  if the attempt to enable WiFi succeeded,
//     *               false if the attempt to enable WiFi failed.
//     */
//    public boolean enableWifi() {
//        // enables WiFi connection
//        return wifiMgr.setWifiEnabled(true);
//    }
//
//    /**
//     * Function disableWifi disables WiFi connection on the device.
//     * @param wifiMgr
//     * @return true  if WiFi connection was disabled,
//     *               false if attempt to disable WiFi failed.
//     */
//    public boolean disableWifi() {
//        // disables WiFi connection
//        return wifiMgr.setWifiEnabled(false);
//    }
//
//    /**
//     * Function getWifiManager gets the WiFiManager object from the device.
//     * @param context
//     * @return WifiManager object. Also sets the class variable
//     *               wifiMgr as the WifiManager object returned.
//     */
//    public WifiManager getWifiManager(Context context) {
//        WifiManager wifiMgr = null;
//
//        // gets WifiManager obj from the system
//        wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//
//        if (wifiMgr == null) {
//            Log.d("TAG", "WIFI_SERVICE is the wrong service name.");
//        }
//
//        return wifiMgr;
//    }
//
//    /**
//     * Function getWifiInfo gets the current WiFi connection information in a
//     * WifiInfo object from the device.
//     * @param context
//     * @return wifiInfo created object or
//     *               null       if wifi is not enabled.
//     */
//    public WifiInfo getWifiInfo(Context context) {
//        WifiInfo wifiInfo = null;
//
//        // gets WiFi network info of the current connection
//        if (checkWifiEnabled()) {
//            wifiInfo = (WifiInfo) wifiMgr.getConnectionInfo();
//        }
//
//        if (wifiInfo == null) {
//            Log.d("TAG", "WifiInfo object is empty.");
//        }
//
//        return wifiInfo;
//    }
//
//    /**
//     * Function that returns a WifiConfiguration object from the WifiInfo
//     * object from the class. If wifiInfo exists, then we are able to retrieve
//     * information from the current connection
//     * @param context
//     * @return WifiConfiguration object created.
//     */
//    public WifiConfiguration getWifiConf(Context context) {
//        WifiConfiguration wifiConfiguration = new WifiConfiguration();
//
//        if (wifiInfo == null) {
//            Log.d("TAG", "WifiInfo object is empty");
//            return null;
//        }
//
//        wifiConfiguration.SSID = wifiInfo.getSSID();
//        wifiConfiguration.networkId = wifiInfo.getNetworkId();
//
//        return wifiConfiguration;
//    }
//
//    /**
//     * Creates a new WifiConfiguration object for wifiConf.
//     */
//    public void clearWifiConfig() {
//        wifiConf = new WifiConfiguration();
//    }
//
//    /**
//     * Function getWifiInRange returns all the WiFi networks that are
//     * accessible through the access point (device AP) found during the
//     * last scan.
//     * @param wifi
//     * @return List of ScanResult containing information on all WiFi networks
//     *               discovered in the range.
//     */
//    public List<ScanResult> getWifiInRange() {
//        // gets ~last~ list of WiFi networks accessible through the access point.
//        return (wifiScan = (List<ScanResult>) wifiMgr.getScanResults());
//    }
//
//    /**
//     * Function that scans for wifi networks available in the devices range.
//     * @return true  if scan started
//     *               false if scan could not be started
//     */
//    public boolean scanWifiInRange() {
//        if (!checkWifiEnabled()) {
//            return false;
//        }
//
//        if (!wifiMgr.startScan()) {
//            Log.d("TAG", "Failed to scan wifi's in range.");
//            return false;
//        }
//
//        return true;
//    }
//
//    /**
//     * Function to disconnect from the currently connected WiFi AP.
//     * @return true  if disconnection succeeded
//     *               false if disconnection failed
//     */
//    public boolean disconnectFromWifi() {
//        return (wifiMgr.disconnect());
//    }
//
//    /**
//     * Function to connect to a selected network
//     * @param networkSSID         network SSID name
//     * @param   networkPassword     network password
//     * @param networkId           network ID from WifiManager
//     * @param SecurityProtocol    network security protocol
//     * @return true  if connection to selected network succeeded
//     *               false if connection to selected network failed
//     */
//    public boolean connectToSelectedNetwork(String networkSSID, String networkPassword) {
//        int networkId;
//        int SecurityProtocol = WEP;
//
//        // Clear wifi configuration variable
//        clearWifiConfig();
//
//        // Sets network SSID name on wifiConf
//        wifiConf.SSID = "\"" + networkSSID + "\"";
//        Log.d(TAG, "SSID Received: " + wifiConf.SSID);
//        switch(SecurityProtocol) {
//            // WEP "security".
//            case WEP:
//                wifiConf.wepKeys[0] = "\"" + networkPassword + "\"";
//                wifiConf.wepTxKeyIndex = 0;
//                wifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
//                wifiConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
//                break;
//
//            // WAP security. We have to set preSharedKey.
//            case WAP:
//                wifiConf.preSharedKey = "\""+ networkPassword +"\"";
//                break;
//
//            // Network without security.
//            case OPEN_NETWORK:
//                wifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
//                break;
//        }
//
//        // Add WiFi configuration to list of recognizable networks
//        if ((networkId = wifiMgr.addNetwork(wifiConf)) == -1) {
//            Log.d("TAG", "Failed to add network configuration!");
//            return false;
//        }
//
//        // Disconnect from current WiFi connection
//        if (!disconnectFromWifi()) {
//            Log.d("TAG", "Failed to disconnect from network!");
//            return false;
//        }
//
//        // Enable network to be connected
//        if (!wifiMgr.enableNetwork(networkId, true)) {
//            Log.d("TAG", "Failed to enable network!");
//            return false;
//        }
//
//        // Connect to network
//        if (!wifiMgr.reconnect()) {
//            Log.d("TAG", "Failed to connect!");
//            return false;
//        }
//
//        return true;
//    }
//}