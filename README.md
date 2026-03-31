# 🛡️ MindGate

**তোমার মনের দরজা** — একটি Android অ্যাপ যা তোমার ফোন ব্যবহারে সচেতনতা আনে।

---

## ✨ Features

### Feature 1 — সেশন টাইমার (App Session Lock)
- যেকোনো অ্যাপ খুললে সময় set করতে বলে
- নির্দিষ্ট সময় শেষে অ্যাপটি **lock** হয়ে যায়
- Lock screen এ countdown timer দেখায়
- Lock সময় শেষে আবার ব্যবহার করা যায়

### Feature 2 — গেট বার্তা (Mindfulness Gate)
- Selected app খোলার আগে একটি **সচেতনতার বার্তা** দেখায়
- **3 সেকেন্ড** (customizable) পরে skip করা যায়
- ফোন **unlock** করলেও বার্তা দেখাতে পারে
- সম্পূর্ণ নিজের মতো customize করা যায়

---

## 🚀 GitHub Actions দিয়ে Build করো

### Step 1: Repository তৈরি করো
1. GitHub এ নতুন repository বানাও (যেমন: `MindGate`)
2. এই সব files upload করো

### Step 2: Gradle Wrapper Jar যোগ করো
GitHub Actions এ build করার আগে একটি কাজ করতে হবে:

```
# তোমার PC/Termux এ:
cd MindGate
gradle wrapper --gradle-version 8.6
```

অথবা এই URL থেকে `gradle-wrapper.jar` ডাউনলোড করে `gradle/wrapper/` ফোল্ডারে রাখো:
```
https://github.com/gradle/gradle/raw/v8.6.0/gradle/wrapper/gradle-wrapper.jar
```

### Step 3: Push করো
```bash
git add .
git commit -m "Initial MindGate app"
git push origin main
```

### Step 4: APK ডাউনলোড করো
- GitHub → তোমার repo → **Actions** tab
- সবুজ checkmark দেখলে ক্লিক করো
- **Artifacts** সেকশন থেকে `MindGate-debug.apk` ডাউনলোড করো

---

## 📱 ইনস্টলের পরে করণীয়

1. **APK ইনস্টল** করো
2. অ্যাপ খোলো → দুটি **Permission** দিতে বলবে:
   - ⚙️ **Accessibility Service** → Settings → Accessibility → MindGate → চালু করো
   - 🪟 **Overlay Permission** → "এই অ্যাপের উপরে দেখানো" → অনুমতি দাও
3. উপরের **Master Toggle** চালু করো
4. **অ্যাপ যোগ করো** (+ বোতাম) → যে অ্যাপে feature চাও সেটা বেছে নাও
5. প্রতিটি অ্যাপে আলাদা করে Feature 1 বা Feature 2 চালু করো

---

## 🗂️ Project Structure

```
MindGate/
├── app/src/main/
│   ├── java/com/mindgate/app/
│   │   ├── MainActivity.kt          ← Dashboard UI
│   │   ├── MindGateApp.kt           ← Application class
│   │   ├── data/
│   │   │   ├── model/Models.kt      ← Data classes
│   │   │   └── datastore/           ← Settings storage
│   │   ├── service/
│   │   │   ├── MindGateAccessibilityService.kt  ← Core logic
│   │   │   └── MonitorService.kt    ← Foreground service
│   │   ├── receiver/
│   │   │   └── Receivers.kt         ← Boot + Screen unlock
│   │   └── ui/screens/
│   │       ├── GateOverlayActivity.kt   ← Gate + Lock screen
│   │       └── SessionSetActivity.kt    ← Session timer set
│   └── AndroidManifest.xml
├── .github/workflows/build.yml      ← GitHub Actions
└── README.md
```

---

## 🎨 UI Design

- **Dark theme**: গভীর নীল-কালো পটভূমি
- **Accent**: Teal (`#64FFDA`) — শান্ত, মনোযোগী রঙ
- **Breathing animation**: Gate screen এ ধীরে ধীরে সংকোচন-প্রসারণ
- **Lock screen**: লাল accent, countdown timer
- **Session set**: বড় সংখ্যা, quick preset buttons

---

## ⚠️ গুরুত্বপূর্ণ নোট

- এই অ্যাপ **Accessibility Service** ব্যবহার করে — এটা ছাড়া কাজ করবে না
- OPPO/Realme/Xiaomi ফোনে **Battery optimization** থেকে বাদ দিতে হবে
- Settings → Apps → MindGate → Battery → **Unrestricted** করো
