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
    implementation 'com.github.herokotlin:VoiceInput:0.0.1'
}
```

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

```xml
<com.github.herokotlin.voiceinput.VoiceInput
    android:id="@+id/voiceInput"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```

```kotlin
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.github.herokotlin.voiceinput.Callback
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        voiceInput.callback = object: Callback {

            override fun onFinishRecord(filePath: String, duration: Int) {
                val audio = File(filePath)
                Log.d("VoiceInput", "${duration}, ${audio.length()}")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        voiceInput.requestPermissionsResult(requestCode, grantResults)
    }
}
```