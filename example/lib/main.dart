import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:wifi_settings/wifi_settings.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  WifiSettings settings;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = "Android P";
      this.settings = new WifiSettings();
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: <Widget>[
              RaisedButton(
                child: Text('List Networks'),
                onPressed: () {
                  this.settings.availableNetworks.then((list) {
                    print('List: ' + list.toString());
                  }).catchError((error) {
                    print(error);
                  });
                },
              ),
              RaisedButton(
                child: Text('Connect to Network'),
                onPressed: () {
                  this
                      .settings
                      .connectToNetwork(
                          ssid: "KOHLI Backup WiFi", password: "@kohls@zone#")
                      .then((ok) {
                    if (ok) {
                      print(ok);
                    }
                  }).catchError((error) {
                    print(error);
                  });
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
