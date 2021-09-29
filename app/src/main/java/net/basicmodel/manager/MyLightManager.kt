package net.basicmodel.manager

import kotlin.jvm.Volatile
import net.basicmodel.interfaces.IAction
import net.basicmodel.utils.ToastUtils
import net.basicmodel.app.App
import net.basicmodel.calculateSecond
import java.lang.RuntimeException

class MyLightManager private constructor(camera: MyCameraManager?) : Runnable {
    @JvmField
    @Volatile
    var needTurnOffLight = false

    @Volatile
    private var iAction: IAction? = null
    private var myCameraManager: MyCameraManager? = null

    @JvmField
    @Volatile
    var delayTime = 0

    @JvmField
    @Volatile
    var needIntercept = false

    fun bindAction(iAction: IAction?) {
        this.iAction = iAction
    }

    override fun run() {
        needIntercept = false
        needTurnOffLight = false
        while (!needIntercept) {
            try {
                val delay = calculateSecond(delayTime)
                iAction?.let {
                    myCameraManager?.openStrobeFlash(it)
                }
                Thread.sleep(delay.toLong())
                iAction?.let {
                    myCameraManager?.turnOffStrobeFlash(it)
                }
                Thread.sleep(delay.toLong())
            } catch (ex: InterruptedException) {
                ex.printStackTrace()
            } catch (ex: RuntimeException) {
                needIntercept = true
                ex.printStackTrace()
            }
        }
        if (needTurnOffLight && App.isFlashOpen) {
            iAction?.let {
                myCameraManager?.turnOffFlash(it)
            }
            needTurnOffLight = false
        }
        needIntercept = false
        needTurnOffLight = false
    }

    companion object {
        private var instance: MyLightManager? = null

        @JvmStatic
        fun getInstance(camera: MyCameraManager?): MyLightManager {
            return if (instance == null) MyLightManager(camera).also { instance = it } else instance!!
        }
    }

    init {
        if (camera != null) {
            myCameraManager = camera
        } else {
            myCameraManager = MyCameraManager.instance
        }
    }
}