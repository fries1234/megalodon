package org.joinmastodon.android

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.util.Log
import me.grishka.appkit.api.Callback
import me.grishka.appkit.api.ErrorResponse
import me.grishka.appkit.imageloader.ImageCache
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest
import me.grishka.appkit.utils.V
import org.joinmastodon.android.api.MastodonAPIController
import org.joinmastodon.android.api.requests.notifications.GetNotificationByID
import org.joinmastodon.android.api.session.AccountSession
import org.joinmastodon.android.api.session.AccountSessionManager
import org.joinmastodon.android.model.Notification
import org.joinmastodon.android.model.PushNotification
import org.joinmastodon.android.ui.utils.UiUtils
import org.parceler.Parcels

class PushNotificationReceiver : BroadcastReceiver() {
    // this companion is where the consts live...
    companion object {
        private const val TAG = "PushNotificationReceive"
        const val NOTIFICATION_ID = 178
    }

    override fun onReceive(context: Context, intent: Intent) {
        // prints debug information if your build is a debug build
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "received intent: $intent")
            val extras = intent.extras;
            for (key in extras!!.keySet()) {
                Log.i(TAG, "$key -> ${extras[key]}")
            }
        }

        // checks if the intents action is equal to a Firebase Cloud Messaging receive intent
        // which somehow still uses the same id as googles older cloud messaging serivces
        if ("com.google.android.c2dm.intent.RECEIVE" == intent.action) {
            // get some badly named intent strings and put them into read only vars
            val serverKey = intent.getStringExtra("k")
            val payload = intent.getStringExtra("p")
            val salt = intent.getStringExtra("s")
            val pushAccountID = intent.getStringExtra("x")

            // checks if all these strings arent empty
            if (!TextUtils.isEmpty(pushAccountID) &&
                !TextUtils.isEmpty(serverKey) &&
                !TextUtils.isEmpty(payload) &&
                !TextUtils.isEmpty(salt)
            ) {
                MastodonAPIController.runInBackground {
                    try {
                        // get a list of logged in accounts
                        val accounts = AccountSessionManager.getInstance().loggedInAccounts
                        // make a null var that might contain an AccountSession
                        var account: AccountSession? = null

                        for (acc in accounts) {
                            // check if account id linked to the notification intent
                            // matches the current acc in the loop
                            if (pushAccountID == acc.pushAccountID) {
                                account = acc
                                break
                            }
                        }

                        // no account = no notification
                        if (account == null) {
                            Log.w(TAG, "onReceive: account for id $pushAccountID not found...")
                            return@runInBackground
                        }

                        // get a PushNotification object from an encrypted notification
                        val pushNotification = AccountSessionManager.getInstance()
                            .getAccount(account.id)
                            .pushSubscriptionManager
                            .decryptNotification(serverKey, payload, salt)
                        GetNotificationByID(pushNotification.notificationId.toString())
                            .setCallback(object : Callback<Notification> {
                                override fun onSuccess(result: Notification) {
                                    MastodonAPIController.runInBackground {
                                        notify(context, pushNotification, account.id, result)
                                    }
                                }

                                override fun onError(error: ErrorResponse?) {
                                    MastodonAPIController.runInBackground {
                                        notify(context, pushNotification, account.id, null)
                                    }
                                }
                            }).exec(account.id)
                    } catch (x: java.lang.Exception) {
                        Log.w(TAG, x)
                    }
                }
            } else {
                Log.w(TAG, "onReceive: invalid push notification format...")
            }
        }
    }

    private fun notify(
        context: Context,
        pushNotification: PushNotification,
        accountID: String,
        notification: Notification?
    ) {
        // get a android notification manager object
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val accSession = AccountSessionManager.getInstance().getAccount(accountID)
        val accountName = "@${accSession.self.username}@${accSession.self.domain}"
        var builder: android.app.Notification.Builder

        // checks if your android version is greater then or equal to Android 8 (Oreo)
        // if so, use notification channels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var hasGroup = false
            val channelGroups = notificationManager.notificationChannelGroups

            for (group in channelGroups) {
                if (group.id == accountID) {
                    hasGroup = true
                    break
                }
            }

            if (!hasGroup) {
                val group = NotificationChannelGroup(accountID, accountName)
                notificationManager.createNotificationChannelGroup(group)
                // gets a list of channels using cool kotlin map features
                val channels = PushNotification.Type.values().map { type ->
                    val channel = NotificationChannel(
                        "${accountID}_${type}",
                        context.getString(type.localizedName),
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    channel.group = accountID
                    channel
                }
                notificationManager.createNotificationChannels(channels)
            }

            builder = android.app.Notification.Builder(
                context,
                "${accountID}_${pushNotification.notificationType}"
            )
        } else {
            // well, youre not on android 8+
            // old deprecated notification api for you....
            builder = android.app.Notification.Builder(context)
                .setPriority(android.app.Notification.PRIORITY_DEFAULT)
                .setDefaults(android.app.Notification.DEFAULT_SOUND or android.app.Notification.DEFAULT_VIBRATE)
        }

        val avatar = ImageCache.getInstance(context)
            .get(UrlImageLoaderRequest(pushNotification.icon, V.dp(50f), V.dp(50f)))
        val contentIntent = Intent(context, MainActivity::class.java)
        contentIntent
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("fromNotification", true)
            .putExtra("accountID", accountID)

        if (notification != null) {
            contentIntent.putExtra("notification", Parcels.wrap(notification))
        }

        builder
            .setContentTitle(pushNotification.title)
            .setContentText(pushNotification.body)
            .setStyle(android.app.Notification.BigTextStyle().bigText(pushNotification.body))
            .setSmallIcon(R.drawable.ic_ntf_logo)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    accountID.hashCode() and 0xFFFF,
                    contentIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setWhen(notification?.createdAt?.toEpochMilli() ?: System.currentTimeMillis())
            .setShowWhen(true)
            .setCategory(android.app.Notification.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.primary_700))

        if (avatar != null) {
            builder.setLargeIcon(UiUtils.getBitmapFromDrawable(avatar))
        }

        // set the subtext to your account name if you have more then 1 account
        // logged in
        if (AccountSessionManager.getInstance().loggedInAccounts.size > 1) {
            builder.setSubText(accountName)
        }

        // this makes your device do the notification thing
        notificationManager.notify(accountID, NOTIFICATION_ID, builder.build())
    }
}