package com.vsoft.trackify.activity

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.widget.Switch
import android.widget.Toast
import com.vsoft.trackify.R
import com.vsoft.trackify.model.User
import com.vsoft.trackify.service.LocationSharingService
import com.vsoft.trackify.util.PermissionUtils
import com.vsoft.trackify.util.ResolvableApiExceptionHolder


class SettingsActivity : Activity() {

    lateinit var user: User
    lateinit var locationShareSwitch: Switch

    private val REQUEST_CHECK_SETTINGS = 12

    private val broadcastReceiver: BroadcastReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val exception = ResolvableApiExceptionHolder.resolvableApiException
            exception.startResolutionForResult(this@SettingsActivity,REQUEST_CHECK_SETTINGS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val intent = intent
        user = intent.getSerializableExtra("user") as User

        locationShareSwitch = findViewById(R.id.switch_location_sharing)

        locationShareSwitch.isChecked = isSharingTurnedOn()

        locationShareSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                if(PermissionUtils.checkForLocationPermissions(this)){
                    turnLocationSharingOn()
                } else {
                    PermissionUtils.requestPermissions(this)
                }
            } else {
                // The toggle is disabled
                turnLocationSharingOff()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                object: IntentFilter("com.vsoft.trackify.service.RESOLVABLE_API_EXCEPTION"){})
    }

    override fun onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(broadcastReceiver)
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionUtils.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    turnLocationSharingOn()
                }
                else {
                    locationShareSwitch.isChecked = false
                }
            }
        }
    }

    private fun turnLocationSharingOff() {
        val sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean(getString(R.string.toggle_sharing), false)
            apply()
            Toast.makeText(applicationContext,"Location sharing turned off!",Toast.LENGTH_SHORT).show()
        }
        stopService(Intent(this,LocationSharingService::class.java))
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    private fun turnLocationSharingOn() {
        val sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean(getString(R.string.toggle_sharing), true)
            apply()
            Toast.makeText(applicationContext,"Location sharing turned on!",Toast.LENGTH_SHORT).show()
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,IntentFilter("com.vsoft.trackify.service.RESOLVABLE_API_EXCEPTION"))
        startService(Intent(this,LocationSharingService::class.java).apply { putExtra("user",user) })
    }

    private fun isSharingTurnedOn(): Boolean {
        val sharedPref = this.getSharedPreferences("com.vsoft.trackify.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
        return sharedPref.getBoolean(getString(R.string.toggle_sharing),false)
    }



}
