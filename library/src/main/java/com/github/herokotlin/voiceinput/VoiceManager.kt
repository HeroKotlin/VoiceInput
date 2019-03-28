package com.github.herokotlin.voiceinput

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import com.github.herokotlin.permission.Permission
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

internal class VoiceManager {

    companion object {
        const val LOG_TAG = "VoiceInput"
    }

    /**
     * 保存录音文件的目录
     */
    var fileDir = ""

    /**
     * 文件扩展名
     */
    var fileExtname = ""

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
    var audioNumberOfChannels = 2

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
     * 是否正在录音
     */
    var isRecording = false

    /**
     * 是否正在播放录音
     */
    var isPlaying = false

    /**
     * 当前正在录音的文件路径
     */
    var filePath = ""

    /**
     * 录音文件的时长
     */
    var fileDuration = 0

    // 外部实时读取的录音时长
    val duration: Long

        get() {
            val now = System.currentTimeMillis()
            var duration = now - recordStartTime
            if (duration > audioMaxDuration) {
                duration = audioMaxDuration.toLong()
                stopRecord()
            }
            return duration
        }

    // 外部实时读取的播放进度
    val progress: Long

        get() {
            return if (player != null) player!!.currentPosition.toLong() else 0
        }

    val permission = Permission(190905, listOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    ))

    var onRecordDurationLessThanMinDuration: (() -> Unit)? = null

    var onFinishRecord: ((success: Boolean) -> Unit)? = null

    var onFinishPlay: ((success: Boolean) -> Unit)? = null

    private var recorder: MediaRecorder? = null

    private var player: MediaPlayer? = null

    private var recordStartTime: Long = 0

    fun startRecord() {

        if (!permission.checkExternalStorageWritable()) {
            return
        }

        filePath = getFilePath(fileDir, fileExtname)

        fileDuration = 0

        val recorder = MediaRecorder()

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)

        recorder.setOutputFormat(audioFormat)
        recorder.setAudioEncoder(audioEncoder)
        recorder.setAudioChannels(audioNumberOfChannels)
        recorder.setAudioEncodingBitRate(audioBitRate)
        recorder.setAudioSamplingRate(audioSampleRate)

        recorder.setOutputFile(filePath)

        var isSuccess = false

        try {
            recorder.prepare()
            recorder.start()
            isSuccess = true
        }
        catch (e: IOException) {
            Log.d(LOG_TAG, "IOException starting MediaRecorder: ${e.message}")
        }
        catch (e: IllegalStateException) {
            Log.d(LOG_TAG, "RuntimeException starting MediaRecorder: ${e.message}")
        }

        if (isSuccess) {
            isRecording = true
            recordStartTime = System.currentTimeMillis()
            this.recorder = recorder
        }
        else {
            recorder.reset()
            recorder.release()
        }

    }

    fun stopRecord() {

        val recorder = this.recorder

        if (!isRecording || recorder == null) {
            return
        }

        var isSuccess = false

        try {
            recorder.stop()
            isSuccess = true
        }
        catch (e: RuntimeException) {
            // if no valid audio/video data has been received when stop() is called
            // 比如点击完开始，立即点结束
            Log.d(LOG_TAG, "RuntimeException stoping MediaRecorder: ${e.message}")
        }

        recorder.reset()
        recorder.release()

        // recorder 无法读取到真实时长
        // 因此接下来必须借助 MediaPlayer
        if (isSuccess) {
            val player = MediaPlayer()
            try {
                player.setDataSource(filePath)
                player.prepare()

                fileDuration = player.duration

                player.reset()
                player.release()

                isSuccess = true
            }
            catch (e: Exception) {
                isSuccess = false
                permission.onPermissionsNotGranted?.invoke()
            }
        }

        if (isSuccess && fileDuration < audioMinDuration) {
            onRecordDurationLessThanMinDuration?.invoke()
            isSuccess = false
        }

        if (!isSuccess) {
            deleteFile()
        }

        this.recorder = null

        isRecording = false

        onFinishRecord?.invoke(isSuccess)

    }

    fun startPlay() {

        var isSuccess = false

        val player = MediaPlayer()

        player.setOnCompletionListener {
            stopPlay()
        }

        try {

            player.setDataSource(filePath)
            player.prepare()
            player.start()

            isSuccess = true

        }
        catch (e: IOException) {
            Log.d(LOG_TAG, "IOException starting MediaPlayer: ${e.message}")
        }

        if (isSuccess) {
            isPlaying = true
            this.player = player
        }
        else {
            player.reset()
            player.release()
        }

    }

    fun stopPlay() {

        val player = this.player

        if (!isPlaying || player == null) {
            return
        }

        player.stop()
        player.reset()
        player.release()

        this.player = null

        isPlaying = false

        onFinishPlay?.invoke(true)

    }

    fun deleteFile() {

        if (filePath.isBlank()) {
            return
        }

        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }

        filePath = ""

    }

    fun getFilePath(dirname: String, extname: String): String {

        // 确保目录存在
        val file = File(dirname)
        if (!file.exists()) {
            file.mkdir()
        }

        // 时间格式的文件名
        val formater = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)

        val filename = "${formater.format(Date())}$extname"

        if (dirname.endsWith("/")) {
            return dirname + filename
        }

        return "$dirname/$filename"

    }

}