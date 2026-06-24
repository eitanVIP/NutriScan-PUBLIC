# NutriScan

NutriScan is an Android app for smart nutrition management, home food inventory tracking, and personalized cooking. Built as a final high school (bagrut) project.

Users can scan food products in two ways: via barcode (using the OpenFoodFacts API) or via AI Scan, which visually identifies individual products using Gemini AI. After scanning, the app displays detailed nutritional information (calories, sugars, fats, etc.) and automatically saves the product to the user's digital pantry. From the pantry, the app can generate AI-powered recipes based on available ingredients with strict allergen filtering, and display nutritional statistics about the home food inventory.

> **Note:** This repository contains a single public commit of the final project. The original development history is in a private repo due to sensitive configuration data (API keys, Firebase config, etc.).

---

## Features

**Food Scanning**
- Barcode scanning via CameraX and ML Kit, fetching product data from the OpenFoodFacts API
- AI Scan mode using Gemini AI to visually identify food products from a camera image
- Detailed nutritional breakdown per scanned product (calories, sugars, fats, proteins, and more)

**Pantry Management**
- Products are saved automatically to the user's digital pantry after scanning
- Pantry stored and synced via Firebase Firestore
- Product images stored in Firebase Storage

**AI Recipe Generation**
- Generates dynamic recipes based on pantry contents using Gemini AI
- Strict allergen filtering based on user-defined allergen preferences
- Two-layer recipe caching: metadata cached via Gson/SharedPreferences, recipe images cached as JPEG files
- Background recipe pre-fetching via WorkManager

**Nutrition Statistics**
- Visual breakdown of the nutritional composition of the home food inventory

**User Accounts**
- Authentication via Firebase Auth (email/password, Google, Apple, GitHub)
- Profile editing and allergen preferences per user

---

## Tech Stack

- **Language:** Java
- **Architecture:** MVVM with LiveData
- **Backend:** Firebase Auth, Firestore, Storage
- **AI:** Gemini AI (recipe generation and AI Scan)
- **Camera:** CameraX
- **Barcode scanning:** ML Kit
- **Food data:** OpenFoodFacts API
- **Networking:** Retrofit
- **Image loading:** Glide
- **Serialization:** Gson
- **Background tasks:** WorkManager

---

## Project Structure

```
main/java/com/eitangrimblat/nutriscan/
├── data/               # Data models (Product, Nutriments, RecipeItem, StatisticsData, ...)
├── firebase/           # Firebase wrappers (Auth, Database, AI/Gemini)
├── nonui/              # Background logic (RecipesCache, RecipesWorker)
├── openfood/           # OpenFoodFacts API client (Retrofit)
└── ui/
    ├── activities/     # MainActivity, MainAppActivity, SignupActivity, SettingsActivity,
    │                   #   AllergiesActivity, EditProfileActivity
    ├── home/           # HomeFragment + HomeViewModel
    ├── recipes/        # RecipesFragment, RecipeFragment, RecipesViewModel
    ├── scan/           # ScanFragment + ScanViewModel
    ├── statistics/     # StatisticsFragment + StatisticsViewModel
    ├── viewhelpers/    # Adapters and BottomSheet (ProductAdapter, RecipeAdapter)
    └── views/          # Custom views (SettingsItemView)
```
