package jp.co.scico.beacons_detector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class BeaconRebootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals("android.intent.action.BOOT_COMPLETED")) {
            Timber.i("GEOFENCING REBOOT", "Reactivate BeaconService")
//            TODO 再起動後に実施する処理をxxPluginにContextを渡して実装
        }
    }
}
