package com.github.herokotlin.voiceinput

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import com.github.herokotlin.circleview.CircleView
import com.github.herokotlin.circleview.CircleViewCallback
import kotlinx.android.synthetic.main.voice_input.view.*

class VoiceInput : FrameLayout {

    lateinit var configuration: VoiceInputConfiguration

    lateinit var callback: VoiceInputCallback

    // 用于请求权限
    var activity: Activity? = null

    val isRecording: Boolean

        get() {
            return voiceManager.isRecording
        }

    val isPlaying: Boolean

        get() {
            return voiceManager.isPlaying
        }

    private var voiceManager = VoiceManager()

    private var animator: ValueAnimator? = null

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
                guideLabel.text = resources.getString(R.string.voice_input_guide_label_preview)
            }
            else {
                previewButton.centerColor = ContextCompat.getColor(context, R.color.voice_input_preview_button_bg_color_normal)
                guideLabel.visibility = View.GONE
                durationLabel.visibility = View.VISIBLE
                guideLabel.text = resources.getString(R.string.voice_input_guide_label_normal)
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
                guideLabel.text = resources.getString(R.string.voice_input_guide_label_delete)
            }
            else {
                deleteButton.centerColor = ContextCompat.getColor(context, R.color.voice_input_delete_button_bg_color_normal)
                guideLabel.visibility = View.GONE
                durationLabel.visibility = View.VISIBLE
                guideLabel.text = resources.getString(R.string.voice_input_guide_label_normal)
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
            callback.onPreviewingChange(value)
        }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    fun init(configuration: VoiceInputConfiguration, callback: VoiceInputCallback) {
        this.configuration = configuration
        this.callback = callback
        voiceManager.fileDir = if (configuration.fileDir.isNotBlank()) configuration.fileDir else context.externalCacheDir.absolutePath
        voiceManager.fileExtname = configuration.fileExtname
        voiceManager.audioFormat = configuration.audioFormat
        voiceManager.audioEncoder = configuration.audioEncoder
        voiceManager.audioNumberOfChannels = configuration.audioNumberOfChannels
        voiceManager.audioBitRate = configuration.audioBitRate
        voiceManager.audioSampleRate = configuration.audioSampleRate
        voiceManager.audioMinDuration = configuration.audioMinDuration
        voiceManager.audioMaxDuration = configuration.audioMaxDuration
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
                callback.onRecordButtonClick()
                startRecord()
            }

            override fun onTouchUp(circleView: CircleView, inside: Boolean, isLongPress: Boolean) {
                stopRecord()
            }

            override fun onTouchMove(circleView: CircleView, x: Float, y: Float) {

                // 达到最大时长会自动停止
                if (!voiceManager.isRecording) {
                    return
                }

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

        previewButton.callback = object: CircleViewCallback { }
        deleteButton.callback = object: CircleViewCallback { }

        playButton.callback = object: CircleViewCallback {

            override fun onTouchDown(circleView: CircleView) {
                callback.onPlayButtonClick()
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

        submitButton.setOnClickListener {
            submit()
        }

        val permission = voiceManager.permission
        permission.onRequestPermissions = { activity, permissions, requestCode ->
            callback.onRequestPermissions(activity, permissions, requestCode)
        }
        permission.onPermissionsNotGranted = {
            callback.onPermissionsNotGranted()
        }
        permission.onPermissionsGranted = {
            callback.onPermissionsGranted()
        }
        permission.onPermissionsDenied = {
            callback.onPermissionsDenied()
        }
        permission.onExternalStorageNotWritable = {
            callback.onExternalStorageNotWritable()
        }

        voiceManager.onRecordDurationLessThanMinDuration = {
            callback.onRecordDurationLessThanMinDuration()
        }
        voiceManager.onFinishRecord = {
            finishRecord()
        }
        voiceManager.onFinishPlay = {
            finishPlay()
        }

    }

    private fun startRecord() {

        if (!requestPermissions()) {
            return
        }

        voiceManager.startRecord()

        if (voiceManager.isRecording) {

            recordButton.centerColor = ContextCompat.getColor(context, R.color.voice_input_record_button_bg_color_pressed)
            recordButton.invalidate()

            previewButton.visibility = View.VISIBLE
            deleteButton.visibility = View.VISIBLE

            guideLabel.visibility = View.GONE
            durationLabel.visibility = View.VISIBLE
            durationLabel.text = formatDuration(0)

            val animator = ValueAnimator.ofInt(0, 1)
            animator.duration = configuration.audioMaxDuration.toLong()
            animator.interpolator = LinearInterpolator()

            animator.addUpdateListener {
                onDurationUpdate()
            }

            animator.start()

            this.animator = animator

        }

    }

    fun stopRecord() {

        voiceManager.stopRecord()

    }

    private fun finishRecord() {

        animator?.cancel()

        if (voiceManager.filePath.isNotBlank()) {
            when {
                isPreviewButtonPressed -> {
                    isPreviewing = true
                }
                isDeleteButtonPressed -> {
                    voiceManager.deleteFile()
                }
                else -> {
                    callback.onFinishRecord(voiceManager.filePath, voiceManager.fileDuration)
                }
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

            val animator = ValueAnimator.ofInt(0, 1)
            // 多加 500，确保进度条能走完
            animator.duration = voiceManager.fileDuration.toLong() + 500
            animator.interpolator = LinearInterpolator()

            animator.addUpdateListener {
                onProgressUpdate()
            }

            animator.start()

            this.animator = animator

        }
    }

    fun stopPlay() {

        voiceManager.stopPlay()

    }

    private fun finishPlay() {

        animator?.cancel()

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

    private fun submit() {
        stopPlay()
        isPreviewing = false
        callback.onFinishRecord(voiceManager.filePath, voiceManager.fileDuration)
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
     * 请求麦克风权限
     */
    fun requestPermissions(): Boolean {
        val context = activity ?: (context as Activity)
        return voiceManager.permission.requestPermissions(context)
    }

    /**
     * 如果触发了用户授权，则必须在 Activity 级别实现 onRequestPermissionsResult 接口，并调此方法完成授权
     */
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        voiceManager.permission.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}