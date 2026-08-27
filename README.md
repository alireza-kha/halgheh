# GlucoRing (Experimental) — پروژه پایه اندروید

اسکلت یک اپ اندرویدی Kotlin برای تخمین قند خون از روی موج PPG رینگ هوشمند
jstyle (blesdk2301 / "jcring")، با کالیبراسیون شخصی از روی اندازه‌گیری سوزنی
و معماری آماده برای افزودن سینک مرکزی در آینده.

## ⚠️ هشدار پزشکی — قبل از هر چیز بخوانید

تخمین قند خون از روی تغییرات نوری پوست (PPG) **در هیچ نهاد معتبری به‌عنوان
جایگزین اندازه‌گیری خونی تایید نشده** و در بهترین حالت یک تخمین شخصی‌سازی‌شده
با خطای قابل‌توجه است. این پروژه را همیشه به‌عنوان **ابزار پژوهشی/آزمایشی**
معرفی کنید، نه ابزار تشخیصی:

- هرگز طراحی نکنید که کاربر بر اساس عدد این اپ دوز انسولین بزند یا تصمیم
  درمانی بگیرد.
- اندازه‌گیری سوزنی همیشه باید منبع مرجع باقی بماند.
- کیفیت مدل (`ModelQuality` در `core-ml`) را همیشه به کاربر نشان بدهید — هدف
  این است که وقتی مدل قابل‌اعتماد نیست، اپ صادقانه بگوید «نمی‌دانم»، نه اینکه
  یک عدد نادرست نشان بدهد.

## ساختار ماژول‌ها

```
GlucoRing/
├── app/            رابط کاربری (Jetpack Compose) و سیم‌کشی برنامه
├── core-ble/       اتصال واقعی BLE GATT + wrapper روی SDK وندور
├── core-signal/    فیلتر و استخراج ویژگی از موج PPG خام
├── core-data/      دیتابیس محلی Room (Room) + repository
├── core-ml/        مدل شخصی‌سازی‌شده (ridge regression) + سنجش کیفیت (MARD)
└── core-sync/      فقط قرارداد/اینترفیس برای سینک آینده — چیزی ارسال نمی‌شود
```

## نکته‌ی مهم فنی: فرمان استریم موج خام PPG هنوز تایید نشده

داخل `2301sdk1.0.jar` کلید `arrayPpgRawData` در `DeviceKey` وجود دارد که
تایید می‌کند SDK از موج خام پشتیبانی می‌کند، اما مستندات ضمیمه‌شده
(`2301 Android SDK Documentation.doc`) فقط دستور اندازه‌گیری دوره‌ای علائم
حیاتی (کد بلوتوث `0x28`، `SetDeviceMeasurementWithType`) را مستند کرده — نه
فرمان شروع/توقف استریم پیوسته‌ی موج خام.

قبل از ادامه باید یکی از این‌ها را انجام دهید:
1. از سازنده (jstyle) مستندات کامل‌تر یا نسخه‌ی جدیدتر SDK بخواهید.
2. با یک ابزار sniff بلوتوثی (مثلاً Android Bluetooth HCI snoop log) ترافیک
   اپ رسمی‌شان را هنگام باز بودن صفحه‌ی "waveform/ECG-PPG" ضبط و فرمان دقیق
   را استخراج کنید.

محل دقیقی که باید تکمیل شود: `core-ble/.../GlucoRingBleClient.kt`، متد
`startRawPpgCapture()` (الان یک `NotImplementedError` پرتاب می‌کند و در
kdoc خودش توضیح کامل داده). بقیه‌ی پایپ‌لاین (`SdkDataListener` →
`ppgFrames` Flow → `PpgFeatureExtractor` → دیتابیس → مدل) از قبل آماده است و
به‌محض شروع دریافت فریم‌های `arrayPpgRawData` کار می‌کند.

تا وقتی این فرمان تایید نشده، مسیر علائم حیاتی دوره‌ای (`startVitalsAutoMeasurement`)
کاملاً مستند و کارکردنی است و می‌توانید با آن HR/SpO2/HRV/فشار خون تخمینی را
تست کنید.

## جریان داده (Data flow)

```
رینگ (BLE) → BleGattManager (GATT خام)
           → BleSDK.DataParsingWithData (SDK وندور)
           → SdkDataListener (Map → مدل‌های Kotlin)
           → GlucoRingBleClient.ppgFrames (Flow)
           → PpgFeatureExtractor (پنجره‌بندی + استخراج ویژگی)
           → GlucoRepository.logPpgWindow (ذخیره در Room)

کاربر (سوزن) → CalibrationViewModel.addReading
             → GlucoRepository.logGlucoseReference (لینک به نزدیک‌ترین پنجره PPG)

GlucoRepository.buildCalibrationDataset
    → RidgeRegressionCalibrator.train (رگرسیون ridge با اعتبارسنجی leave-one-out)
    → ذخیره در calibration_models (Room)
    → GlucoseEstimator.estimate (تخمین لحظه‌ای، فقط وقتی مدل موجود باشد)
```

## سینک مرکزی (برای آینده)

طبق انتخاب شما، فعلاً غیرفعال است — فقط `core-sync/SyncContract.kt` قرارداد
را مشخص کرده. نکات مهم قبل از فعال‌سازی واقعی:

- فقط بردار ویژگی + مقدار قند مرجع سینک شود، **نه موج خام**، نه چیزی که
  هویت واقعی کاربر را نشان بدهد.
- قبل از پیاده‌سازی بک‌اند، صفحه‌ی رضایت صریح (consent) طراحی کنید — این
  داده، داده‌ی سلامت حساس است.
- بسته به کشور/کاربران‌تان ممکن است نیاز به بررسی حقوقی/رگولاتوری هم داشته
  باشید.

## اجرا / ادامه‌ی کار

این پروژه در محیطی بدون Android SDK واقعی نوشته شده — من نتوانستم خودم آن را
واقعاً کامپایل کنم (نه Android SDK دارم، نه دسترسی شبکه به Google/Maven
Central). برای این‌که در **GitHub Actions** واقعاً بیلد شود، این‌ها را اضافه
کردم:

- **Gradle Wrapper واقعی** (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`) —
  دانلود شده از ریلیز رسمی Gradle 8.9، همان چیزی که Android Studio هم تولید
  می‌کند. بدون این فایل‌ها، `./gradlew` روی رانرِ GitHub اصلاً وجود ندارد.
- **`.github/workflows/android-ci.yml`** — روی هر push/PR به شاخه‌ی `main`،
  JDK 17 و Android SDK (پلتفرم 34 + build-tools 34.0.0) را نصب می‌کند،
  `./gradlew assembleDebug` و `./gradlew testDebugUnitTest` را اجرا می‌کند، و
  APK دیباگ را هم به‌عنوان artifact و هم (روی push به `main`) به‌عنوان یک
  **GitHub Release دائمی** منتشر می‌کند.
- **`.gitignore`** استاندارد اندروید/Gradle (شامل `local.properties`، پوشه‌های
  `build/`، `.idea/` و غیره) و یک `local.properties.example` به‌جایش.

### دانلود APK بدون سرچ توی تب Actions

بعد از اولین push موفق، APK دیباگ همیشه اینجا در دسترسه (لینک ثابت، بدون
لاگین، بدون انقضا — با هر push به `main` آپدیت می‌شه):

```
https://github.com/<owner>/<repo>/releases/tag/latest-debug
```

(روش قدیمی‌تر هم هنوز هست: تب Actions → اجرای موردنظر → پایین صفحه →
artifact با نام `glucoring-debug-apk`، ولی این یکی بعد از ۹۰ روز منقضی
می‌شه و نیاز به لاگین گیت‌هاب داره.)

⚠️ این APK امضای دیباگ داره (نه امضای انتشار) و صرفاً برای تست روی دستگاه
خودتونه، نه توزیع عمومی.

### قدم‌ها برای push کردن به گیت‌هاب

```bash
cd GlucoRing
git init
git add .
git commit -m "GlucoRing skeleton"
git branch -M main
git remote add origin <آدرس ریپوی شما>
git push -u origin main
```

بعد از push، تب **Actions** ریپو را چک کنید — اگر بیلد سبز شد یعنی پروژه واقعاً
کامپایل می‌شود؛ اگر قرمز شد، لاگ همان جا نشان می‌دهد کدام خط دقیقاً خطا داده
(چیزی که من در این محیط نمی‌توانم از قبل ببینم).

نکات تکمیلی:

1. برای توسعه‌ی محلی، پروژه را در Android Studio (Hedgehog یا جدیدتر) باز کنید؛
   `local.properties` را خودش می‌سازد.
2. صفحات UI فعلاً حداقلی هستند (فانکشنال ولی بدون طراحی نهایی) — قبل از
   انتشار به کاربر واقعی، حتماً بازطراحی بصری و تست دستگاه واقعی لازم است.
3. مقدار `assumedSampleRateHz` در `PpgFeatureExtractor` یک فرض اولیه است —
   وقتی به داده‌ی واقعی از رینگ دسترسی داشتید، نرخ نمونه‌برداری واقعی SDK را
   جایگزین کنید.
