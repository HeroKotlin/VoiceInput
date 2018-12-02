package com.github.herokotlin.voiceinput

import android.content.Context

abstract class VoiceInputConfiguration(context: Context) {

    /**
     * 保存录音文件的目录
     */
    var fileDir = context.externalCacheDir.absolutePath

    /**
     * 文件扩展名
     */
    var fileExtname = ".m4a"

    /**
     * 请求权限
     */
    abstract fun requestPermissions(permissions: List<String>, requestCode: Int): Boolean

}