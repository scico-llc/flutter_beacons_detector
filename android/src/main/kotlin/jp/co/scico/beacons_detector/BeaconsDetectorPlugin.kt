package jp.co.scico.beacons_detector

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.NonNull
import com.google.gson.Gson

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.Region
import org.json.JSONArray
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
// TODO beacon周りの操作はServiceに記載した方が良い？
/** BeaconsDetectorPlugin */
class BeaconsDetectorPlugin : ActivityAware, FlutterPlugin, MethodCallHandler {
    private var mContext: Context? = null
    private var mActivity: Activity? = null
    private val channel: String = "plugins.flutter.io/beacons_detector"

    companion object {
        @JvmStatic
        val SHARED_PREFERENCES_KEY = "beacons_detector_plugin_cache"

        @JvmStatic
        val CALLBACK_DISPATCHER_HANDLE_KEY = "callback_dispatch_handler"

        @JvmStatic
        val synchronizedCacheLock = Object()

        @JvmStatic
        val PERSISTENT_BEACON_PARAMS = "persistent_beacon_params"

        @JvmStatic
        val PERSISTENT_REGION_IDS = "persistent_region_ids"

        private var REQUEST_LOCATION_PERMISSIONS = 1890
        private var PERMISSION_REQUEST_BACKGROUND_LOCATION = 1891
        private var mBeaconHelper: BeaconHelper? = null

        interface PluginImpl {
            fun startScanning(context: Context)
            fun startMonitoring(context: Context)
            fun stopMonitoringBeacons()
            fun addRegions(regions: List<Region>)
            fun clearRegions()
            fun setBeaconNotifiers(callback: (BeaconData) -> Void)
            fun setScanPeriod(
                foregroundScanPeriod: Long,
                foregroundBetweenScanPeriod: Long,
                backgroundScanPeriod: Long,
                backgroundBetweenScanPeriod: Long,
            )

            fun startBackgroundService(context: Context) {
                Timber.i("startBackgroundService")
                val intent = Intent(context, BeaconDetectorService::class.java)
                context.startForegroundService(intent)
            }

            fun stopBackgroundService(context: Context) {
                Timber.i("stopBackgroundService")
                val intent = Intent(context, BeaconDetectorService::class.java)
                context.stopService(intent)
            }
        }

        @JvmStatic
        fun registerWith(beaconHelper: BeaconHelper) {
            Timber.i("registerWith")
            this.mBeaconHelper = beaconHelper
        }

        /// Flutterから受け取ったCallbackDispatcherを保存
        @JvmStatic
        private fun initializeService(context: Context, callbackHandle: Long) {
            Timber.d("Initializing BeaconService")
            context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                .edit()
                .putLong(CALLBACK_DISPATCHER_HANDLE_KEY, callbackHandle)
                .apply()
        }

        @JvmStatic
        private fun bhInitialize(context: Context, param: Map<*, *>, beaconHelper: BeaconHelper) {
            Timber.d("Initializing Beacon Monitoring Params")
            val scanParamJson = Gson().toJson(param)
            beaconHelper.setScanPeriod(
                param["FgScanPeriod"] as Long,
                param["FgScanBetween"] as Long,
                param["BgScanPeriod"] as Long,
                param["BgScanBetween"] as Long,
            )
            context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                .edit()
                .putString(PERSISTENT_BEACON_PARAMS, scanParamJson)
                .apply()
        }

        @JvmStatic
        private fun bhRegisterRegions(
            args: List<*>,
            beaconHelper: BeaconHelper
        ) {
            Timber.d("Add Monitoring Regions")
            val regions = ArrayList<Region>()
            for (_r in args) {
                val regionData = _r as Map<*, *>
                val region = Region(
                    regionData["identifier"] as String,
                    Identifier.fromUuid(UUID.fromString(regionData["uuid"] as String)),
                    null,
                    null
                )
                regions.add(region)
            }

            beaconHelper.addRegions(regions)
        }

        @JvmStatic
        private fun bhStartMonitoring(
            args: List<*>,
            beaconHelper: BeaconHelper
        ) {
            Timber.d("Add Monitoring Regions")
            val regions = ArrayList<Region>()
            for (_r in args) {
                val regionData = _r as Map<*, *>
                val region = Region(
                    regionData["identifier"] as String,
                    Identifier.fromUuid(UUID.fromString(regionData["uuid"] as String)),
                    null,
                    null
                )
                regions.add(region)
            }

            beaconHelper.addRegions(regions)
        }

        fun reRegisterAfterReboot(context: Context) {
            synchronized(synchronizedCacheLock) {
                val prefs =
                    context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)

                // Initialize Setting of Beacon Monitoring
                val paramObj = prefs.getString(PERSISTENT_BEACON_PARAMS, null) ?: return
                val map: Map<String, Any> = HashMap()
                val params = Gson().fromJson(paramObj, map.javaClass)
                val beaconHelper = BeaconHelper()
                bhInitialize(context, params, beaconHelper)

                val persistentRegionIds = prefs.getStringSet(PERSISTENT_REGION_IDS, null)
                if(persistentRegionIds!=null) {
                    val regions = ArrayList<Map<*, *>>()
                    for (id in persistentRegionIds) {
                        val regionObj =
                            prefs.getString(getPersistentRegionKey(id), null) ?: continue
                        regions.add(Gson().fromJson(regionObj, map.javaClass))
                    }
                    bhRegisterRegions(regions, beaconHelper)
                }

            }
        }

        @JvmStatic
        private fun addRegionToCache(context: Context, id: String, args: ArrayList<*>) {
            synchronized(synchronizedCacheLock) {
                val prefs =
                    context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                val obj = JSONArray(args)

                val persistentRegions = prefs.getStringSet(PERSISTENT_REGION_IDS, null)
                val newPersistentRegions = if(persistentRegions==null) {
                    HashSet()
                } else {
                    HashSet(persistentRegions.toMutableSet())
                }
                newPersistentRegions.add(id)

                context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                    .edit()
                    .putStringSet(PERSISTENT_REGION_IDS, newPersistentRegions)
                    .putString(getPersistentRegionKey(id), obj.toString())
                    .apply()
            }
        }

        @JvmStatic
        private fun getPersistentRegionKey(id: String): String {
            return "persistent_region/$id"
        }


    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        val args = call.arguments<ArrayList<*>>()
        when (call.method) {
            "BeaconsPlugin.initialize" -> {
                // TODO Permissionの確認
                val callbackHandler = args[1] as Long
                initializeService(mContext!!, callbackHandler)

                val scanParam = args[1] as Map<*, *>
                bhInitialize(mContext!!, scanParam, mBeaconHelper!!)
                result.success(true)
            }
            "BeaconsPlugin.startScan" -> {
            }
            "BeaconsPlugin.startMonitoring" -> {
                val regionsData = args[1] as List<*>
                val regions = arrayListOf<Region>()
                regionsData.forEach {
                    val data = it as Map<*, *>
                    regions.add(
                        Region(
                            data["identifier"] as String,
                            Identifier.fromUuid(UUID.fromString(data["uuid"] as String)),
                            null,
                            null,
                        ),
                    )
                }
            }
            "BeaconsPlugin.stopMonitoring" -> {
            }
            "BeaconsPlugin.getRegisteredRegions" -> {
            }
            else -> result.notImplemented()
        }

    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        mContext = flutterPluginBinding.applicationContext
        mBeaconHelper = BeaconHelper()
        val channel = MethodChannel(flutterPluginBinding.binaryMessenger, channel)
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        mContext = null
        mBeaconHelper = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        mActivity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        mActivity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        mActivity = binding.activity
    }

    override fun onDetachedFromActivity() {
        mActivity = null
    }


}
