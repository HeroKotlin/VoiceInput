package com.github.herokotlin.voiceinput

import android.content.Context
import android.media.MediaRecorder

abstract class VoiceInputConfiguration(val context: Context) {

    /**
     * 保存录音文件的目录
     */
    var fileDir = context.externalCacheDir.absolutePath

    /**
     * 文件扩展名
     */
    var fileExtname = ".m4a"

    /**
     * 音频格式
     */
    var audioFormat = MediaRecorder.OutputFormat.MPEG_4

    /**
     * 音频编码器
     */
    var audioEncoder = MediaRecorder.AudioEncoder.HE_AAC

    /**
     * 双声道还是单声道
     */
    var numberOfChannels = 2

    /**
     * 码率
     */
    var audioBitRate = 320000

    /**
     * 采样率
     */
    var audioSampleRate = 44100

    /**
     * 支持的最短录音时长
     */
    var audioMinDuration = 1000

    /**
     * 支持的最长录音时长
     */
    var audioMaxDuration = 60 * 1000

    /**
     * 请求权限
     */
    abstract fun requestPermissions(permissions: List<String>, requestCode: Int): Boolean

}