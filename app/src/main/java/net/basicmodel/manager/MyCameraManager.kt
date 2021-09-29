package net.basicmodel.manager

import android.hardware.Camera
import android.os.Handler
import net.basicmodel.manager.MyCameraManager
import net.basicmodel.app.App
import kotlin.jvm.Synchronized
import net.basicmodel.interfaces.IAction
import java.lang.Exception
import java.lang.RuntimeException

class MyCameraManager private constructor() {
    private var params: Camera.Parameters? = null
    private var isSupport = false
    private val handler = Handler()
    private var isAlreadyStartPreview = false

    fun bindCamera() {
        try {
            params = Companion.camera!!.parameters
            val modes = params?.supportedFlashModes
            if (modes != null) {
                val currentMode = params?.flashMode
                if (Camera.Parameters.FLASH_MODE_TORCH != currentMode) {
                    isSupport = modes.contains(Camera.Parameters.FLASH_MODE_TORCH)
                }
            }
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
    }

    fun resetCamera() {
        handler.post {
            if (Companion.camera != null) {
                Companion.camera!!.stopPreview()
                Companion.camera!!.setPreviewCallback(null)
                Companion.camera!!.release()
                Companion.camera = null
                App.isFlashOpen = false
                setAlreadyStartPreview(false)
            }
        }
    }

    @Synchronized
    fun openFlashLight(indicator: IAction) {
        if (!App.isFlashOpen) {
            try {
                App.isFlashOpen = true
                indicator.actionOn()
                Thread {
                    if (isSupport) {
                        params!!.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                        Companion.camera!!.parameters = params
                    } else {
                        params!!.flashMode = Camera.Parameters.FLASH_MODE_ON
                        Companion.camera!!.parameters = params
                    }
                    Companion.camera!!.startPreview()
                }.start()
            } catch (ex: RuntimeException) {
                ex.printStackTrace()
            } catch (e: Exception) {
                indicator.actionFail()
            }
        }
    }

    fun openStrobeFlash(indicator: IAction) {
        if (!App.isFlashOpen) {
            try {
                App.isFlashOpen = true
                indicator.actionOn()
                if (isSupport) {
                    params!!.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                    Companion.camera!!.parameters = params
                } else {
                    params!!.flashMode = Camera.Parameters.FLASH_MODE_ON
                    Companion.camera!!.parameters = params
                }
                if (!isAlreadyStartPreview) {
                    Companion.camera!!.startPreview()
                    setAlreadyStartPreview(true)
                }
            } catch (e: Exception) {
                indicator.actionFail()
            }
        }
    }

    @Synchronized
    fun turnOffFlash(indicator: IAction) {
        if (App.isFlashOpen) {
            try {
                App.isFlashOpen = false
                indicator.actionOff()
                Thread {
                    if (Companion.camera != null) {
                        params = Companion.camera!!.parameters
                        params?.flashMode = Camera.Parameters.FLASH_MODE_OFF
                        Companion.camera!!.parameters = params
                        Companion.camera!!.stopPreview()
                    }
                }.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        setAlreadyStartPreview(false)
    }

    fun turnOffStrobeFlash(indicator: IAction) {
        if (App.isFlashOpen) {
            try {
                App.isFlashOpen = false
                indicator.actionOff()
                params = Companion.camera!!.parameters
                params?.flashMode = Camera.Parameters.FLASH_MODE_OFF
                Companion.camera!!.parameters = params
                if (!isAlreadyStartPreview) {
                    Companion.camera!!.stopPreview()
                    setAlreadyStartPreview(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val camera: Camera?
        get() = Companion.camera

    private fun setAlreadyStartPreview(alreadyStartPreview: Boolean) {
        isAlreadyStartPreview = alreadyStartPreview
    }

    companion object {
        private var camera: Camera? = null
        private var myCameraManager: MyCameraManager? = null

        @JvmStatic
        val instance: MyCameraManager?
            get() {
                if (myCameraManager == null) {
                    myCameraManager = MyCameraManager()
                }
                if (camera == null) {
                    try {
                        camera = Camera.open()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return myCameraManager
            }
    }
}