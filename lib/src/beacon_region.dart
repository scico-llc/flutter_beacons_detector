import 'package:uuid/uuid.dart';

class BeaconRegion {
  final String identifier;
  final String uuid;

  const BeaconRegion(this.identifier, this.uuid);

  factory BeaconRegion.randomId(String uuid) {
    final _randomId = const Uuid().v4();
    return BeaconRegion(_randomId, uuid);
  }

  factory BeaconRegion.fromMap(Map<String, dynamic> data) {
    return BeaconRegion(data['identifier'] as String, data['uuid'] as String);
  }

  Map<String, String> toMap() {
    return {
      'identifier': identifier,
      'uuid': uuid,
    };
  }
}
