Landmark Manager App

A Mobile App for Storing, Viewing, Updating & Deleting Bangladeshi Landmarks

📝 App Summary

Landmark Manager is a mobile application that allows users to create, view, update, and delete geographic landmarks.
Each landmark includes:

Title

Latitude & Longitude

Image

Server-side persistent storage

The app uses a REST API (PHP backend) and displays locations using OpenStreetMap (OSMDroid) instead of Google Maps.

The purpose of the project is to demonstrate real-world API integration, CRUD operations, image uploads, mapping, and basic offline data handling.

⭐ Feature List
✅ Core Features

Add new landmarks with:

Auto-detected GPS coordinates

Image (automatically resized to 800×600)

View all landmarks in:

Overview map view

List records view

Update existing landmarks (with or without image replacement)

Delete landmarks permanently from server

Real-time UI refresh across fragments

Robust API calls with Retrofit

Smooth image loading using Glide

🗺 Mapping

Uses OpenStreetMap (OSMDroid)

No API key required

Custom markers for landmarks

📡 Networking

Uses your assigned API:

https://labs.anontech.info/cse489/t3/api.php


Supports:

Method	Action
POST	Create Landmark
GET	Fetch All Landmarks
PUT	Update Landmark
DELETE	Delete Landmark
🗄 Optional Features (Bonus)

Offline caching with Room (basic structure prepared)

Automatic data refresh after CRUD operations

🛠 Setup Instructions
1️⃣ Clone the Repository
git clone <your-github-repo-link>
cd Landmark-Map-App

2️⃣ Open in Android Studio

File → Open

Choose the project folder

Wait for Gradle to finish

3️⃣ Required Permissions

Make sure your AndroidManifest.xml includes:

<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

4️⃣ Enable OpenStreetMap (OSMDroid)

No API key required.

Add inside <application>:

<meta-data
    android:name="org.osmdroid.config"
    android:value="com.example.landmark_app" />


And initialize in fragments:

Configuration.getInstance().userAgentValue = requireContext().packageName

5️⃣ Configure Retrofit

RetrofitInstance.kt already points to:

https://labs.anontech.info/cse489/t3/api.php

6️⃣ Run the App

Connect Android device

Press Run ▶

Test Create → View → Update → Delete

🧰 Project Structure
app/
 ├── ui/
 │    ├── EntryFragment.kt
 │    ├── OverviewFragment.kt
 │    ├── RecordsFragment.kt
 │
 ├── model/
 │    └── Landmark.kt
 │
 ├── network/
 │    ├── LandmarkApiService.kt
 │    └── RetrofitInstance.kt
 │
 ├── repository/
 │    └── LandmarkRepository.kt
 │
 ├── local/
 │    ├── LandmarkEntity.kt
 │    ├── LandmarkDao.kt
 │    └── AppDatabase.kt
 │
 ├── adapter/
 │    └── LandmarkAdapter.kt
 │
 └── MainActivity.kt

🧪 Known Limitations

Backend API does not return detailed error messages (common 400 issues).

Some server-side DELETE operations may appear slow due to caching.

Offline caching is partially implemented (can be extended).

API does not support searching or filtering on server side.

Large images require resizing, which may take a short moment on slower devices.
