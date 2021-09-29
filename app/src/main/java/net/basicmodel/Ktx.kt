package net.basicmodel

import android.app.Application

class Ktx private constructor(application: Application) {

    companion object {
        @Volatile
        private var INSTANCE: Ktx? = null

        fun initialize(application: Application) {
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Ktx(application).let {
                    INSTANCE = it
                }
            }
        }

        fun getInstance() =
            INSTANCE ?: throw NullPointerException("Have you invoke initialize() before?")
    }

    val app = application
}