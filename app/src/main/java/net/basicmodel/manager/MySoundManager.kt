package net.basicmodel.manager

import android.content.Context
import android.media.SoundPool
import net.basicmodel.manager.MySoundManager
import android.media.AudioManager
import android.os.Build
import android.media.AudioAttributes
import net.basicmodel.R

object MySoundManager {
    private var soundPool: SoundPool? = null
    private var isComplete = false
    private var clickSound = 0
    private var moveSound = 0
    private var warningSound = 0
    private const val MAX_VOLUME = 1
    private const val MAX_STREAMS = 5

    @JvmStatic
    fun clickSound() {
        if (isComplete) {
            soundPool?.play(clickSound, MAX_VOLUME.toFloat(), MAX_VOLUME.toFloat(), 1, 0, 1f)
        }
    }

    @JvmStatic
    fun moveSound() {
        if (isComplete) {
            soundPool?.play(moveSound, MAX_VOLUME.toFloat(), MAX_VOLUME.toFloat(), 1, 0, 1f)
        }
    }

    @JvmStatic
    fun warningSound() {
        if (isComplete) {
            soundPool?.play(warningSound, MAX_VOLUME.toFloat(), MAX_VOLUME.toFloat(), 1, 0, 1f)
        }
    }

    fun create(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_ALARM,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.ADJUST_UNMUTE
                )
            }
            val audioAttrib = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val builder = SoundPool.Builder()
            builder.setAudioAttributes(audioAttrib).setMaxStreams(MAX_STREAMS)
            soundPool = builder.build()
        } else {
            soundPool = SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0)
        }
        soundPool?.setOnLoadCompleteListener { soundPool, sampleId, status -> isComplete = true }
        clickSound = soundPool?.load(context, R.raw.sound_toggle, 1) ?: 0
        moveSound = soundPool?.load(context, R.raw.adjustment_move, 1) ?: 0
        warningSound = soundPool?.load(context, R.raw.double_bip, 1) ?: 0
    }
}