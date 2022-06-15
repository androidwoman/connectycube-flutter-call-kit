package com.connectycube.flutter.connectycube_flutter_call_kit

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getColorizedText
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getString
import java.io.File

const val CALL_CHANNEL_ID = "calls_channel_id"
const val CALL_CHANNEL_NAME = "Calls"


fun cancelCallNotification(context: Context, callId: String) {
    val notificationManager = NotificationManagerCompat.from(context)
    notificationManager.cancel(callId.hashCode())
}

fun showCallNotification(
    context: Context, callId: String, callType: Int, callInitiatorId: Int,
    callInitiatorName: String, callOpponents: ArrayList<Int>, userInfo: String, userImage: String,
) {
    val notificationManager = NotificationManagerCompat.from(context)

    val intent = getLaunchIntent(context)

    val pendingIntent = PendingIntent.getActivity(
        context,
        callId.hashCode(),
        intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT

    )

    val ringtone: Uri

    val customRingtone = getString(context, "ringtone")
    Log.d("NotificationsManager", "customRingtone $customRingtone")
    if (!TextUtils.isEmpty(customRingtone)) {
        ringtone = Uri.parse("android.resource://" + context.packageName + "/raw/" + customRingtone)
        Log.d("NotificationsManager", "ringtone 1 $ringtone")
    } else {
        ringtone = Settings.System.DEFAULT_RINGTONE_URI
    }

    Log.d("NotificationsManager", "ringtone 2 $ringtone")

    val callTypeTitle =
        String.format(CALL_TYPE_PLACEHOLDER, if (callType == 1) "Video" else "Audio")

    val builder: NotificationCompat.Builder =
        createCallNotification(
            context,
            callInitiatorName,
            callTypeTitle,
            pendingIntent,
            ringtone,
            userImage
        )

    // Add actions
    addCallRejectAction(
        context,
        builder,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        callOpponents,
        userInfo
    )
    addCallAcceptAction(
        context,
        builder,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        callOpponents,
        userInfo
    )

    // Add full screen intent (to show on lock screen)
    addCallFullScreenIntent(
        context,
        builder,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        callOpponents,
        userInfo,
        userImage
    )

    // Add action when delete call notification
    addCancelCallNotificationIntent(
        context,
        builder,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        userInfo
    )

    // Set small icon for notification
    setNotificationSmallIcon(context, builder)

    // Set notification color accent
    setNotificationColor(context, builder)

    createCallNotificationChannel(notificationManager, ringtone)

    notificationManager.notify(callId.hashCode(), builder.build())
}

fun getLaunchIntent(context: Context): Intent? {
    val packageName = context.packageName
    val packageManager: PackageManager = context.packageManager
    return packageManager.getLaunchIntentForPackage(packageName)
}

fun createCallNotification(
    context: Context,
    title: String,
    text: String?,
    pendingIntent: PendingIntent,
    ringtone: Uri,
    userImage: String,
): NotificationCompat.Builder {
    val largeIcon = if (userImage == "R.drawable.profile")
        BitmapFactory.decodeResource(context.resources, R.drawable.profile)
    else {
        BitmapFactory.decodeFile(File(userImage).absolutePath)
    }
    val notificationBuilder = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
    notificationBuilder
        .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
        .setContentTitle(title)
        .setContentText(text)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setAutoCancel(true)
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_CALL)
        .setContentIntent(pendingIntent)
        .setSound(ringtone)
        .setLargeIcon(largeIcon)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setTimeoutAfter(60000)
    return notificationBuilder
}

fun addCallRejectAction(
    context: Context,
    notificationBuilder: NotificationCompat.Builder,
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    opponents: ArrayList<Int>,
    userInfo: String
) {
    val bundle = Bundle()
    bundle.putString(EXTRA_CALL_ID, callId)
    bundle.putInt(EXTRA_CALL_TYPE, callType)
    bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
    bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
    bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, opponents)
    bundle.putString(EXTRA_CALL_USER_INFO, userInfo)

    val declinePendingIntent: PendingIntent = PendingIntent.getBroadcast(
        context,
        callId.hashCode(),
        Intent(context, EventReceiver::class.java)
            .setAction(ACTION_CALL_REJECT)
            .putExtras(bundle),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    )
    val declineAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
        context.resources.getIdentifier(
            "ic_menu_close_clear_cancel",
            "drawable",
            context.packageName
        ),
        getColorizedText("Reject", "#E02B00"),
        declinePendingIntent
    )
        .build()

    notificationBuilder.addAction(declineAction)
}

fun addCallAcceptAction(
    context: Context,
    notificationBuilder: NotificationCompat.Builder,
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    opponents: ArrayList<Int>,
    userInfo: String
) {
    val bundle = Bundle()
    bundle.putString(EXTRA_CALL_ID, callId)
    bundle.putInt(EXTRA_CALL_TYPE, callType)
    bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
    bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
    bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, opponents)
    bundle.putString(EXTRA_CALL_USER_INFO, userInfo)

    val acceptPendingIntent: PendingIntent = PendingIntent.getBroadcast(
        context,
        callId.hashCode(),
        Intent(context, EventReceiver::class.java)
            .setAction(ACTION_CALL_ACCEPT)
            .putExtras(bundle),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    )
    var color = "#4CB050"
    if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(
            context,
            Manifest.permission.SYSTEM_ALERT_WINDOW
        )
        != PermissionChecker.PERMISSION_GRANTED
    ) {
        Log.d("Permission", "Permission SYSTEM_ALERT_WINDOW not granted")
        color = "#FF6C6969"
    }

    val acceptAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
        context.resources.getIdentifier("ic_menu_call", "drawable", context.packageName),
        getColorizedText("Accept", color),
        acceptPendingIntent
    )

        .build()
    notificationBuilder.addAction(acceptAction)
}

fun addCallFullScreenIntent(
    context: Context,
    notificationBuilder: NotificationCompat.Builder,
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    callOpponents: ArrayList<Int>,
    userInfo: String,
    userImage: String,
) {
    val callFullScreenIntent: Intent = createStartIncomingScreenIntent(
        context,
        callId,
        callType,
        callInitiatorId,
        callInitiatorName,
        callOpponents,
        userInfo,
        userImage
    )
    val fullScreenPendingIntent = PendingIntent.getActivity(
        context,
        callId.hashCode(),
        callFullScreenIntent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    )
    notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
}

fun addCancelCallNotificationIntent(
    appContext: Context?,
    notificationBuilder: NotificationCompat.Builder,
    callId: String,
    callType: Int,
    callInitiatorId: Int,
    callInitiatorName: String,
    userInfo: String
) {
    val bundle = Bundle()
    bundle.putString(EXTRA_CALL_ID, callId)
    bundle.putInt(EXTRA_CALL_TYPE, callType)
    bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
    bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
    bundle.putString(EXTRA_CALL_USER_INFO, userInfo)

    val deleteCallNotificationPendingIntent = PendingIntent.getBroadcast(
        appContext,
        callId.hashCode(),
        Intent(appContext, EventReceiver::class.java)
            .setAction(ACTION_CALL_NOTIFICATION_CANCELED)
            .putExtras(bundle),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    )
    notificationBuilder.setDeleteIntent(deleteCallNotificationPendingIntent)
}

fun createCallNotificationChannel(notificationManager: NotificationManagerCompat, sound: Uri) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CALL_CHANNEL_ID,
            CALL_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.setSound(
            sound, AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
        )
        notificationManager.createNotificationChannel(channel)
    }
}

fun setNotificationSmallIcon(context: Context, notificationBuilder: NotificationCompat.Builder) {
    val resID =
        context.resources.getIdentifier("ic_launcher_foreground", "drawable", context.packageName)
    if (resID != 0) {
        notificationBuilder.setSmallIcon(resID)
    } else {
        notificationBuilder.setSmallIcon(context.applicationInfo.icon)
    }
}

fun setNotificationColor(context: Context, notificationBuilder: NotificationCompat.Builder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val color = getString(context, "color")

        if (!TextUtils.isEmpty(color)) {
            notificationBuilder.color = Color.parseColor(color)
        } else {
            val accentID = context.resources.getIdentifier(
                "call_notification_color_accent",
                "color",
                context.packageName
            )
            if (accentID != 0) {
                notificationBuilder.color = context.resources.getColor(accentID, null)
            } else {
                notificationBuilder.color = Color.parseColor("#4CAF50")
            }
        }
    }
}

