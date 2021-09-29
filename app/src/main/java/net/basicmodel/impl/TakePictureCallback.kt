package net.basicmodel.impl

import net.basicmodel.interfaces.ICompass
import android.hardware.Camera.PictureCallback
import androidx.appcompat.widget.AppCompatImageView
import net.basicmodel.R
import android.os.AsyncTask
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.os.Environment
import net.basicmodel.addImageToGallery
import net.basicmodel.bitmapRotate
import net.basicmodel.scaleBitmap
import net.basicmodel.ui.activity.CompassActivity
import net.basicmodel.ui.dialog.ProgressDialog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class TakePictureCallback(
    private val compassActivity: CompassActivity,
    private var myCamera: Camera,
    private var iCompass: ICompass
) : PictureCallback {
    private var flashView: AppCompatImageView? = null

    private var mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK
    private var mRotation = 0
    fun setRotation(mRotation: Int) {
        this.mRotation = mRotation
    }

    fun setMyCamera(myCamera: Camera) {
        this.myCamera = myCamera
    }

    fun setCameraId(mCameraId: Int) {
        this.mCameraId = mCameraId
    }

    fun setGlassViewInterface(iCompass: ICompass) {
        this.iCompass = iCompass
    }

    fun setView(btnFlash: AppCompatImageView?) {
        flashView = btnFlash
    }

    override fun onPictureTaken(data: ByteArray, camera: Camera) {
        SaveImageTask().execute(data)
    }

    private val outputMediaFile: File?
        get() {
            val mediaStorageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                compassActivity.getString(R.string.folder)
            )
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null
                }
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            return File(
                mediaStorageDir.path + File.separator
                        + "IMG_" + timeStamp + ".jpg"
            )
        }

    private inner class SaveImageTask : AsyncTask<ByteArray?, Void?, Bitmap?>() {
        var dialog: ProgressDialog? = null
        var pictureFile: File? = null

        override fun onPreExecute() {
            super.onPreExecute()
            dialog = ProgressDialog(compassActivity)
            dialog!!.setTopColorRes(R.color.darkBlueGrey)
            dialog!!.setIcon(R.drawable.ic_progress)
            dialog!!.setTitle(compassActivity.getString(R.string.msg_loading))
            dialog!!.setCancelable(false)
            dialog!!.show()
        }

        override fun onPostExecute(bitmap: Bitmap?) {
            super.onPostExecute(bitmap)
            if (dialog != null) {
                dialog!!.dismiss()
            }
            try {
                myCamera.startPreview()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            iCompass.turnOffFlash()
            flashView!!.isSelected = false
            compassActivity.isFlashButtonSelected =false
            if (bitmap != null) {
                addImageToGallery(pictureFile!!.path, compassActivity)
                iCompass.updateImageForGalleryView(scaleBitmap(bitmap, 200, 200))
            }
        }

        override fun doInBackground(vararg params: ByteArray?): Bitmap? {
            pictureFile = outputMediaFile
            if (pictureFile == null) {
                return null
            }
            try {
                val fos = FileOutputStream(pictureFile)
                var realImage = BitmapFactory.decodeByteArray(params[0], 0, params[0]?.size ?: 0)
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    if (mRotation == 90) {
                        realImage = bitmapRotate(realImage, 90)
                    } else if (mRotation == 270) {
                        realImage = bitmapRotate(realImage, 270)
                    }
                } else {
                    realImage = bitmapRotate(realImage, 270)
                }
                realImage.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.close()
                return realImage
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }
    }

}