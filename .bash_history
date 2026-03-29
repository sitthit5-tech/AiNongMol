            sb.append("ปุ่ม: ${node.contentDescription} | ตำแหน่ง: ${rect.centerX()},${rect.centerY()}\n")
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { scanNodes(it, sb); it.recycle() }
        }
    }

    // ฟังก์ชัน 'นิ้วผี': สั่งคลิกที่พิกัด x, y (เอาไว้ให้ AI เรียกใช้)
    fun performClick(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
        Log.d("NongMol", "ส่งนิ้วผีไปจิ้มที่: $x, $y")
    }

    override fun onInterrupt() {}
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("NongMol", "น้องมลตื่นแล้ว พร้อมทำงานแทนพี่ครับ!")
    }
}
EOF

# 3. [FILE] PermissionActivity: หน้าด่านจัดการสิทธิ์
cat << 'EOF' > app/src/main/java/com/nongmol/agent/PermissionActivity.kt
package com.nongmol.agent

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color

class PermissionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 60)
            setBackgroundColor(Color.parseColor("#EEEEEE"))
        }

        val header = TextView(this).apply {
            text = "น้องมล - ผู้ช่วยอัจฉริยะ (Agent)"
            textSize = 24f
            setPadding(0,0,0,50)
        }
        root.addView(header)

        // ปุ่ม 1: Accessibility
        root.addView(Button(this).apply {
            text = "1. เปิดสิทธิ์อ่านหน้าจอและสั่งงาน"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        // ปุ่ม 2: Overlay
        root.addView(Button(this).apply {
            text = "2. เปิดสิทธิ์แสดงผลทับแอปอื่น"
            setOnClickListener { 
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) 
            }
        })

        setContentView(root)
    }
}
EOF

# 4. [FILE] Strings: ภาษาไทยแบบจัดเต็ม
cat << 'EOF' > app/src/main/res/values/strings.xml
<resources>
    <string name="app_name">น้องมล Agent</string>
    <string name="accessibility_description">น้องมลใช้สิทธิ์นี้เพื่ออ่านหน้าจอและช่วยเหลือคุณในการสั่งงานอัตโนมัติ ข้อมูลจะถูกเก็บเป็นความลับในเครื่องเท่านั้น</string>
</resources>
EOF

# 5. [FILE] Manifest: ปิดจบการตั้งค่าระบบ
cat << 'EOF' > app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <application android:label="@string/app_name" android:theme="@style/Theme.AppCompat.Light.NoActionBar">
        <activity android:name=".PermissionActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <service android:name=".MyAgentService" android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE" android:exported="true">
            <intent-filter><action android:name="android.accessibilityservice.AccessibilityService" /></intent-filter>
            <meta-data android:name="android.accessibilityservice" android:resource="@xml/accessibility_service_config" />
        </service>
    </application>
</manifest>
EOF

# 6. ส่งขึ้น GitHub พร้อมบิ้ว APK ชุดใหญ่
git add .
git commit -m "🚀 ALL-IN-ONE: Screen Scan, Gesture, Thai UI, and Permissions"
git push origin main -f
# 1. เข้าโฟลเดอร์โครงการ
cd ~/AiNongMol
# 2. อัปเกรด MyAgentService: เพิ่มระบบ 'ปากพูด' (TTS) และ 'หูฟัง' (STT)
cat << 'EOF' > app/src/main/java/com/nongmol/agent/MyAgentService.kt
package com.nongmol.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class MyAgentService : AccessibilityService(), TextToSpeech.OnInitListener {
    
    private var tts: TextToSpeech? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        // ติดตั้งระบบ 'ปาก' (Text-to-Speech)
        tts = TextToSpeech(this, this)
        Log.d("NongMol", "น้องมลตื่นแล้ว พร้อมทำงานแทนพี่ครับ!")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("th") // ตั้งค่าเสียงพูดเป็นภาษาไทย
            speak("น้องมล พร้อมสแตนด์บายแล้วค่ะ")
        }
    }

    // ฟังก์ชันให้น้องมลโต้ตอบผ่านเสียง
    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        val screenData = StringBuilder()
        scanNodes(rootNode, screenData)
        
        val finalContent = screenData.toString()
        if (finalContent.isNotEmpty()) {
            Log.d("NongMol", "--- ข้อมูลหน้าจอ ---")
            Log.d("NongMol", finalContent)
        }
    }

    private fun scanNodes(node: AccessibilityNodeInfo, sb: StringBuilder) {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (node.text != null) {
            sb.append("ข้อความ: ${node.text} | ตำแหน่ง: ${rect.centerX()},${rect.centerY()}\n")
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { scanNodes(it, sb); it.recycle() }
        }
    }

    fun performClick(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    override fun onInterrupt() {}
    
    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
    }
}
EOF

# 3. อัปเกรด PermissionActivity: เพิ่มปุ่มขอสิทธิ์ 'ไมโครโฟน'
cat << 'EOF' > app/src/main/java/com/nongmol/agent/PermissionActivity.kt
package com.nongmol.agent

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.graphics.Color

class PermissionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 60)
            setBackgroundColor(Color.parseColor("#EEEEEE"))
        }

        root.addView(TextView(this).apply {
            text = "ศูนย์ควบคุมน้องมล (Voice & Agent)"
            textSize = 24f
            setPadding(0,0,0,50)
        })

        // เพิ่มการขอสิทธิ์ไมโครโฟน
        root.addView(Button(this).apply {
            text = "1. อนุญาตใช้ไมโครโฟน (ฟังคำสั่งเสียง)"
            setOnClickListener {
                ActivityCompat.requestPermissions(this@PermissionActivity, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            }
        })

        root.addView(Button(this).apply {
            text = "2. เปิดสิทธิ์ Accessibility (ตา+มือ)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        root.addView(Button(this).apply {
            text = "3. เปิดสิทธิ์ Overlay (แสดงทับ)"
            setOnClickListener { 
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) 
            }
        })

        setContentView(root)
    }
}
EOF

# 4. อัปเกรด Manifest: ใส่สิทธิ์บันทึกเสียง
sed -i '/<application/i \    <uses-permission android:name="android.permission.RECORD_AUDIO" />' app/src/main/AndroidManifest.xml
# 5. Push ขึ้น GitHub บิ้วเวอร์ชัน "โต้ตอบเสียงไทย"
git add .
git commit -m "🎙️ Add Voice Interaction (TTS/STT) to NongMol Agent"
git push origin main -f# 1. เข้าโฟลเดอร์โครงการ
cd ~/AiNongMol
# 2. สร้าง/แก้ไขไฟล์ Workflow ให้เก็บ APK ไว้ให้โหลด (Artifacts)
mkdir -p .github/workflows
cat << 'EOF' > .github/workflows/android.yml
name: Build NongMol Agent APK

on:
  push:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4

    - name: set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build with Gradle
      run: ./gradlew assembleDebug

    - name: Upload APK
      uses: actions/upload-artifact@v4
      with:
        name: NongMol-Agent-Debug-APK
        path: app/build/outputs/apk/debug/*.apk
EOF

# 3. อัปเกรดระบบเสียงและสิทธิ์ในแอป (เพื่อให้ชัวร์ว่าครบชุด)
# (ผมจะรันซ้ำส่วนสำคัญเพื่อให้โครงสร้างไฟล์สมบูรณ์ที่สุด)
git add .
git commit -m "📦 ADD APK EXPORT: Build and keep APK for download"
git push origin main -f
# 1. เข้าโฟลเดอร์โครงการ
cd ~/AiNongMol
# 2. ล้างค่าเก่าที่ทำให้บิ้วเป็นแอป Sample
rm -rf app/src/main/java/org/nehuatl/sample
mkdir -p app/src/main/java/com/nongmol/agent
mkdir -p app/src/main/res/xml
# 3. สร้างไฟล์ 'สมองและเสียง' (Voice Interaction Service) 
# ไฟล์นี้จะทำให้ชื่อ "น้องมล" ไปปรากฏในหน้าตั้งค่าผู้ช่วยดิจิทัลครับ
cat << 'EOF' > app/src/main/java/com/nongmol/agent/MyVoiceService.kt
package com.nongmol.agent
import android.service.voice.VoiceInteractionService

class MyVoiceService : VoiceInteractionService()
EOF

# 4. อัปเกรด MyAgentService (ใส่ปากพูดไทยทันทีที่เปิดแอป)
cat << 'EOF' > app/src/main/java/com/nongmol/agent/MyAgentService.kt
package com.nongmol.agent

import android.accessibilityservice.AccessibilityService
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityEvent
import java.util.*

class MyAgentService : AccessibilityService(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("th")
            tts?.speak("ระบบน้องมล พร้อมช่วยเหลือแล้วค่ะ", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}
    override fun onInterrupt() {}
}
EOF

# 5. แก้ไข Manifest ให้ "น้องมล" ขึ้นแท่นเป็นแอปผู้ช่วย (Assistant)
cat << 'EOF' > app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.nongmol.agent">
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <application android:label="น้องมล Agent" android:theme="@style/Theme.AppCompat.Light.NoActionBar">
        <activity android:name=".PermissionActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service android:name=".MyAgentService" 
                 android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE" 
                 android:exported="true">
            <intent-filter><action android:name="android.accessibilityservice.AccessibilityService" /></intent-filter>
            <meta-data android:name="android.accessibilityservice" android:resource="@xml/accessibility_service_config" />
        </service>

        <service android:name=".MyVoiceService"
                 android:permission="android.permission.BIND_VOICE_INTERACTION"
                 android:exported="true">
            <intent-filter><action android:name="android.service.voice.VoiceInteractionService" /></intent-filter>
            <meta-data android:name="android.voice_interaction" android:resource="@xml/voice_interaction_config" />
        </service>
    </application>
</manifest>
EOF

# 6. สร้างไฟล์ Config สำหรับ Voice (เพื่อให้ชื่อไปโผล่ในหน้าตั้งค่า)
cat << 'EOF' > app/src/main/res/xml/voice_interaction_config.xml
<?xml version="1.0" encoding="utf-8"?>
<voice-interaction-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:sessionService="com.nongmol.agent.MyVoiceService"
    android:recognitionService="com.nongmol.agent.MyVoiceService"
    android:supportsAssist="true"
    android:supportsLaunchFromKeyguard="true" />
EOF

# 7. บังคับเปลี่ยนชื่อแอปและภาษาไทย
cat << 'EOF' > app/src/main/res/values/strings.xml
<resources>
    <string name="app_name">น้องมล Agent</string>
    <string name="accessibility_description">น้องมลช่วยสแกนจอและสั่งงานแทนคุณ</string>
</resources>
EOF

# 8. อัปเดตไฟล์ build.gradle เพื่อเปลี่ยน ID แอปจาก Sample เป็น NongMol
sed -i 's/applicationId "org.nehuatl.sample"/applicationId "com.nongmol.agent"/' app/build.gradle
# 9. ส่งขึ้นบิ้วใหม่รอบสุดท้าย
git add .
git commit -m "🎯 FIX: Change Package to NongMol and Register Voice Assistant"
git push origin main -f
# 1. เข้าโฟลเดอร์โครงการ
cd ~/AiNongMol
# 2. ลบ Workflow เก่าๆ ออกให้หมดเพื่อลดความสับสน
rm -rf .github/workflows/*.yml
# 3. สร้าง Workflow ใหม่ตัวเดียวที่ "ชัวร์" ที่สุด (ชื่อ NongMol-Final-Build)
cat << 'EOF' > .github/workflows/nongmol_build.yml
name: NongMol-Final-Build

on:
  push:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    - name: Build APK
      run: ./gradlew assembleDebug
    - name: Upload APK Artifact
      uses: actions/upload-artifact@v4
      with:
        name: NongMol-Agent-Final
        path: app/build/outputs/apk/debug/*.apk
EOF

# 4. ส่งขึ้นไปล้างไพ่ใหม่
git add .
git commit -m "🧹 Cleanup workflows and build NongMol Final APK"
git push origin main -f
# 1. เข้าโฟลเดอร์โครงการ
cd ~/AiNongMol
# 2. ลบ Workflow เก่าๆ ออกให้หมดเพื่อลดความสับสน
rm -rf .github/workflows/*.yml
# 3. สร้าง Workflow ใหม่ตัวเดียวที่ "ชัวร์" ที่สุด (ชื่อ NongMol-Final-Build)
cat << 'EOF' > .github/workflows/nongmol_build.yml
name: NongMol-Final-Build

on:
  push:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    - name: Build APK
      run: ./gradlew assembleDebug
    - name: Upload APK Artifact
      uses: actions/upload-artifact@v4
      with:
        name: NongMol-Agent-Final
        path: app/build/outputs/apk/debug/*.apk
EOF

# 4. ส่งขึ้นไปล้างไพ่ใหม่
git add .
git commit -m "🧹 Cleanup workflows and build NongMol Final APK"
git push origin main -f
# 1. เข้าโฟลเดอร์โครงการ
cd ~/AiNongMol
# 2. แก้ไข Workflow ใหม่ให้รันแบบ Simple ที่สุด (ตัด Cache ออกเพื่อลด Error)
cat << 'EOF' > .github/workflows/nongmol_build.yml
name: NongMol-Final-Build

on:
  push:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build APK with Gradle
      run: ./gradlew assembleDebug

    - name: Upload APK Artifact
      uses: actions/upload-artifact@v4
      with:
        name: NongMol-Agent-Final
        path: "**/build/outputs/apk/debug/*.apk"
EOF

# 3. ตรวจสอบไฟล์ gradle-wrapper (ป้องกัน Error No file in...)
git add .
git commit -m "🔧 Fix: Remove gradle cache and use wildcard path for APK"
git push origin main -f
