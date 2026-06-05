# 🕌 SalahApp — تطبيق أوقات الصلاة والأذان (Native Android)

تطبيق Android أصلي (Java) لحساب أوقات الصلاة، تشغيل الأذان، وتذكير أذكار الصباح/المساء. يعمل **حتّى لو أغلقت التطبيق بالكامل** عبر `AlarmManager` + `ForegroundService` + `BootReceiver`.

---

## ✨ الميزات
- حساب أوقات الصلوات الخمس + الشروق (مكتبة Adhan).
- 9 طرق حساب (أم القرى، رابطة العالم، مصر، كراتشي، دبي، قطر، الكويت، ISNA، لجنة الرؤية).
- تشغيل الأذان تلقائيًا عند دخول الوقت (حتّى مع إغلاق التطبيق).
- تذكير أذكار الصباح والمساء.
- دعم RTL وواجهة عربية بالكامل.
- وضع ليلي / نهاري (Material 3).
- تحديث الموقع عبر GPS + تخزينه للعمل بدون إنترنت.
- دليل البطارية / المنبهات الدقيقة داخل الإعدادات.
- إعادة جدولة تلقائية بعد إعادة تشغيل الجهاز.

---

## 📦 المتطلبات
- **JDK 17** (أو أحدث)
- **Android SDK** (يوصى بتثبيت Android Studio Iguana/Koala)
- `compileSdk = 34`, `minSdk = 24`
- Internet لتنزيل التبعيات أول مرة

---

## 🚀 البناء

```bash
# 1) افتح المجلد
cd SalahApp

# 2) أعط صلاحية تنفيذ لـ gradlew (فقط للمرة الأولى على Linux/Mac)
chmod +x gradlew

# 3) أنشئ ملف local.properties يشير إلى مسار Android SDK عندك
#    مثال على Mac:    sdk.dir=/Users/USERNAME/Library/Android/sdk
#    مثال على Linux:  sdk.dir=/home/USERNAME/Android/Sdk
#    مثال على Windows: sdk.dir=C\:\\Users\\USERNAME\\AppData\\Local\\Android\\Sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties

# 4) بناء إصدار Release APK
./gradlew assembleRelease

# 5) ستجد الـ APK في
# app/build/outputs/apk/release/app-release.apk
```

على Windows استعمل: `gradlew.bat assembleRelease`

---

## 🎵 استبدال أصوات الأذان
الملفات الموجودة حالياً في `app/src/main/res/raw/` **فارغة (10 ثواني صمت)** كـ placeholders. استبدلها بملفات `.mp3` حقيقية بنفس الأسماء:

```
adhan_madinah.mp3   ← أذان المدينة (الافتراضي)
adhan_makkah.mp3    ← أذان مكة
adhan_kuwait.mp3    ← أذان الكويت
adhan_egypt.mp3     ← أذان مصر
```

**ملاحظة:** استخدم أحرف إنجليزية صغيرة فقط في أسماء الملفات (Android requirement).

---

## 🔓 التوقيع للنشر
`build.gradle` حالياً يستخدم مفتاح Debug الافتراضي للفائدة في `release` (لتستطيع التجربة فوراً). للنشر على Google Play:

1. أنشئ keystore:
```bash
keytool -genkey -v -keystore salah-release.jks -alias salah -keyalg RSA -keysize 2048 -validity 10000
```
2. أضف إلى `~/.gradle/gradle.properties`:
```properties
SALAH_STORE_FILE=/path/to/salah-release.jks
SALAH_STORE_PASSWORD=...
SALAH_KEY_ALIAS=salah
SALAH_KEY_PASSWORD=...
```
3. حدّث `app/build.gradle`:
```groovy
signingConfigs {
    release {
        storeFile file(SALAH_STORE_FILE)
        storePassword SALAH_STORE_PASSWORD
        keyAlias SALAH_KEY_ALIAS
        keyPassword SALAH_KEY_PASSWORD
    }
}
buildTypes.release.signingConfig signingConfigs.release
```
4. أنشئ AAB للنشر: `./gradlew bundleRelease`

---

## 📁 هيكل المشروع

```
SalahApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/salah/app/
│   │   │   ├── SalahApplication.java       ← تهيئة بدء التطبيق
│   │   │   ├── activities/
│   │   │   │   ├── MainActivity.java        ← الواجهة الرئيسية
│   │   │   │   └── SettingsActivity.java    ← إعدادات
│   │   │   ├── receivers/
│   │   │   │   ├── PrayerAlarmReceiver.java   ← إطلاق الأذان
│   │   │   │   ├── AthkarAlarmReceiver.java   ← تذكير الأذكار
│   │   │   │   └── BootCompletedReceiver.java ← إعادة جدولة بعد الإقلاع
│   │   │   ├── services/
│   │   │   │   ├── AdhanService.java          ← Foreground Service للأذان
│   │   │   │   ├── PrayerForegroundService.java
│   │   │   │   └── PrayerNotificationService.java
│   │   │   ├── utils/
│   │   │   │   ├── PrayerTimesCalculator.java
│   │   │   │   ├── AlarmScheduler.java        ← AlarmManager wrapper
│   │   │   │   ├── NotificationHelper.java
│   │   │   │   ├── WakeLockManager.java
│   │   │   │   ├── LocationHelper.java        ← FusedLocation + Geocoder
│   │   │   │   ├── PreferencesManager.java    ← SharedPreferences
│   │   │   │   └── PermissionHelper.java
│   │   │   ├── models/
│   │   │   │   ├── PrayerTime.java
│   │   │   │   ├── Location.java
│   │   │   │   └── UserSettings.java
│   │   │   └── adapters/
│   │   │       └── PrayerTimesAdapter.java    ← RecyclerView adapter
│   │   ├── res/
│   │   │   ├── layout/                    (XML layouts)
│   │   │   ├── values/                    (strings, colors, styles, arrays)
│   │   │   ├── values-night/              (الوضع الليلي)
│   │   │   ├── drawable/                  (الأيقونات)
│   │   │   ├── raw/                       (الأذان — MP3)
│   │   │   ├── xml/                       (backup_rules, file_paths)
│   │   │   └── mipmap-anydpi-v26/         (adaptive launcher icon)
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/
├── gradlew
├── gradlew.bat
├── .gitignore
└── README.md
```

---

## ⚠️ ملاحظات مهمة عن الأجهزة ذات إدارة البطارية العدوانية

على أجهزة Xiaomi/Huawei/Vivo/Oppo — إذا لم تعمل الإشعارات بعد إغلاق التطبيق، دخل لـ **الإعدادات داخل التطبيق** و:
1. اضغط على "إيقاف توفير البطارية للتطبيق".
2. اضغط على "فتح إعدادات المنبهات الدقيقة" وفعّلها.
3. في إعدادات الجهاز:
   - **Xiaomi:** التطبيقات ← SalahApp ← "التشغيل التلقائي" ✓ + "توفير البطارية" → بدون قيود.
   - **Samsung:** البطارية ← تطبيقات خاملة → احذف SalahApp منها.
   - **Huawei:** البطارية ← تشغيل التطبيق ← جعلها يدوية (الكل ✓).
   - **Vivo/Oppo:** البطارية ← استهلاك خلفية مرتفع → اسمح.

---

## 🧪 الاختبار
1. افتح التطبيق واسمح بإذن الموقع.
2. في `SettingsActivity` غيّر وقت أذكار الصباح إلى "خلال دقيقة" (عبر تعديل سريع لـ hour/min في الكود أو إضافة TimePicker).
3. أغلق التطبيق واسحبه من Recent Apps.
4. افصل الشاشة وانتظر دقيقة → يجب أن يظهر إشعار الأذكار مع اهتزاز.

---

## 📜 الترخيص
مفتوح المصدر للاستخدام الشخصي والدعوي. مكتبة Adhan تحت Apache 2.0.

**أسأل الله أن يبارك فيه ويجعله صدقة جارية 🤲**
