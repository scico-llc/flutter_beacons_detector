package jp.co.scico.beacons_detector

enum class BeaconProximity(val value: String) {
    UNKNOWN("Unknown"),
    IMMEDIATE("Immediate"),
    NEAR("Near"),
    FAR("Far"),
}
