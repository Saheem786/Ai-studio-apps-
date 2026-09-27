package com.example.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

class DeviceActionManager(private val context: Context) {

    companion object {
        private const val TAG = "ArushiDeviceActions"

        // Safe allowlist of common applications mapped to known package names or intents
        private val ALLOWED_APPS = mapOf(
            "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "youtube" to listOf("com.google.android.youtube"),
            "instagram" to listOf("com.instagram.android"),
            "maps" to listOf("com.google.android.apps.maps"),
            "google maps" to listOf("com.google.android.apps.maps"),
            "chrome" to listOf("com.android.chrome"),
            "browser" to listOf("com.android.chrome"),
            "camera" to listOf("android.media.action.IMAGE_CAPTURE"),
            "calculator" to listOf("com.google.android.calculator", "com.android.calculator2"),
            "settings" to listOf("android.settings.SETTINGS"),
            "clock" to listOf("com.google.android.deskclock", "com.android.deskclock"),
            "alarm" to listOf("com.google.android.deskclock", "com.android.deskclock"),
            "spotify" to listOf("com.spotify.music"),
            "phone" to listOf("android.intent.action.DIAL"),
            "dialer" to listOf("android.intent.action.DIAL")
        )
    }

    /**
     * Open WhatsApp directly or via deep link
     */
    fun openWhatsApp(): DeviceActionResult {
        Log.d(TAG, "Executing openWhatsApp action")
        val pm = context.packageManager
        val packages = listOf("com.whatsapp", "com.whatsapp.w4b")

        for (pkg in packages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return try {
                    context.startActivity(launchIntent)
                    Log.i(TAG, "WhatsApp opened successfully via package $pkg")
                    DeviceActionResult.Success("openWhatsApp", "WhatsApp opened successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch WhatsApp package $pkg", e)
                    DeviceActionResult.Failure("openWhatsApp", "Could not launch WhatsApp: ${e.message}")
                }
            }
        }

        // Fallback: WhatsApp URL scheme or web
        return try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            Log.i(TAG, "WhatsApp opened via web deep link")
            DeviceActionResult.Success("openWhatsApp", "WhatsApp opened via web link")
        } catch (e: Exception) {
            Log.w(TAG, "WhatsApp is not installed and fallback failed", e)
            DeviceActionResult.Failure("openWhatsApp", "WhatsApp is not installed on this device.")
        }
    }

    /**
     * Safely open an application from the allowlist
     */
    fun openApp(rawAppName: String): DeviceActionResult {
        val appName = rawAppName.trim().lowercase()
        Log.d(TAG, "Executing openApp for '$appName'")

        if (appName.contains("whatsapp")) {
            return openWhatsApp()
        }

        val candidates = ALLOWED_APPS.entries.firstOrNull { (key, _) ->
            appName.contains(key) || key.contains(appName)
        }?.value

        if (candidates == null) {
            Log.w(TAG, "App '$appName' is not in the security allowlist")
            return DeviceActionResult.Failure(
                "openApp",
                "App '$rawAppName' is not in the supported applications list. Supported: WhatsApp, YouTube, Instagram, Maps, Chrome, Camera, Calculator, Settings, Clock, Spotify."
            )
        }

        val pm = context.packageManager
        for (candidate in candidates) {
            // Check if it's a generic action intent
            if (candidate.startsWith("android.")) {
                try {
                    val actionIntent = Intent(candidate).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (actionIntent.resolveActivity(pm) != null) {
                        context.startActivity(actionIntent)
                        return DeviceActionResult.Success("openApp", "Opened $rawAppName")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to trigger action intent $candidate", e)
                }
            }

            // Check package launch intent
            val launchIntent = pm.getLaunchIntentForPackage(candidate)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return try {
                    context.startActivity(launchIntent)
                    Log.i(TAG, "Opened app $rawAppName successfully ($candidate)")
                    DeviceActionResult.Success("openApp", "Opened $rawAppName successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening $rawAppName", e)
                    DeviceActionResult.Failure("openApp", "Failed to start $rawAppName: ${e.message}")
                }
            }
        }

        return DeviceActionResult.Failure("openApp", "$rawAppName is not installed on this device.")
    }

    /**
     * Safely open a validated URL in the browser
     */
    fun openWebsite(rawUrl: String): DeviceActionResult {
        Log.d(TAG, "Executing openWebsite for '$rawUrl'")
        var cleanUrl = rawUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        return try {
            val uri = Uri.parse(cleanUrl)
            if (uri.scheme != "http" && uri.scheme != "https") {
                return DeviceActionResult.Failure("openWebsite", "Invalid URL scheme. Only HTTP and HTTPS are permitted.")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "Opened website: $cleanUrl")
            DeviceActionResult.Success("openWebsite", "Opened $cleanUrl in browser")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open website $cleanUrl", e)
            DeviceActionResult.Failure("openWebsite", "Unable to open website: ${e.message}")
        }
    }

    /**
     * Initiate phone dialer with phone number
     */
    fun makeCall(rawPhoneNumber: String): DeviceActionResult {
        Log.d(TAG, "Executing makeCall for '$rawPhoneNumber'")
        val cleanNumber = rawPhoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isEmpty()) {
            return DeviceActionResult.Failure("makeCall", "Invalid phone number provided")
        }

        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "Dialer opened for $cleanNumber")
            DeviceActionResult.Success("makeCall", "Opening dialer for $cleanNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch dialer", e)
            DeviceActionResult.Failure("makeCall", "Could not start call: ${e.message}")
        }
    }

    /**
     * Search contacts by name and initiate call
     */
    fun callContact(contactName: String): DeviceActionResult {
        Log.d(TAG, "Executing callContact for query '$contactName'")
        val cleanQuery = contactName.trim()
        if (cleanQuery.isEmpty()) {
            return DeviceActionResult.Failure("callContact", "Please specify a contact name")
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "READ_CONTACTS permission is required to search contacts")
            return DeviceActionResult.PermissionRequired(
                android.Manifest.permission.READ_CONTACTS,
                "Permission needed to access contacts to call $cleanQuery"
            )
        }

        val matchingContacts = searchContacts(cleanQuery)
        Log.d(TAG, "Found ${matchingContacts.size} contacts matching '$cleanQuery'")

        return when {
            matchingContacts.isEmpty() -> {
                DeviceActionResult.ContactNotFound(cleanQuery)
            }
            matchingContacts.size == 1 -> {
                val contact = matchingContacts.first()
                makeCall(contact.phoneNumber)
                DeviceActionResult.ContactFound(
                    contact,
                    "Calling ${contact.name} (${contact.phoneNumber})"
                )
            }
            else -> {
                // Multiple matching contacts: Never guess! Let Arushi ask the user
                DeviceActionResult.MultipleContactsFound(matchingContacts, cleanQuery)
            }
        }
    }

    private fun searchContacts(query: String): List<ContactItem> {
        val results = mutableListOf<ContactItem>()
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        try {
            contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext() && results.size < 10) {
                    val id = if (idIdx != -1) cursor.getString(idIdx) ?: "" else ""
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) ?: "" else ""
                    val number = if (numIdx != -1) cursor.getString(numIdx) ?: "" else ""
                    if (name.isNotBlank() && number.isNotBlank()) {
                        results.add(ContactItem(id = id, name = name, phoneNumber = number))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts", e)
        }

        return results.distinctBy { it.phoneNumber }
    }
}
