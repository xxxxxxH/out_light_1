package net.basicmodel.app

import android.app.Application
import android.preference.PreferenceManager
import com.effective.android.anchors.task.Task
import com.effective.android.anchors.task.TaskCreator
import net.basicmodel.Ktx
import net.basicmodel.R
import net.basicmodel.constants.Constants
import net.basicmodel.manager.MySoundManager
import java.io.File

class MyTaskCreator(private val application: Application) : TaskCreator {

    companion object {
        const val TASK_PREFERENCE = "TASK_PREFERENCE"
        const val TASK_SOUND = "TASK_SOUND"
        const val TASK_KTX = "TASK_KTX"
        const val TASK_EMPTY = "TASK_EMPTY"
    }

    override fun createTask(taskName: String) =
        when (taskName) {
            TASK_PREFERENCE -> TaskPreference(application)
            TASK_SOUND -> TaskSound(application)
            TASK_KTX ->TaskKtx(application)
            else -> TaskEmpty()
        }

}

abstract class MyTaskFactory(id: String, isAsyncTask: Boolean = false) : Task(id, isAsyncTask)

class TaskPreference(private val application: Application) : MyTaskFactory(MyTaskCreator.TASK_PREFERENCE) {
    override fun run(name: String) {
        val file = File(
            "/data/data/"
                    + application.packageName + "/shared_prefs/" + Constants.FLASH_LIGHT_NAME
        )
        if (!file.exists()) {
            PreferenceManager.setDefaultValues(
                application,
                Constants.FLASH_LIGHT_NAME, Application.MODE_PRIVATE, R.xml.settings, false
            )
        }
    }
}

class TaskSound(private val application: Application) : MyTaskFactory(MyTaskCreator.TASK_SOUND) {
    override fun run(name: String) {
        MySoundManager.create(application)
    }
}

class TaskKtx(private val application: Application) : MyTaskFactory(MyTaskCreator.TASK_KTX) {
    override fun run(name: String) {
        Ktx.initialize(application)
    }
}

class TaskEmpty : MyTaskFactory(MyTaskCreator.TASK_EMPTY) {
    override fun run(name: String) {
    }
}
