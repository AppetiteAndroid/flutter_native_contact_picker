package com.jayesh.flutter_contact_picker

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.provider.ContactsContract
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.PluginRegistry
import java.util.*

/** FlutterContactPickerPlugin */
class FlutterContactPickerPlugin : FlutterPlugin, MethodCallHandler, ActivityAware,
    PluginRegistry.ActivityResultListener {

    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var pendingResult: Result? = null
    private val PICK_CONTACT = 2015

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(
            flutterPluginBinding.binaryMessenger,
            "flutter_native_contact_picker"
        )
        channel.setMethodCallHandler(this);
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "selectContact") {
            if (pendingResult != null) {
                pendingResult!!.error("multiple_requests", "Cancelled by a second request.", null)
                pendingResult = null
            }
            pendingResult = result

            val i = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            activity?.startActivityForResult(i, PICK_CONTACT)
        } else {
            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(@NonNull p0: ActivityPluginBinding) {
        this.activity = p0.activity
        p0.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
//    p0.removeActivityResultListener(this)
        this.activity = null
    }

    override fun onReattachedToActivityForConfigChanges(activityPluginBinding: ActivityPluginBinding) {
        this.activity = activityPluginBinding.activity
        activityPluginBinding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        this.activity = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        try {
            if (requestCode != PICK_CONTACT) {
                return false
            }
            if (resultCode != RESULT_OK) {
                pendingResult?.success(null)
                pendingResult = null
                return true
            }

            data?.data?.let { contactUri ->
                val cursor = activity?.contentResolver?.query(contactUri, null, null, null, null);
                cursor?.use {
                    it.moveToFirst()
                    val numberIndex =
                        it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nameIndex =
                        it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    if (numberIndex >= 0) {

                        val number =
                            it.getString(numberIndex)

                        val fullName = if (nameIndex >= 0)
                            it.getString(nameIndex)
                        else
                            "Unknown"

                        val contact = HashMap<String, Any>()
                        contact.put("fullName", fullName)
                        contact.put("phoneNumbers", listOf(number))
                        pendingResult?.success(contact)
                        pendingResult = null
                        return@use true
                    }
                    return@use false
                }
            }
        } catch (e: Exception) {

        }

        pendingResult?.success(null)
        pendingResult = null
        return true
    }
}