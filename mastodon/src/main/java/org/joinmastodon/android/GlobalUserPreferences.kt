package org.joinmastodon.android

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIController.gson
import java.lang.reflect.Type

class GlobalUserPreferences {
    companion object {
        @JvmField
        var playGifs: Boolean = false

        @JvmField
        var useCustomTabs: Boolean = false

        @JvmField
        var trueBlackTheme: Boolean = false

        @JvmField
        var showReplies: Boolean = false

        @JvmField
        var showBoosts: Boolean = false

        @JvmField
        var loadNewPosts: Boolean = false

        @JvmField
        var showFederatedTimeline: Boolean = false

        @JvmField
        var showInteractionCounts: Boolean = false

        @JvmField
        var alwaysExpandContentWarnings: Boolean = false

        @JvmField
        var disableMarquee: Boolean = false

        @JvmField
        var disableSwipe: Boolean = false

        @JvmField
        var voteButtonForSingleChoice: Boolean = false

        @JvmStatic
        lateinit var theme: ThemePreference

        @JvmStatic
        lateinit var color: ColorPreference

        @JvmStatic
        private val recentLanguageTypes =
            object : TypeToken<Map<String, List<String>>>() {}.type

        @JvmField
        val recentLanguages: Map<String, List<String>> =
            fromJson(getPrefs().getString("recentLanguages", "{}")!!, recentLanguageTypes, HashMap())

        @JvmStatic
        private fun getPrefs(): SharedPreferences {
            return MastodonApp.context!!.getSharedPreferences("global", Context.MODE_PRIVATE)
        }

        @JvmStatic
        private fun <T> fromJson(json: String, type: Type, orElse: T): T {
            return try {
                gson.fromJson(json, type)
            } catch (ignored: JsonSyntaxException) {
                orElse
            }
        }

        @JvmStatic
        fun load() {
            val prefs = getPrefs()
            playGifs = prefs.getBoolean("playGifs", true)
            useCustomTabs = prefs.getBoolean("useCustomTabs", true)
            trueBlackTheme = prefs.getBoolean("trueBlackTheme", false)
            showReplies = prefs.getBoolean("showReplies", true)
            showBoosts = prefs.getBoolean("showBoosts", true)
            loadNewPosts = prefs.getBoolean("loadNewPosts", true)
            showFederatedTimeline = prefs.getBoolean(
                "showFederatedTimeline",
                BuildConfig.BUILD_TYPE != "playRelease"
            )
            showInteractionCounts = prefs.getBoolean("showInteractionCounts", false)
            alwaysExpandContentWarnings = prefs.getBoolean("alwaysExpandContentWarnings", false)
            disableMarquee = prefs.getBoolean("disableMarquee", false)
            disableSwipe = prefs.getBoolean("disableSwipe", false)
            voteButtonForSingleChoice = prefs.getBoolean("voteButtonForSingleChoice", true)
            theme = ThemePreference.values()[prefs.getInt("theme", 0)]

            color = try {
                ColorPreference.valueOf(prefs.getString("color", ColorPreference.PINK.name)!!)
            } catch (_: IllegalArgumentException) {
                // invalid color name or color was previously saved as integer
                ColorPreference.PINK
            } catch (_: ClassCastException) {
                ColorPreference.PINK
            }
        }

        @JvmStatic
        fun save() {
            getPrefs().edit()
                .putBoolean("playGifs", playGifs)
                .putBoolean("useCustomTabs", useCustomTabs)
                .putBoolean("showReplies", showReplies)
                .putBoolean("showBoosts", showBoosts)
                .putBoolean("loadNewPosts", loadNewPosts)
                .putBoolean("showFederatedTimeline", showFederatedTimeline)
                .putBoolean("trueBlackTheme", trueBlackTheme)
                .putBoolean("showInteractionCounts", showInteractionCounts)
                .putBoolean("alwaysExpandContentWarnings", alwaysExpandContentWarnings)
                .putBoolean("disableMarquee", disableMarquee)
                .putBoolean("disableSwipe", disableSwipe)
                .putInt("theme", theme.ordinal)
                .putString("color", color.name)
                .putString("recentLanguages", gson.toJson(recentLanguages))
                .apply();
        }
    }

    enum class ColorPreference {
        MATERIAL3,
        PINK,
        PURPLE,
        GREEN,
        BLUE,
        BROWN,
        RED,
        YELLOW,
        SUSSYRED
    }

    enum class ThemePreference {
        AUTO,
        LIGHT,
        DARK
    }
}