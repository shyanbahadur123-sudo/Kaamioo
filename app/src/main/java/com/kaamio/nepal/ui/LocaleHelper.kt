package com.kaamio.nepal.ui

import android.content.Context
import android.content.ContextWrapper
import java.util.Locale

class LocaleHelper(base: Context) : ContextWrapper(base) {
    companion object {
        fun wrap(context: Context, language: String): ContextWrapper {
            val locale = Locale(language)
            Locale.setDefault(locale)
            val config = context.resources.configuration
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            return LocaleHelper(context.createConfigurationContext(config))
        }
    }
}
