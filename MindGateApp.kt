package com.mindgate.app

import android.app.Application
import com.mindgate.app.data.datastore.MindGateDataStore

class MindGateApp : Application() {
    lateinit var dataStore: MindGateDataStore
        private set

    override fun onCreate() {
        super.onCreate()
        dataStore = MindGateDataStore(this)
    }
}
