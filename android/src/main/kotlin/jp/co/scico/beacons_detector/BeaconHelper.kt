package jp.co.scico.beacons_detector

import android.content.Context
import android.os.RemoteException
import org.altbeacon.beacon.*
import timber.log.Timber

open class BeaconHelper : BeaconsDetectorPlugin.Companion.PluginImpl {

    private var beaconManager: BeaconManager? = null
    companion object {

        @JvmStatic
        private var listOfRegions = arrayListOf<Region>()

        @JvmStatic
        private var foregroundScanPeriod = 1100L

        @JvmStatic
        var foregroundBetweenScanPeriod = 0L

        @JvmStatic
        var backgroundScanPeriod = 1100L

        @JvmStatic
        var backgroundBetweenScanPeriod = 0L
    }

    private fun setUpBeaconManager(context: Context) {
        if(isLocationGranted(context)) {
            Timber.i("setUpBeaconManager")
            beaconManager = BeaconManager.getInstanceForApplication(context)

            beaconManager?.beaconParsers?.add(BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"))
            beaconManager?.beaconParsers?.add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))

            beaconManager?.backgroundBetweenScanPeriod = backgroundBetweenScanPeriod
            beaconManager?.backgroundScanPeriod = backgroundScanPeriod
            beaconManager?.foregroundBetweenScanPeriod = foregroundBetweenScanPeriod
            beaconManager?.foregroundScanPeriod = foregroundScanPeriod
        } else {
            Timber.e("Location permissions are needed.")
        }
    }

    private fun startRegionMonitoring(region: Region) {
        try {
            Timber.i("startMonitoringBeacons: ${region.uniqueId}")
            beaconManager?.startMonitoring(region)
        } catch (e: RemoteException) {
            e.printStackTrace()
            Timber.e(e.message.toString())
        }
    }

    override fun setScanPeriod(
        foregroundScanPeriod: Long,
        foregroundBetweenScanPeriod: Long,
        backgroundScanPeriod: Long,
        backgroundBetweenScanPeriod: Long,
    ) {
        BeaconHelper.backgroundScanPeriod = backgroundScanPeriod
        BeaconHelper.backgroundBetweenScanPeriod = backgroundBetweenScanPeriod
        BeaconHelper.foregroundScanPeriod = foregroundScanPeriod
        BeaconHelper.foregroundBetweenScanPeriod = foregroundBetweenScanPeriod
    }


    override fun stopMonitoringBeacons() {
        Timber.i("stopMonitoringBeacons")

        if(listOfRegions.size!=0) {
            listOfRegions.forEach {
                beaconManager?.startRangingBeacons(it)
                beaconManager?.stopMonitoring(it)
            }
        }
    }


    override fun setBeaconNotifiers(callback: (BeaconData) -> Void) {
        Timber.i("onBeaconServiceConnect")

        beaconManager?.removeAllMonitorNotifiers()

        beaconManager?.addMonitorNotifier(object : MonitorNotifier {
            override fun didEnterRegion(region: Region) {
                try {
                    Timber.d("didEnterRegion")
                    beaconManager?.removeAllRangeNotifiers()
                    beaconManager?.startRangingBeacons(region)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun didExitRegion(region: Region) {
                try {
                    Timber.d("didExitRegion")
                    beaconManager?.stopRangingBeacons(region)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun didDetermineStateForRegion(i: Int, region: Region) {}
        })

        beaconManager?.addRangeNotifier { beacons, _ ->
            if(beacons.isNotEmpty()) {
                beacons.forEach { beacon ->
                    val identifier = listOfRegions.find {
                        beacon.id1.toString()==it.id1.toString()
                    }?.uniqueId

                    val beaconData = identifier?.let { BeaconData.fromRawData(it, beacon) }
                    if(beaconData!=null) {
                        callback(beaconData)
                    } else {
                        Timber.e("detected beacon has invalid data")
                    }
                }
            }
        }
    }


    override fun addRegions(regions: List<Region>) {
        listOfRegions.addAll(regions)

        regions.forEach {
            Timber.i("Region Added: ${it.uniqueId}, UUID: ${it.id1}")
        }
    }

    override fun clearRegions() {
        listOfRegions.clear()

        Timber.i("Regions Cleared")
    }

    private fun setUpBLE(context: Context) {
        hasBLEFeature(context)
        isBluetoothEnabled(context)
    }

    override fun startScanning(context: Context) {
        setUpBLE(context)
        setUpBeaconManager(context)
        val region = Region("all", null, null, null)

        startRegionMonitoring(region)
        beaconManager?.startRangingBeacons(region)
    }

    override fun startMonitoring(context: Context) {
        setUpBLE(context)
        setUpBeaconManager(context)
        if(listOfRegions.isNotEmpty()) {
            Timber.i("Started Monitoring ${listOfRegions.size} regions.")
            listOfRegions.forEach {
                startRegionMonitoring(it)
            }
        } else {
            Timber.i("startScanning: No regions added..")
        }

    }
}
