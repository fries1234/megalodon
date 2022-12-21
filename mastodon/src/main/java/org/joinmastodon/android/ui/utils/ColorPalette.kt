package org.joinmastodon.android.ui.utils

import android.content.Context
import androidx.annotation.StyleRes
import org.joinmastodon.android.GlobalUserPreferences.*
import org.joinmastodon.android.GlobalUserPreferences.Companion.trueBlackTheme
import org.joinmastodon.android.R

class ColorPalette {
    companion object {
        // this is annotated like this so java code can see this as a static property
        @JvmField
         val palettes = mapOf(
            ColorPreference.MATERIAL3 to ColorPalette(R.style.ColorPalette_Material3)
                .dark(
                    R.style.ColorPalette_Material3_Dark,
                    R.style.ColorPalette_Material3_AutoLightDark
                ),
            ColorPreference.PINK to ColorPalette(R.style.ColorPalette_Pink),
            ColorPreference.PURPLE to ColorPalette(R.style.ColorPalette_Purple),
            ColorPreference.GREEN to ColorPalette(R.style.ColorPalette_Green),
            ColorPreference.BLUE to ColorPalette(R.style.ColorPalette_Blue),
            ColorPreference.BROWN to ColorPalette(R.style.ColorPalette_Brown),
            ColorPreference.RED to ColorPalette(R.style.ColorPalette_Red),
            ColorPreference.YELLOW to ColorPalette(R.style.ColorPalette_Yellow),
            ColorPreference.SUSSYRED to ColorPalette(R.style.ColorPalette_SussyRed)
        )
    }

    @StyleRes
    private var base: Int? = null

    @StyleRes
    private var autoDark: Int? = null

    @StyleRes
    private var light: Int? = null

    @StyleRes
    private var dark: Int? = null

    @StyleRes
    private var black: Int? = null

    @StyleRes
    private var autoBlack: Int? = null

    constructor(@StyleRes baseRes: Int) {
        this.base = baseRes
    }

    fun light(@StyleRes res: Int): ColorPalette {
        this.light = res
        return this
    }

    fun dark(@StyleRes res: Int, @StyleRes auto: Int): ColorPalette {
        this.dark = res
        this.autoDark = auto
        return this
    }

    fun black(@StyleRes res: Int, @StyleRes auto: Int): ColorPalette {
        this.dark = res
        this.autoBlack = auto
        return this
    }

    fun apply(context: Context) {
        // a fancy kotlin function that throws an IllegalStateException for you
        check(dark != null && autoDark != null || black != null && autoBlack != null || light != null || base != null)
        {
            "Invalid color scheme definition"
        }

        // use fancy kotlin stuff to call get the theme as a property
        val theme = context.theme
        // if the base resource is not null, apply the style
        // this is usually whats executed as the palettes map
        // usually just gives the class constructor one argument
        if (base != null) theme.applyStyle(base!!, true)
        if (light != 0 && theme.equals(ThemePreference.LIGHT)) theme.applyStyle(light!!, true)
        else if (theme.equals(ThemePreference.DARK)) {
            if (dark != null && !trueBlackTheme) theme.applyStyle(dark!!, true)
            else if (black != 0 && trueBlackTheme) theme.applyStyle(dark!!, true)
        } else if (theme.equals(ThemePreference.AUTO)) {
            if (autoDark != null && !trueBlackTheme) theme.applyStyle(autoDark!!, true)
            else if (autoBlack != null && trueBlackTheme) theme.applyStyle(autoBlack!!, true)
        }
    }
}