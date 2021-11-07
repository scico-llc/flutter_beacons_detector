import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'beacon.dart';

void callbackDispatcher() {
  const _backgroundChannel =
      MethodChannel('plugins.flutter.io/beacons_detector_bg');

  WidgetsFlutterBinding.ensureInitialized();

  _backgroundChannel.setMethodCallHandler((MethodCall call) async {
    final List<dynamic> args = call.arguments;
    final Function? callback = PluginUtilities.getCallbackFromHandle(
      CallbackHandle.fromRawHandle(args[0]),
    )!;
    assert(callback != null);
    final beacon = Beacon.fromChannel(args[1].cast<String, dynamic>());

    callback!(beacon);
  });
  _backgroundChannel.invokeMethod('initialized');
}
