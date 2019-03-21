package com.github.herokotlin.voiceinput

import android.app.Activity
import android.support.v4.app.ActivityCompat

interface VoiceInputCallback {

    // 预览发生变化
    fun onPreviewingChange(isPreviewing: Boolean) {

    }

    fun onRecordButtonClick() {

    }

    fun onPlayButtonClick() {

    }

    // 录音结束或点击发送时触发
    fun onFinishRecord(audioPath: String, audioDuration: Int) {

    }

    // 录音时间太短
    fun onRecordDurationLessThanMinDuration() {

    }

    fun onRequestPermissions(activity: Activity, permissions: Array<out String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    fun onPermissionsGranted() {

    }

    fun onPermissionsDenied() {

    }

    fun onPermissionsNotGranted() {

    }

    fun onExternalStorageNotWritable() {

    }

}