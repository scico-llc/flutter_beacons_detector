import 'beacon_proximity.dart';

class Beacon {
  final String identifier;
  final String uuid;
  final String majour;
  final String minor;
  final double distance;
  final BeaconProximity proximity;
  final DateTime scanTime;
  final String macAddress;
  final int? rssi;
  final int? txPower;

  const Beacon._(
    this.identifier,
    this.uuid,
    this.majour,
    this.minor,
    this.distance,
    this.proximity,
    this.scanTime,
    this.macAddress,
    this.rssi,
    this.txPower,
  );

  factory Beacon.fromChannel(Map<String, dynamic> data) {
    return Beacon._(
      data['identifier'],
      data['uuid'],
      data['majour'],
      data['minor'],
      data['distance'],
      data['proximity'],
      data['scanTime'],
      data['macAddress'],
      data['rssi'],
      data['txPower'],
    );
  }
}
