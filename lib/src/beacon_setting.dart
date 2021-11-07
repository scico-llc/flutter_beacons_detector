class BeaconSetting {
  final int androidFgScanPeriod;
  final int androidFgScanBetween;
  final int androidBgScanPeriod;
  final int androidBgScanBetween;
  final int iosFgScanPeriod;
  final int iosFgScanBetween;
  final int iosBgScanPeriod;
  final int iosBgScanBetween;

  BeaconSetting.manually(
    this.androidFgScanPeriod,
    this.androidFgScanBetween,
    this.androidBgScanPeriod,
    this.androidBgScanBetween,
    this.iosFgScanPeriod,
    this.iosFgScanBetween,
    this.iosBgScanPeriod,
    this.iosBgScanBetween,
  );

  factory BeaconSetting.byOS(
      int androidPeriod, int androidBetween, int iosPeriod, int iosBetween) {
    return BeaconSetting.manually(
      androidPeriod,
      androidBetween,
      androidPeriod,
      androidBetween,
      iosPeriod,
      iosBetween,
      iosPeriod,
      iosBetween,
    );
  }

  factory BeaconSetting.byGround(int foregroundPeriod, int foregroundBetween,
      int backgroundPeriod, int backgroundBetween) {
    return BeaconSetting.manually(
      foregroundPeriod,
      foregroundBetween,
      backgroundPeriod,
      backgroundBetween,
      foregroundPeriod,
      foregroundBetween,
      backgroundPeriod,
      backgroundBetween,
    );
  }

  Map<String, int> toAndroidSetting() {
    return {
      'FgScanPeriod': androidFgScanPeriod,
      'FgScanBetween': androidFgScanBetween,
      'BgScanPeriod': androidBgScanPeriod,
      'BgScanBetween': androidBgScanBetween,
    };
  }

  Map<String, int> toIosSetting() => {
        'FgScanPeriod': iosFgScanPeriod,
        'FgScanBetween': iosFgScanBetween,
        'BgScanPeriod': iosBgScanPeriod,
        'BgScanBetween': iosBgScanBetween,
      };
}
