package jp.co.scico.beacons_detector

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.JobIntentService
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.view.FlutterCallbackInformation
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class BeaconDetectorService : MethodChannel.MethodCallHandler, Service() {
    private val queue = ArrayDeque<List<Any>>()
    private lateinit var mBackgroundChannel: MethodChannel
    private lateinit var mContext: Context
    private val channel: String = "plugins.flutter.io/beacons_detector_bg"


    companion object {
        @JvmStatic
        private val TAG = "GeofencingService"

        @JvmStatic
        private val JOB_ID = UUID.randomUUID().mostSignificantBits.toInt()

        @JvmStatic
        private var sBackgroundFlutterEngine: FlutterEngine? = null

        @JvmStatic
        private val sServiceStarted = AtomicBoolean(false)

//        @JvmStatic
//        fun enqueueWork(context: Context, work: Intent) {
//            enqueueWork(context, BeaconDetectorService::class.java, JOB_ID, work)
//        }
    }

    override fun onCreate() {
        super.onCreate()
        startMonitoring(this)
    }

    override fun onBind(p0: Intent?): IBinder? {
       return null
    }

    fun onBeaconDataReceived() {
//        val callbackHandle = intent.getLongExtra(BeaconsDetectorPlugin.CALLBACK_HANDLE_KEY, 0)
//        val beaconUpdateList = listOf(callbackHandle)
//
//        synchronized(sServiceStarted) {
//            if (!sServiceStarted.get()) {
//                // Queue up geofencing events while background isolate is starting
//                queue.add(beaconUpdateList)
//            } else {
//                // Callback method name is intentionally left blank.
//                Handler(mContext.mainLooper).post { mBackgroundChannel.invokeMethod("", beaconUpdateList) }
//            }
//        }
    }

    private fun startMonitoring(context: Context) {
        synchronized(sServiceStarted) {
            mContext = context

            // Callback Dispatcherを登録
            if(sBackgroundFlutterEngine==null) {
                val callbackHandle = context.getSharedPreferences(
                    BeaconsDetectorPlugin.SHARED_PREFERENCES_KEY,
                    Context.MODE_PRIVATE
                )
                    .getLong(BeaconsDetectorPlugin.CALLBACK_DISPATCHER_HANDLE_KEY, 0)
                if(callbackHandle==0L) {
                    Timber.e(TAG, "Fatal: no callback registered")
                    return
                }

                val callbackInfo =
                    FlutterCallbackInformation.lookupCallbackInformation(callbackHandle)
                if(callbackInfo==null) {
                    Timber.e(TAG, "Fatal: failed to find callback")
                    return
                }
                Timber.i(TAG, "Starting GeofencingService...")
                sBackgroundFlutterEngine = FlutterEngine(context)

                val args = DartExecutor.DartCallback(
                    context.assets,
                    FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                    callbackInfo
                )
                sBackgroundFlutterEngine!!.dartExecutor.executeDartCallback(args)
                IsolateHolderService.setBackgroundFlutterEngine(sBackgroundFlutterEngine)
            }
        }
        mBackgroundChannel =
            MethodChannel(sBackgroundFlutterEngine!!.dartExecutor.binaryMessenger, channel)
        mBackgroundChannel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when(call.method) {
            "BeaconService.initialized" -> {
                synchronized(sServiceStarted) {
                    while (!queue.isEmpty()) {
                        mBackgroundChannel.invokeMethod("", queue.remove())
                    }
                    sServiceStarted.set(true)
                }
            }
            "BeaconService.promoteToForeground" -> {
                mContext.startForegroundService(Intent(mContext, IsolateHolderService::class.java))
            }
            "BeaconService.demoteToBackground" -> {
                val intent = Intent(mContext, IsolateHolderService::class.java)
                intent.action = IsolateHolderService.ACTION_SHUTDOWN
                mContext.startForegroundService(intent)
            }
            else -> result.notImplemented()
        }
        result.success(null)
    }
}
