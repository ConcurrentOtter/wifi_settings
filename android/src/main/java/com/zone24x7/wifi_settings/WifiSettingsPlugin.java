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
                this.connectToNetwork(call, result);
                break;
            case "listWifiNetworks":
                this.listWifiNetworks(call, result);
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
     * @param call   {@link MethodCall}
     * @param result {@link Result}
     */
    private void listWifiNetworks(MethodCall call, Result result) {

        try {

            _isReady();

            log("listWifiNetworks() called");
            List<Map<String, String>> list = new ArrayList<>();

            for (ScanResult sr : wifiManager.getScanResults()) {
                Map<String, String> m = new HashMap<>();
                m.put("ssid", sr.SSID);
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
    private void connectToNetwork(MethodCall call, Result result) {

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
