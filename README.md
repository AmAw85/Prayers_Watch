# 🕰️ Prayers Watch — the prayer clock app

**Languages:** [العربية](#-العربية) · [Français](#-français) · [English](#-english)

> 🤝 **Toute proposition de modification est bienvenue.**

---

## 🇩🇿 العربية

### نظرة عامة
**Prayers Watch** هو تطبيق أندرويد أصلي لمواقيت الصلاة.  
يعرض **ساعة تناظرية بمدة 12 ساعة**، مع **حالات الوقوت** مثل `CURRENT` / `STALE` / `UNAVAILABLE` / `UNVERIFIED`، ويعمل **دون اتصال بعد أول مزامنة ناجحة**.

اللغة الافتراضية هي **العربية (RTL)**، مع نسخ كاملة **فرنسية** و**إنجليزية**.

### حالة المشروع
- ✅ **Build:** `gradle :app:assembleDebug` ينجح.
- ✅ **Tests:** `gradle :app:testDebugUnitTest` ينجح (7 اختبارات، 0 فشل).
- ⏳ **UI runtime:** لم تُختبر بعد على جهاز أو محاكي.
- 🔁 **البيانات:** مصدر AlAdhan يُستخدم بحذر ويكتّب **„محسوب“**، وليس **„رسمي“**.

### البناء والاختبار
- **المنصة:** Android SDK 35، `minSdk 26`، Java 17.
- **Build:**
  ```sh
  JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 ~/.local/opt/gradle/bin/gradle :app:assembleDebug
  ```
- **Tests:**
  ```sh
  JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 ~/.local/opt/gradle/bin/gradle :app:testDebugUnitTest
  ```
- **قاعدة البيانات:** SQLite مع عمود غير متدمّر `sunrise_epoch`.

### قاعدة العمل
- **فترات الصلوات:**
  - الفجر → الشروق
  - الظهر → العصر
  - العصر → المغرب
  - المغرب → العشاء
  - العشاء → فجر اليوم التالي
- **الشروق** حدث شمسي مستقل، وليس صلاة سادسة.
- لا يُستنتج أي فترة إذا غاب طرف. لا يوجد وقت مخترع.

### المحدد
- **لا يوجد تكامل** مع واجهة رسمية جزائرية موثقة. لا تُستخدم مصادر غير موثقة.
- الأوقات الإضافية **اختيارية/ضرورية** أُزيلت من التطبيق.
- التطبيق قابل للاستخدام **دون اتصال بعد أول مزامنة ناجحة**. قبل ذلك، لا يُعرض أي وقت غير موثوق.

---

## 🇫🇷 Français

### Aperçu
**Prayers Watch** est une application Android native de l'horloge des prières.  
Elle affiche une **horloge analogique 12 h** avec **états de données** (`CURRENT`, `STALE`, `UNAVAILABLE`, `UNVERIFIED`) et fonctionne **hors ligne après la première synchronisation réussie**.

La langue par défaut est **l'arabe (RTL)**, avec des variantes **français** et **anglais**.

### État courant
- ✅ **Build :** `gradle :app:assembleDebug` passe.
- ✅ **Tests JVM :** `gradle :app:testDebugUnitTest` passe (7 tests, 0 échec).
- ⏳ **UI runtime :** non vue sur un appareil ou émulateur.
- 🔁 **Données :** source AlAdhan utilisée prudemment, classée **„calculée“**, jamais **„officielle“**.

### Compilation et tests
- **Plateforme :** Android SDK 35, `minSdk 26`, Java 17.
- **Build :**
  ```sh
  JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 ~/.local/opt/gradle/bin/gradle :app:assembleDebug
  ```
- **Tests :**
  ```sh
  JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 ~/.local/opt/gradle/bin/gradle :app:testDebugUnitTest
  ```
- **Base de données :** SQLite avec colonne non destructive `sunrise_epoch`.

### Règle métier
- **Intervalles :**
  - Fajr → Shurūq
  - Dhuhr → Asr
  - Asr → Maghrib
  - Maghrib → Isha
  - Isha → Fajr du **lendemain**
- Shurūq est un **événement solaire distinct**, pas une sixième prière.
- Aucun intervalle n'est inventé : si une borne manque, la période n'est pas affichée.

### Limites connues
- **Aucune API officielle algérienne documentée** n'est intégrée. Aucune source inventée.
- Les temps optionnels/obligatoires ont été **supprimés** de l'application.
- L'application reste **consultable hors ligne après la première synchronisation réussie**. Avant cela, aucun temps non vérifié n'est affiché.

---

## 🇬🇧 English

### Overview
**Prayers Watch** is a native Android prayer clock app.  
It shows a **12-hour analog clock** with **data states** (`CURRENT`, `STALE`, `UNAVAILABLE`, `UNVERIFIED`) and runs **offline after the first successful sync**.

The default language is **Arabic (RTL)** with full **French** and **English** variants.

### Current state
- ✅ **Build:** `gradle :app:assembleDebug` succeeds.
- ✅ **JVM tests:** `gradle :app:testDebugUnitTest` succeeds (7 tests, 0 failure).
- ⏳ **UI runtime:** not yet verified on an actual device or emulator.
- 🔁 **Data source:** AlAdhan is used carefully and labelled **calculated**, never **official**.

### Build and tests
- **Platform:** Android SDK 35, `minSdk 26`, Java 17.
- **Build:**
  ```sh
  JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 ~/.local/opt/gradle/bin/gradle :app:assembleDebug
  ```
- **Tests:**
  ```sh
  JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 ~/.local/opt/gradle/bin/gradle :app:testDebugUnitTest
  ```
- **Database:** SQLite with a non-destructive `sunrise_epoch` column.

### Business rule
- **Interval mapping:**
  - Fajr → Shurūq
  - Dhuhr → Asr
  - Asr → Maghrib
  - Maghrib → Isha
  - Isha → Fajr of the **next day**
- Shurūq is a **separate solar event**, not a sixth prayer.
- No interval is invented: if a bound is missing, the interval is not displayed.

### Known limits
- A documented **Algerian official prayer-time API** is **not** integrated, and none is invented.
- Optional/necessary **prayer slots** have been removed from the model and UI.
- The app remains **usable offline after the first successful sync**. Before that, no untrusted times are shown.
