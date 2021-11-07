import 'dart:io';
import 'dart:ui';

import 'package:flutter/services.dart';

import 'beacon.dart';
import 'beacon_region.dart';
import 'beacon_setting.dart';
import 'callback_dispatcher.dart';

class BeconManager {
  static const _foregroundChannel =
      MethodChannel('plugins.flutter.io/beacons_detector');
  static const _backgroundChannel =
      MethodChannel('plugins.flutter.io/beacons_detector_bg');

  static Future<void> initialize(BeaconSetting setting) async {
    // set callback dispacher
    final CallbackHandle? callbackHandle =
        PluginUtilities.getCallbackHandle(callbackDispatcher);
    final args = <dynamic>[callbackHandle!.toRawHandle()];

    // set beacon scan initial data
    late Map<String, int>? _settingData;
    if (Platform.isAndroid) {
      _settingData = setting.toAndroidSetting();
    } else if (Platform.isIOS) {
      _settingData = setting.toIosSetting();
    } else {
      throw UnsupportedError('this plugin support only ios and android');
    }

    await _foregroundChannel.invokeMethod('BeaconsPlugin.initialize', args);
  }

  static Future<void> startScan(Function(Beacon beacon) callback) async {
    if (!Platform.isAndroid) {
      throw UnsupportedError('this method run on only android platform');
    }
    final CallbackHandle? callbackHandle =
        PluginUtilities.getCallbackHandle(callback);
    final args = <dynamic>[callbackHandle!.toRawHandle()];

    await _foregroundChannel.invokeMethod('BeaconsPlugin.startScan', args);
  }

  static Future<void> startMonitoring(
    List<BeaconRegion> regions,
    Function(Beacon beacon) callback,
  ) async {
    if (regions.isEmpty) {
      throw UnsupportedError(
        'argument regions needs to contain at least one region',
      );
    }
    if (regions.length > 20 && Platform.isIOS) {
      throw UnsupportedError(
        'iOS support register reagions <= 20.',
      );
    }
    final CallbackHandle? callbackHandle =
        PluginUtilities.getCallbackHandle(callback);
    final args = <dynamic>[callbackHandle!.toRawHandle()];

    args.addAll(regions.map((region) => region.toMap()));
    await _foregroundChannel.invokeMethod(
        'BeaconsPlugin.startMonitoring', args);
  }

  static Future<void> stopMonitoring() async =>
      _foregroundChannel.invokeMethod('BeaconsPlugin.stopMonitoring');

  // static Future<void> addRegions(List<BeaconRegion> regions) async {
  //   if (regions.isEmpty) {
  //     throw UnsupportedError(
  //         'argument regions needs to contain at least one region');
  //   }
  //
  //   final List<Map<String, String>> args =
  //       regions.map((region) => region.toMap()).toList();
  //   await _channel.invokeMethod('BeaconsPlugin.addRegions', args);
  // }

  // static Future<void> clearRegions() async =>
  //     _foregroundChannel.invokeMethod('BeaconsPlugin.clearRegions');

  static Future<List<BeaconRegion>> getRegisteredRegions() async =>
      (await _foregroundChannel
              .invokeMethod('BeaconsPlugin.getRegisteredRegions'))
          .cast<Map<String, String>>()
          .map((region) => BeaconRegion.fromMap(region))
          .toList();

  static Future<void> promoteToForeground() async => await _backgroundChannel
      .invokeMethod('BeaconsService.promoteToForeground');

  static Future<void> demoteToBackground() async => await _backgroundChannel
      .invokeMethod('BeaconsService.demoteToBackground');
}
