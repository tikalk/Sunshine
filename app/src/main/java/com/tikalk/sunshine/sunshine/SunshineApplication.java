package com.tikalk.sunshine.sunshine;

import android.app.Application;

import com.tikalk.sunshine.utils.log.CrashReportingTree;

import timber.log.Timber;

/**
 * Created by oren on 03/02/16.
 */
public class SunshineApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());

        }
        Timber.tag(SunshineApplication.class.toString());
        Timber.i("onCreate");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Timber.i("onTerminate");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Timber.w("onLowMemory");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Timber.w("onTrimMemory level : %i",level);
    }
}
