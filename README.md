# VoiceInput

![](https://user-images.githubusercontent.com/2732303/43363620-5caf8452-933b-11e8-8133-a026bd66373f.png)


Add it in your root build.gradle at the end of repositories:

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency

```
dependencies {
    implementation 'com.github.musicode:VoiceInput:0.0.9'
}
```

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

```xml
<com.github.musicode.voiceinput.VoiceInput
    android:id="@+id/voiceInput"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```

```kotlin
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val savePath = externalCacheDir.absolutePath + "/audio_record"
        val file = File(savePath)
        if (!file.exists()) {
            file.mkdir()
        }

        voiceInput.savePath = savePath
        voiceInput.onRecordSuccess = { file, duration ->
            val audio = File(file)
            Log.d("VoiceInput", "${duration}, ${audio.length()}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        voiceInput.requestPermissionsResult(this, requestCode, grantResults)
    }

}
```