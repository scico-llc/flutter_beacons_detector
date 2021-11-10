package jp.co.scico.beacons_detector

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)

fun isLocationGranted(context: Context): Boolean {
    val fineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    )==PackageManager.PERMISSION_GRANTED

    val coarseLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )==PackageManager.PERMISSION_GRANTED

    return  fineLocation && coarseLocation

}

// Check the device have Bluetooth feature or not
fun hasBLEFeature(activity: Context): Boolean {
    activity.packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }
        ?.also {
            return false
        }
    return true
}

// Check BLE is activated or not
fun isBluetoothEnabled(content: Context) {
    fun setUpBlueToothAdapter(content: Context): BluetoothAdapter? {
        val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
            val bluetoothManager =
                content.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }
        return bluetoothAdapter
    }


    setUpBlueToothAdapter(content)?.takeIf { !it.isEnabled }?.apply {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        content.startActivity(enableBtIntent)
    }
}
