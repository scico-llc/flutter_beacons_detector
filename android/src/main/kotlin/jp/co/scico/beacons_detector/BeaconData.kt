package jp.co.scico.beacons_detector

import kotlin.math.roundToInt

data class BeaconData(
    var name: String? = "",
    var uuid: String? = "",
    var major: String = "",
    var minor: String = "",
    var distance: Double = 0.0,
    var proximity: BeaconProximity = BeaconProximity.UNKNOWN,
    var scanTime: Long = 0,
    var macAddress: String = "",
    var rssi: Int = 0,
    var txPower: Int = 0
) {

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "uuid" to uuid,
            "major" to major,
            "minor" to minor,
            "distance" to distance,
            "proximity" to proximity,
            "scanTime" to scanTime,
            "macAddress" to macAddress,
            "rssi" to rssi,
            "txPower" to txPower,
        )
    }

    companion object {
        fun fromRawData(name: String, beacon: org.altbeacon.beacon.Beacon): BeaconData {
            return BeaconData(
                name,
                beacon.id1.toString(),
                beacon.id2.toString(),
                beacon.id3.toString(),
                ((beacon.distance * 100.0).roundToInt() / 100.0),
                this.getProximityOfBeacon(beacon),
                System.currentTimeMillis(),
                beacon.bluetoothAddress,
                beacon.rssi,
                beacon.txPower,
            )
        }

        private fun getProximityOfBeacon(beacon: org.altbeacon.beacon.Beacon): BeaconProximity {
            return if(beacon.distance < 0.5) {
                BeaconProximity.IMMEDIATE
            } else if(beacon.distance > 0.5 && beacon.distance < 3.0) {
                BeaconProximity.NEAR
            } else if(beacon.distance > 3.0) {
                BeaconProximity.FAR
            } else {
                BeaconProximity.UNKNOWN
            }
        }
    }
}

