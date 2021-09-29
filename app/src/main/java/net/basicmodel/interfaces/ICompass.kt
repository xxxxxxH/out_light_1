package net.basicmodel.interfaces

import android.graphics.Bitmap

interface ICompass {
    fun turnOnFlash()
    fun turnOffFlash()
    fun updateImageForGalleryView(bitmap: Bitmap?)
}