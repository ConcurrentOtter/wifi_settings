import 'dart:async';

import 'package:flutter/services.dart';

class WifiSettings {
  static const MethodChannel _channel = const MethodChannel('wifi_settings');

  /// Get a list of access points found in the most recent scan.
  /// Check: https://developer.android.com/guide/topics/connectivity/wifi-scan
  ///
  Future<List<dynamic>> get availableNetworks async {
    return await _channel.invokeListMethod('listWifiNetworks');
  }

  Future<bool> connectToNetwork({String ssid, String password}) async {
    Map<String, String> args = new Map();
    args['ssid'] = ssid;
    args['password'] = password;
    return await _channel.invokeMethod('connectToNetwork', args);
  }

  Future<bool> disconnect() async {
    return await _channel.invokeMethod('disconnect');
  }
}
