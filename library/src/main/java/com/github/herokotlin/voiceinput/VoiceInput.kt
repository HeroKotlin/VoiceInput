package com.github.herokotlin.voiceinput

import android.content.Context
import android.os.Handler
import android.os.Message
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.github.herokotlin.circleview.CircleView
import com.github.herokotlin.circleview.CircleViewCallback
import kotlinx.android.synthetic.main.voice_input.view.*
import java.lang.ref.WeakReference

class VoiceInput : FrameLayout {

    companion object {
        private const val MESSAGE_TIME_UPDATE = 12134
    }

    var callback: VoiceInputCallback? = null

    private var voiceManager = VoiceManager(context)

    private var isPreviewButtonPressed = false

        set(value) {
            if (field == value) {
                return
            }
            field = value

            if (value) {
                previewButton.centerColor = ContextCompat.getColor(context, R.color.voice_input_preview_button_bg_color_hover)
                guideLabel.visibility = View.VISIBLE
                durationLabel.visibility = View.GONE
                guideLabel.text = resources.getString(R.string.voice_input_guide_preview)
            }
            else {
                previewButton.centerColor = ContextCompat.getColor(context, R.color.voice_input_preview_button_bg_color_normal)
                guideLabel.visibility = View.GONE
                durationLabel.visibility = View.VISIBLE
                guideLabel.text = resources.getString(R.string.voice_input_guide_normal)
            }
            previewButton.invalidate()
        }

    private var isDeleteButtonPressed = false

        set(value) {
            if (field == value) {
                return
            }
            field = value

            if (value) {
                deleteButton.centerColor = ContextCompat.getColor(context, R.color.voice_input_delete_button_bg_color_hover)
                guideLabel.visibility = View.VISIBLE
                durationLabel.visibility = View.GONE
                guideLabel.text = resources.getString(R.string.voice_input_guide_delete)
            }
            else {
                deleteButton.centerColor = ContextCompat.getColor(context, R.color.voice_input_delete_button_bg_color_normal)
                guideLabel.visibility = View.VISIBLE
                durationLabel.visibility = View.GONE
                guideLabel.text = resources.getString(R.string.voice_input_guide_normal)
            }
            previewButton.invalidate()
        }

    /**
     * 是否在预览界面
     */
    private var isPreviewing = false

        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (value) {
                resetPreviewView()
                recordView.visibility = View.GONE
                previewView.visibility = View.VISIBLE
            }
            else {
                recordView.visibility = View.VISIBLE
                previewView.visibility = View.GONE
            }
        }

    // 参考 https://blog.csdn.net/qq_38355313/article/details/79082837
    private class DurationUpdateHandler(voiceInput: VoiceInput) : Handler() {

        private val instance: WeakReference<VoiceInput> = WeakReference(voiceInput)

        override fun handleMessage(msg: Message?) {
            val voiceInput = instance.get()
            if (voiceInput != null && msg?.what == MESSAGE_TIME_UPDATE) {
                voiceInput.onDurationUpdate()
            }
        }
    }

    private class ProgressUpdateHandler(voiceInput: VoiceInput) : Handler() {

        private val instance: WeakReference<VoiceInput> = WeakReference(voiceInput)

        override fun handleMessage(msg: Message?) {
            val voiceInput = instance.get()
            if (voiceInput != null && msg?.what == MESSAGE_TIME_UPDATE) {
                voiceInput.onProgressUpdate()
            }
        }
    }

    private class TimeThread(voiceInput: VoiceInput, private val interval: Long) : Thread() {
        private val instance: WeakReference<VoiceInput> = WeakReference(voiceInput)
        var running = true
        override fun run() {
            do {
                if (running) {
                    val voiceInput = instance.get()
                    if (voiceInput != null) {
                        Thread.sleep(interval)
                        voiceInput.timerHandler?.sendEmptyMessage(MESSAGE_TIME_UPDATE)
                    }
                }
                else {
                    break
                }
            }
            while (true)
        }
    }

    private var timerHandler: Handler? = null

    private var timer: TimeThread? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    private fun init() {

        LayoutInflater.from(context).inflate(R.layout.voice_input, this)

        val recordButtonRadius = resources.getDimensionPixelSize(R.dimen.voice_input_record_button_radius)

        val previewButtonMarginRight = resources.getDimensionPixelSize(R.dimen.voice_input_preview_button_margin_right)
        val previewButtonBorderWidth = resources.getDimensionPixelSize(R.dimen.voice_input_preview_button_border_width)
        val previewButtonRadius = resources.getDimensionPixelSize(R.dimen.voice_input_preview_button_radius)

        val deleteButtonMarginLeft = resources.getDimensionPixelSize(R.dimen.voice_input_delete_button_margin_left)
        val deleteButtonBorderWidth = resources.getDimensionPixelSize(R.dimen.voice_input_delete_button_border_width)
        val deleteButtonRadius = resources.getDimensionPixelSize(R.dimen.voice_input_delete_button_radius)

        recordButton.callback = object: CircleViewCallback {

            override fun onTouchDown(circleView: CircleView) {
                startRecord()
            }

            override fun onTouchUp(circleView: CircleView, inside: Boolean, isLongPress: Boolean) {
                if (voiceManager.isRecording) {
                    stopRecord()
                }
            }

            override fun onTouchMove(circleView: CircleView, x: Float, y: Float) {

                val offsetY = y - recordButtonRadius

                var centerX = -1 * (previewButtonMarginRight + previewButtonRadius + previewButtonBorderWidth)
                var offsetX = (x - centerX).toDouble()

                isPreviewButtonPressed = Math.sqrt(offsetX * offsetX + offsetY * offsetY) <= previewButtonRadius
                if (isPreviewButtonPressed) {
                    return
                }

                centerX = 2 * recordButtonRadius + deleteButtonMarginLeft + deleteButtonRadius + deleteButtonBorderWidth
                offsetX = (x - centerX).toDouble()

                isDeleteButtonPressed = Math.sqrt(offsetX * offsetX + offsetY * offsetY) <= deleteButtonRadius

            }
        }

        playButton.callback = object: CircleViewCallback {

            override fun onTouchDown(circleView: CircleView) {
                circleView.centerColor = ContextCompat.getColor(context, R.color.voice_input_play_button_center_color_pressed)
                circleView.invalidate()
            }

            override fun onTouchUp(circleView: CircleView, inside: Boolean, isLongPress: Boolean) {
                circleView.centerColor = ContextCompat.getColor(context, R.color.voice_input_play_button_center_color_normal)
                circleView.invalidate()

                if (inside) {
                    if (voiceManager.isPlaying) {
                        stopPlay()
                    }
                    else {
                        startPlay()
                    }
                }
            }

            override fun onTouchEnter(circleView: CircleView) {
                circleView.centerColor = ContextCompat.getColor(context, R.color.voice_input_play_button_center_color_pressed)
                circleView.invalidate()
            }

            override fun onTouchLeave(circleView: CircleView) {
                circleView.centerColor = ContextCompat.getColor(context, R.color.voice_input_play_button_center_color_normal)
                circleView.invalidate()
            }
        }

        cancelButton.setOnClickListener {
            cancel()
        }

        sendButton.setOnClickListener {
            send()
        }

        voiceManager.onPermissionsGranted = {
            callback?.onPermissionsGranted()
        }
        voiceManager.onPermissionsDenied = {
            callback?.onPermissionsDenied()
        }
        voiceManager.onRecordWithoutPermissions = {
            callback?.onRecordWithoutPermissions()
        }
        voiceManager.onRecordWithoutExternalStorage = {
            callback?.onRecordWithoutExternalStorage()
        }
        voiceManager.onRecordDurationLessThanMinDuration = {
            callback?.onRecordDurationLessThanMinDuration()
        }
        voiceManager.onFinishRecord = {
            finishRecord()
        }
        voiceManager.onFinishPlay = {
            finishPlay()
        }

    }

    private fun startTimer(interval: Long, handler: Handler) {
        timer = TimeThread(this, interval)
        timerHandler = handler
        timer?.start()
    }

    private fun stopTimer() {
        timer?.running = false
        timer?.join()
        timer = null
        timerHandler = null
    }

    private fun startRecord() {

        voiceManager.startRecord()

        if (voiceManager.isRecording) {

            recordButton.centerColor = ContextCompat.getColor(context, R.color.voice_input_record_button_bg_color_pressed)
            recordButton.invalidate()

            previewButton.visibility = View.VISIBLE
            deleteButton.visibility = View.VISIBLE

            guideLabel.visibility = View.GONE
            durationLabel.visibility = View.VISIBLE
            durationLabel.text = formatDuration(0)

            startTimer(100, DurationUpdateHandler(this))

        }

    }

    private fun stopRecord() {

        voiceManager.stopRecord()

    }

    private fun finishRecord() {

        stopTimer()

        if (voiceManager.filePath.isNotBlank()) {
            if (isPreviewButtonPressed) {
                isPreviewing = true
            }
            else if (isDeleteButtonPressed) {
                voiceManager.deleteFile()
            }
            else {
                callback?.onFinishRecord(voiceManager.filePath, voiceManager.fileDuration)
            }
        }

        isPreviewButtonPressed = false
        isDeleteButtonPressed = false

        recordButton.centerColor = ContextCompat.getColor(context, R.color.voice_input_record_button_bg_color_normal)
        recordButton.invalidate()

        previewButton.visibility = View.GONE
        deleteButton.visibility = View.GONE

        guideLabel.visibility = View.VISIBLE
        durationLabel.visibility = View.GONE

    }

    private fun startPlay() {

        voiceManager.startPlay()

        if (voiceManager.isPlaying) {
            playButton.centerImage = R.drawable.voice_input_stop
            playButton.invalidate()
            // interval 设小一点才能看到进度条走完
            // 否则就是还剩一段就结束了
            startTimer(1000 / 200, ProgressUpdateHandler(this))
        }
    }

    private fun stopPlay() {

        voiceManager.stopPlay()

    }

    private fun finishPlay() {

        stopTimer()

        resetPreviewView()

    }

    private fun resetPreviewView() {

        progressLabel.text = formatDuration(voiceManager.fileDuration.toLong())

        playButton.centerImage = R.drawable.voice_input_play
        playButton.trackValue = 0.toFloat()
        playButton.invalidate()

    }

    private fun onDurationUpdate() {

        durationLabel.text = formatDuration(voiceManager.duration)

    }

    private fun onProgressUpdate() {

        // 读取的 ms 有时会出现顺序错误，如 1000, 2000, 1200 这样的
        // 因此我们必须在此保证 ms 是递增的
        val progress = voiceManager.progress

        var trackValue = progress.toFloat() / voiceManager.fileDuration

        // 借助 playButton.trackValue 正好可以解决这个递增的问题
        if (trackValue > playButton.trackValue) {

            // 如果无限接近 1，可以认为是 1
            // 不然在结束时，可能出现 4000 / 4001 的尴尬局面
            if (trackValue > 0.99) {
                trackValue = 1f
            }

            playButton.trackValue = trackValue
            playButton.invalidate()

            progressLabel.text = formatDuration(progress)

        }

    }

    private fun cancel() {
        stopPlay()
        voiceManager.deleteFile()
        isPreviewing = false
    }

    private fun send() {
        stopPlay()
        isPreviewing = false
        callback?.onFinishRecord(voiceManager.filePath, voiceManager.fileDuration)
    }

    /**
     * 把时长格式化成 xx:xx
     */
    private fun formatDuration(duration: Long): String {

        var value = duration / 1000
        if (duration < 0) {
            value = 0
        }

        val minutes = value / 60
        val seconds = value - minutes * 60

        val a = if (minutes > 9) minutes.toString() else "0$minutes"
        val b = if (seconds > 9) seconds.toString() else "0$seconds"

        return "$a:$b"

    }

    /**
     * 获取单项权限的结果
     */
    fun hasPermission(permission: String): Boolean {
        return voiceManager.hasPermission(permission)
    }

    /**
     * 请求麦克风权限
     */
    fun requestPermissions(): Boolean {
        return voiceManager.requestPermissions()
    }

    /**
     * 如果触发了用户授权，则必须在 Activity 级别实现 onRequestPermissionsResult 接口，并调此方法完成授权
     */
    fun requestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        voiceManager.requestPermissionsResult(requestCode, grantResults)
    }

}