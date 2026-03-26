package com.opencontacts.core.common

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast

private const val DEFAULT_DIALER_REQUEST_CODE = 4042

fun isOpenContactsDefaultDialer(context: Context): Boolean {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    return telecomManager?.defaultDialerPackage == context.packageName
}

fun startInternalCallOrPrompt(context: Context, phone: String?): Boolean {
    val number = phone?.trim().orEmpty()
    if (number.isBlank()) return false
    val uri = Uri.parse("tel:$number")
    if (isOpenContactsDefaultDialer(context)) {
        return runCatching {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecomManager.placeCall(uri, null)
            true
        }.getOrElse {
            Toast.makeText(context, "Unable to place the call right now.", Toast.LENGTH_SHORT).show()
            false
        }
    }
    Toast.makeText(context, "Set OpenContacts as the default phone app to place calls inside the app.", Toast.LENGTH_LONG).show()
    val activity = context as? Activity
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            if (activity != null) {
                activity.startActivityForResult(intent, DEFAULT_DIALER_REQUEST_CODE)
            } else {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            return false
        }
    }
    val fallback = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(fallback)
    return false
}
