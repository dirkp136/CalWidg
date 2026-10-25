package de.dirk.kalender

import android.app.Application
import de.dirk.kalender.data.Scheduler

class KalenderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Scheduler.scheduleAll(this)
    }
}
