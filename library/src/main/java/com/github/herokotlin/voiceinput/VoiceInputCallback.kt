package com.github.herokotlin.voiceinput

interface VoiceInputCallback {

    // 点击录音按钮时，发现没权限
    fun onRecordWithoutPermissions() {

    }

    // 录音结束或点击发送时触发
    fun onFinishRecord(audioPath: String, audioDuration: Int) {

    }

    // 录音时间太短
    fun onRecordDurationLessThanMinDuration() {

    }

    // 没有外部存储可用
    fun onRecordWithoutExternalStorage() {

    }

    // 用户点击同意授权
    fun onPermissionsGranted() {

    }

    // 用户点击拒绝授权
    fun onPermissionsDenied() {

    }

}