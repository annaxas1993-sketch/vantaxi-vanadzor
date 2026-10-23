# Taxi Servis Vanadzor
Fresh Android project for two app variants: passenger and driver.

## Build
Run the GitHub Actions workflow **Build Taxi Servis Vanadzor APKs**. It generates separate passenger and driver debug APKs, signed with Android's standard debug key for installation/testing. These are not Google Play production releases. A private release keystore and secure signing secrets are required for production-signed APK/AAB files.

## Backend
Uses the existing Supabase project and the current RPC `submit_ride_order`. Before production, audit database RPC security, verify driver role policies, test end-to-end order assignment, and implement background GPS/realtime updates. Do not commit service-role keys or release keystores.
