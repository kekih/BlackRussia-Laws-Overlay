package com.blackrussia.laws

import android.content.Context
import android.content.SharedPreferences

object AppPrefs {
    private const val PREFS = "black_russia_prefs"
    private const val KEY_THEME = "theme"

    const val THEME_DARK = "dark"
    const val THEME_LIGHT = "light"
    const val THEME_ORANGE = "orange"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getTheme(ctx: Context): String =
        prefs(ctx).getString(KEY_THEME, THEME_DARK) ?: THEME_DARK

    fun setTheme(ctx: Context, theme: String) {
        prefs(ctx).edit().putString(KEY_THEME, theme).apply()
    }
}
