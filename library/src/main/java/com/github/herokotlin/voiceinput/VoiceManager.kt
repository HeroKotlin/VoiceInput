package com.github.herokotlin.voiceinput

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class VoiceManager(private val context: Context) {

    companion object {
        const val LOG_TAG = "VoiceInput"
        const val SECOND = 1000
        const val PERMISSION_REQUEST_CODE = 12134
    }

    /**
     * 是否正在录音
     */
    var isRecording = false

    /**
     * 是否正在播放录音
     */
    var isPlaying = false

    /**
     * 文件扩展名
     */
    var audioExtname = ".m4a"

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
     * 保存录音文件的目录
     */
    var fileDir = context.externalCacheDir.absolutePath

    /**
     * 当前正在录音的文件路径
     */
    var filePath = ""

    /**
     * 录音文件的时长
     */
    var fileDuration = 0

    /**
     * 支持的最短录音时长
     */
    var minDuration = SECOND

    /**
     * 支持的最长录音时长
     */
    var maxDuration = 60 * SECOND

    // 外部实时读取的录音时长
    var duration: Long = 0

        get() {
            val now = System.currentTimeMillis()
            var duration = now - recordStartTime
            if (duration > maxDuration) {
                duration = maxDuration.toLong()
                stopRecord()
            }
            return duration
        }


    // 外部实时读取的播放进度
    var progress: Long = 0

        get() {
            return if (player != null) player!!.currentPosition.toLong() else 0
        }

    var onPermissionsGranted: (() -> Unit)? = null

    var onPermissionsDenied: (() -> Unit)? = null

    var onRecordWithoutPermissions: (() -> Unit)? = null

    var onRecordWithoutExternalStorage: (() -> Unit)? = null

    var onRecordDurationLessThanMinDuration: (() -> Unit)? = null

    var onFinishRecord: ((success: Boolean) -> Unit)? = null

    var onFinishPlay: ((success: Boolean) -> Unit)? = null

    private var recorder: MediaRecorder? = null

    private var player: MediaPlayer? = null

    private var recordStartTime: Long = 0


    /**
     * 获取单项权限的结果
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 判断是否有权限录音，如没有，发起授权请求
     */
    fun requestPermissions(): Boolean {

        var permissions = arrayOf<String>()

        if (!hasPermission(android.Manifest.permission.RECORD_AUDIO)) {
            permissions = permissions.plus(android.Manifest.permission.RECORD_AUDIO)
        }

        if (!hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permissions = permissions.plus(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                context as Activity,
                permissions,
                PERMISSION_REQUEST_CODE
            )
            return false
        }

        return true

    }

    /**
     * 如果触发了用户授权，则必须在 Activity 级别实现 onRequestPermissionsResult 接口，并调此方法完成授权
     */
    fun requestPermissionsResult(requestCode: Int, grantResults: IntArray) {

        if (requestCode != PERMISSION_REQUEST_CODE) {
            return
        }

        var hasPermission = false

        val count = grantResults.count()
        if (count == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                hasPermission = true
            }
        }
        else if (count == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                || grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                hasPermission = true
            }
        }

        if (hasPermission) {
            onPermissionsGranted?.invoke()
        }
        else {
            onPermissionsDenied?.invoke()
        }

    }

    /**
     * 检查外部存储是否可用，如不可用，无法录音
     */
    private fun checkExternalStorageAvailable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED
    }

    fun startRecord() {

        if (!requestPermissions()) {
            onRecordWithoutPermissions?.invoke()
            return
        }

        if (!checkExternalStorageAvailable()) {
            onRecordWithoutExternalStorage?.invoke()
            return
        }

        filePath = getFilePath(fileDir, audioExtname)

        fileDuration = 0

        val recorder = MediaRecorder()

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)

        recorder.setOutputFormat(audioFormat)
        recorder.setAudioEncoder(audioEncoder)
        recorder.setAudioChannels(numberOfChannels)
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
                onRecordWithoutPermissions?.invoke()
            }
        }

        if (isSuccess && fileDuration < minDuration) {
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