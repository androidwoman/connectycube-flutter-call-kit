package com.connectycube.flutter.connectycube_flutter_call_kit

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.connectycube.flutter.connectycube_flutter_call_kit.background_isolates.ConnectycubeFlutterBgPerformingService
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.isApplicationForeground


class EventReceiver : BroadcastReceiver() {
    private val TAG = "EventReceiver"
    override fun onReceive(context: Context, intent: Intent?) {

        if (intent == null || TextUtils.isEmpty(intent.action)) return

        when (intent.action) {
            ACTION_CALL_REJECT -> {
                val extras = intent.extras
                val callId = extras?.getString(EXTRA_CALL_ID)
                val callType = extras?.getInt(EXTRA_CALL_TYPE)
                val callInitiatorId = extras?.getInt(EXTRA_CALL_INITIATOR_ID)
                val callInitiatorName = extras?.getString(EXTRA_CALL_INITIATOR_NAME)
                val callOpponents = extras?.getIntegerArrayList(EXTRA_CALL_OPPONENTS)
                val userInfo = extras?.getString(EXTRA_CALL_USER_INFO)
                Log.i(TAG, "NotificationReceiver onReceive Call REJECT, callId: $callId")

                val broadcastIntent = Intent(ACTION_CALL_REJECT)
                val bundle = Bundle()
                bundle.putString(EXTRA_CALL_ID, callId)
                bundle.putInt(EXTRA_CALL_TYPE, callType!!)
                bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId!!)
                bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
                bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, callOpponents)
                bundle.putString(EXTRA_CALL_USER_INFO, userInfo)
                broadcastIntent.putExtras(bundle)

                LocalBroadcastManager.getInstance(context.applicationContext)
                    .sendBroadcast(broadcastIntent)

                NotificationManagerCompat.from(context).cancel(callId.hashCode())

                processCallEnded(context, callId!!)

                if (!isApplicationForeground(context)) {
                    broadcastIntent.putExtra("userCallbackHandleName", REJECTED_IN_BACKGROUND)
                    ConnectycubeFlutterBgPerformingService.enqueueMessageProcessing(
                        context,
                        broadcastIntent
                    )
                }
            }

            ACTION_CALL_ACCEPT -> {
                val extras = intent.extras
                val callId = extras?.getString(EXTRA_CALL_ID)
                val callType = extras?.getInt(EXTRA_CALL_TYPE)
                val callInitiatorId = extras?.getInt(EXTRA_CALL_INITIATOR_ID)
                val callInitiatorName = extras?.getString(EXTRA_CALL_INITIATOR_NAME)
                val callOpponents = extras?.getIntegerArrayList(EXTRA_CALL_OPPONENTS)
                val userInfo = extras?.getString(EXTRA_CALL_USER_INFO)
                Log.i(TAG, "NotificationReceiver onReceive Call ACCEPT, callId: $callId")

                val broadcastIntent = Intent(ACTION_CALL_ACCEPT)
                val bundle = Bundle()
                bundle.putString(EXTRA_CALL_ID, callId)
                bundle.putInt(EXTRA_CALL_TYPE, callType!!)
                bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId!!)
                bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
                bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, callOpponents)
                bundle.putString(EXTRA_CALL_USER_INFO, userInfo)
                broadcastIntent.putExtras(bundle)
                NotificationManagerCompat.from(context).cancel(callId.hashCode())
                if (Build.VERSION.SDK_INT >= 31 && PermissionChecker.checkSelfPermission(
                        context,
                        Manifest.permission.SYSTEM_ALERT_WINDOW
                    )
                    != PermissionChecker.PERMISSION_GRANTED
                ) {
                    Log.d(
                        "Permission",
                        "Permission SYSTEM_ALERT_WINDOW not granted new notification build"
                    )
                    val intent = getLaunchIntent(context)

                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        callId.hashCode(),
                        intent,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT

                    )
                    val resID =
                        context.resources.getIdentifier(
                            "ic_launcher_foreground",
                            "drawable",
                            context.packageName
                        )
                    val notificationBuilder = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
                    notificationBuilder
                        .setSmallIcon(resID)
                        .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                        .setContentTitle("Permission")
                        .setContentText("hey permission not generated please open the app and accept the call or tap on notification to open the app")
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setTimeoutAfter(60000)
                    NotificationManagerCompat.from(context)
                        .notify(callId.hashCode(), notificationBuilder.build())
                } else {


                    LocalBroadcastManager.getInstance(context.applicationContext)
                        .sendBroadcast(broadcastIntent)



                    saveCallState(context, callId!!, CALL_STATE_ACCEPTED)

                    if (!isApplicationForeground(context)) {
                        broadcastIntent.putExtra("userCallbackHandleName", ACCEPTED_IN_BACKGROUND)
                        ConnectycubeFlutterBgPerformingService.enqueueMessageProcessing(
                            context,
                            broadcastIntent
                        )
                    }

                    val launchIntent = getLaunchIntent(context)
                    launchIntent?.action = ACTION_CALL_ACCEPT
                    context.startActivity(launchIntent)
                }
            }

            ACTION_CALL_NOTIFICATION_CANCELED -> {
                val extras = intent.extras
                val callId = extras?.getString(EXTRA_CALL_ID)
                val callType = extras?.getInt(EXTRA_CALL_TYPE)
                val callInitiatorId = extras?.getInt(EXTRA_CALL_INITIATOR_ID)
                val callInitiatorName = extras?.getString(EXTRA_CALL_INITIATOR_NAME)
                val userInfo = extras?.getString(EXTRA_CALL_USER_INFO)
                Log.i(
                    TAG,
                    "NotificationReceiver onReceive Delete Call Notification, callId: $callId"
                )
                LocalBroadcastManager.getInstance(context.applicationContext)
                    .sendBroadcast(
                        Intent(ACTION_CALL_NOTIFICATION_CANCELED).putExtra(
                            EXTRA_CALL_ID,
                            callId
                        )
                    )
            }
        }
    }
}
