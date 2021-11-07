#import "BeaconsDetectorPlugin.h"
#if __has_include(<beacons_detector/beacons_detector-Swift.h>)
#import <beacons_detector/beacons_detector-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "beacons_detector-Swift.h"
#endif

@implementation BeaconsDetectorPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftBeaconsDetectorPlugin registerWithRegistrar:registrar];
}
@end
