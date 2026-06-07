# Hisab AI — AI-Powered Accounting for Pakistani Businesses

Hisab AI is a double-entry bookkeeping Android app built for wholesale distributors
in Pakistan. It supports Roman Urdu interface, credit (Khata) management, stock
tracking across showroom and godown, AI-powered chat assistant, and OCR receipt scanning.

## Tech Stack
- Kotlin + Jetpack Compose
- Room (SQLite) with double-entry journal
- Groq API via Supabase Edge Function (secure proxy)
- MLKit on-device OCR + Groq Vision for receipt parsing
- iText7 for PDF generation

## Setup

1. Clone: `git clone https://github.com/joking-really/Hisab-AI.git`
2. Open in Android Studio (Hedgehog or later)
3. Copy `.env.example` to `.env` and configure:
   - **Recommended:** `SUPABASE_URL` + `SUPABASE_ANON_KEY` (secure, API key never in APK)
   - **Fallback:** `GROQ_API_KEY` (key embedded in APK — less secure)
   - **Fallback 2:** `GEMINI_API_KEY` (key embedded in APK — less secure)
4. Build and run

## Security
- Supabase Edge Function proxies all AI API calls (keys never touch device)
- Database backup/export available via top-bar export button
- Debug keystore is NOT in repo (generate your own)

## Architecture
- Double-entry bookkeeping: Every transaction creates balanced Dr/Cr journal lines
- Offline-first: All data in local SQLite, works without internet
- AI assistant uses function calling to query real business data
