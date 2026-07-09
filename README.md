# cargolink - Turning empty trucks into earning assets

cargolink is a next-generation logistics platform connecting Vendors and Drivers with AI-powered route optimization, cost prediction, and secure payments.

## Features
- **AI Logistics Engine**: Powered by Google Gemini for cost prediction and driver matching.
- **Real-time Tracking**: Live GPS tracking for shipments.
- **Secure Payments**: Integrated Escrow and COD settlements with Razorpay and Stripe.
- **Digital Proof of Delivery**: QR code scanning and OTP verification for secure handovers.
- **Driver Verification**: Document upload and trust score system for reliable logistics.

## Tech Stack
- **Language**: Kotlin 2.3.0
- **UI Framework**: Jetpack Compose
- **Backend**: Firebase (Auth, Firestore, Messaging)
- **AI**: Google Generative AI (Gemini Flash 1.5)
- **Maps**: Google Maps Compose SDK
- **Processing**: KSP2 (Kotlin Symbol Processing)
- **DI**: Dagger Hilt

## Getting Started
1. Add your `GOOGLE_MAPS_KEY`, `GEMINI_API_KEY`, `RAZORPAY_KEY`, and `STRIPE_KEY` to `local.properties`.
2. Sync the project with Gradle.
3. Run the app on an Android device or emulator.
