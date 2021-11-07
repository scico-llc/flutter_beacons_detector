import 'dart:async';

import 'package:flutter/services.dart';

class BeaconsDetector {
  static const MethodChannel _channel = MethodChannel('beacons_detector');
  static const event_channel = EventChannel('beacons_event_stream');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
