# Final Term Project – Android App + Node.js Quiz Server

This project combines an interactive Android mobile application with a hosted Node.js-based quiz server to deliver both single-player and competitive 1v1 quiz experiences.

## 📱 Android App – Core Modules

The Android app, built with **Java** and **Firebase**, supports user authentication, dynamic quiz gameplay, and leaderboard tracking.

### 🔑 Authentication

* **`Login.java`** – Email/password-based login using Firebase Auth.
* **`RegisterActivity.java`** – Handles new user registration and error validation.

### 🧠 Quiz Flow

* **`MainActivity.java`** – Mode selector and navigation (single or 1v1).
* **`GameActivity.java`** – Core quiz gameplay:

  * Fetches questions from the backend via `/next-question`
  * Submits answers to `/submit-answer`
  * Records results in Firebase Realtime Database
  * Displays timer, handles UI state for 1v1 wait logic

### 🏆 Leaderboard

* **`LeadboardAct.java`** – Displays scores from Firebase `results/`, ranks users by correct answers with medal highlights.

## 🚀 How to Use

### 🔧 Install & Run App

1. Clone the branch:

```bash
git clone -b android_app https://github.com/DandRook/final_term_proj.git
```

2. Open in **Android Studio**.
3. Sync Gradle and run on device/emulator.

Make sure Firebase configuration is set in `google-services.json`.

### 🧪 Firebase Setup

* Ensure Firebase Realtime Database and Authentication are enabled.
* Project uses anonymous user IDs from Firebase.

## 🌐 Backend Quiz Server (Minimal Interaction Needed)

The backend runs at:

> [https://final-term-proj.onrender.com](https://final-term-proj.onrender.com)

It powers the following endpoints:

* `GET /next-question` – Supplies quiz questions
* `POST /submit-answer` – Validates user answers

The server is live; no changes are necessary unless customization is required.

## 👥 Authors

* GitHub: [@DandRook](https://github.com/DandRook)
* GitHub: [@Lauren0002](https://github.com/Lauren0002)
