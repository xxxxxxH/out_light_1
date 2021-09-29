package net.basicmodel.app

import android.app.Application
import net.basicmodel.manager.MySoundManager
import android.preference.PreferenceManager
import com.effective.android.anchors.*
import com.effective.android.anchors.task.project.Project
import net.basicmodel.BuildConfig
import net.basicmodel.Ktx
import net.basicmodel.R
import net.basicmodel.app.App
import net.basicmodel.constants.Constants
import java.io.File

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        AnchorsManager.getInstance()
            .debuggable {
                BuildConfig.DEBUG
            }
            .anchors {
                arrayOf(MyTaskCreator.TASK_PREFERENCE)
            }
            .taskFactory {
                Project.TaskFactory(MyTaskCreator(this))
            }
            .graphics {
                arrayOf(
                    MyTaskCreator.TASK_KTX.sons(MyTaskCreator.TASK_SOUND.sons(MyTaskCreator.TASK_PREFERENCE))
                )
            }
            .startUp()
    }

    companion object {
        @JvmField
        var isFlashOpen = false
    }
}