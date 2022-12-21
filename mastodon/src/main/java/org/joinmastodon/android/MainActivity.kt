package org.joinmastodon.android

import android.app.Fragment
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import me.grishka.appkit.FragmentStackActivity
import org.joinmastodon.android.api.ObjectValidationException
import org.joinmastodon.android.api.session.AccountSession
import org.joinmastodon.android.api.session.AccountSessionManager
import org.joinmastodon.android.fragments.ComposeFragment
import org.joinmastodon.android.fragments.HomeFragment
import org.joinmastodon.android.fragments.ProfileFragment
import org.joinmastodon.android.fragments.ThreadFragment
import org.joinmastodon.android.fragments.onboarding.AccountActivationFragment
import org.joinmastodon.android.fragments.onboarding.CustomWelcomeFragment
import org.joinmastodon.android.model.Notification
import org.joinmastodon.android.ui.utils.UiUtils
import org.joinmastodon.android.updater.GithubSelfUpdater
import org.parceler.Parcels

class MainActivity : FragmentStackActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        UiUtils.setUserPreferredTheme(this)
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            if (AccountSessionManager.getInstance().loggedInAccounts.isEmpty()) {
                showFragmentClearingBackStack(CustomWelcomeFragment())
            } else {
                AccountSessionManager.getInstance().maybeUpdateLocalInfo()
                var session: AccountSession
                val args = Bundle()

                if (intent.getBooleanExtra("fromNotification", false)) {
                    val accountID = intent.getStringExtra("accountID")
                    try {
                        session = AccountSessionManager.getInstance().getAccount(accountID)
                        if (!intent.hasExtra("notification"))
                            args.putString("tab", "notifications")
                    } catch (x: java.lang.IllegalStateException) {
                        session = AccountSessionManager.getInstance().lastActiveAccount!!
                    }
                } else {
                    session = AccountSessionManager.getInstance().lastActiveAccount!!
                }

                args.putString("account", session.id)
                val fragment =
                    if (session.activated) HomeFragment() else AccountActivationFragment()
                fragment.arguments = args
                showFragmentClearingBackStack(fragment)

                if (intent.getBooleanExtra(
                        "fromNotification",
                        false
                    ) && intent.hasExtra("notification")
                ) {
                    val notification =
                        Parcels.unwrap<Notification>(intent.getParcelableExtra("notification"))
                    showFragmentForNotification(notification, session.id)
                } else if (intent.getBooleanExtra("compose", false)) {
                    showCompose()
                } else {
                    maybeRequestNotificationsPermission()
                }
            }
        }

        if (GithubSelfUpdater.needSelfUpdating()) {
            GithubSelfUpdater.getInstance().maybeCheckForUpdates()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.getBooleanExtra("fromNotification", false)) {
            val accountID = intent.getStringExtra("accountID")

            try {
                AccountSessionManager.getInstance().getAccount(accountID)
            } catch (_: java.lang.IllegalStateException) {
                return
            }

            if (intent.hasExtra("notification")) {
                val notification =
                    Parcels.unwrap<Notification>(intent.getParcelableExtra("notification"))
                showFragmentForNotification(notification, accountID!!)
            } else {
                AccountSessionManager.getInstance().lastActiveAccountID = accountID
                val args = Bundle()
                args.putString("account", accountID)
                args.putString("tab", "notifications")
                val fragment = HomeFragment()
                fragment.arguments = args
                showFragmentClearingBackStack(fragment)
            }
        } else if (intent.getBooleanExtra("compose", false)) {
            showCompose()
        }
    }

    private fun showFragmentForNotification(notification: Notification, accountID: String) {
        val fragment: Fragment
        val args = Bundle()
        args.putString("account", accountID)
        args.putBoolean("_can_go_back", true)

        try {
            notification.postprocess()
        } catch (x: ObjectValidationException) {
            Log.w("MainActivity", x)
            return
        }

        if (notification.status != null) {
            fragment = ThreadFragment()
            args.putParcelable("status", Parcels.wrap(notification.status))
        } else {
            fragment = ProfileFragment()
            args.putParcelable("profileAccount", Parcels.wrap(notification.account))
        }

        fragment.arguments = args
        showFragment(fragment)
    }

    private fun showCompose() {
        val session = AccountSessionManager.getInstance().lastActiveAccount
        if (session == null || !session.activated)
            return
        val compose = ComposeFragment()
        val composeArgs = Bundle()
        composeArgs.putString("account", session.id)
        compose.arguments = composeArgs
        showFragment(compose)
    }

    private fun maybeRequestNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf<String>(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }
}