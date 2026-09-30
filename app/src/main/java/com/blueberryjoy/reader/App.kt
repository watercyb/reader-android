package com.blueberryjoy.reader

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate

class App : Application() {
    companion object {
        private lateinit var instance: App

        @SuppressLint("AppBundleLocaleChanges")
        fun getAppContext(): Context {
            val context = instance.applicationContext
            val locales = AppCompatDelegate.getApplicationLocales()
            val config = Configuration(context.resources.configuration)
            config.setLocales(locales.unwrap() as LocaleList?)
            return context.createConfigurationContext(config)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }
}
