# نتيجة البناء — 0.1.0 تجريبي
## نتيجة فعلية
- Gradle: testDebugUnitTest assembleDebug assembleDebugAndroidTest — BUILD SUCCESSFUL.
- 6 اختبارات Kotlin/JUnit: نجحت، 0 أخطاء و0 فشل، XML داخل verification.
- 9 اختبارات SQLite على الاستعلامات المستخرجة: نجحت.
- مصادر Room وCompose تجمعت، وRoom schema 1.json تولد عبر KSP.
- حزمة اختبارات Room الخمس تجمعت لكنها لم تُشغّل على جهاز أو محاكي.
- فحص APK باستخدام aapt: لا INTERNET أو صلاحيات تخزين أو جهات اتصال. يوجد إذن داخلي عادي من AndroidX للمستقبلات غير المصدّرة.

## المحتويات
artifacts/debt-ledger-debug.apk: APK موقع بتوقيع Debug للتجربة فقط.
artifacts/debt-ledger-androidTest.apk: حزمة اختبارات، ليست التطبيق الذي يفتحه المستخدم.
app/: مصدر التطبيق؛ gradlew وgradlew.bat موجودان مع checksum لتوزيعة Gradle.
docs/V0_PROMPT_AR.md: وصف جاهز لمعاينة الواجهات على v0.

## ما لم يُعتمد
المراجعة البصرية وRTL وTalkBack ولوحة المفاتيح على جهاز، واختبارات Room runtime، والنسخ المشفر والاستعادة. لا توجد نسخة Release موقعة بمفتاح ثابت للمستخدم بعد.
التصميم الحالي تنفيذ Compose وظيفي أولي؛ ليس استيرادًا لنتيجة v0، ولم يتم الدخول إلى v0 أو إنشاء مشروع هناك.

## بوابة هذه المرحلة
BUILD + UNIT TESTS: PASS.
DEVICE QA + BACKUP + RELEASE: PENDING.
لا تدّع نجاح الاختبارات الخمس على Android حتى تشغيل connectedDebugAndroidTest فعليًا.
