# 💳 StreetPay: The "I Forgot to Pay" Redemption Project

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-2025.12.00-green.svg)](https://developer.android.com/jetpack/compose)
[![Status](https://img.shields.io/badge/Status-In_Development-orange.svg)]()

## 📜 The Backstory (Or: My Moment of Shame)

It happened. I bought something from a local trader, and because her phone was a chaotic graveyard of SMS notifications, she couldn't find my payment confirmation. I—being a distracted human—forgot to wait, and I left without paying. 🤦‍♂️

Instead of just going back and paying (which I did, don't worry!), I decided to over-engineer a solution so this never happens to another trader again. 

**StreetPay** is a specialized Android application designed to turn a trader's phone into a high-visibility, hardware-controlled transaction terminal. No more scrolling through 500 "Win a Car!" spam messages to find one GHS 5.00 payment.

---

##  Features

- **SMS Auto-Magic**: Instantly parses incoming Momo (MTN) and Telecel (T-CASH) notifications. 
- **Color-Coded Sanity**: MTN is Yellow, Telecel is Red. It's so bright you could see a payment from across the market.
- **Physical Controller Support**: Connects to a custom embedded system via USB-OTG. Traders can navigate their transactions with physical buttons (Next, Prev, Seen) instead of fumbling with a touch screen.
- **"Proof-Read" IDs**: A parser so strict it would make my high school English teacher proud. No malformed IDs or duplicate records allowed.
- **Nerdy Analytics**: Track "Total Cash In" and "Average Transaction Value" without needing a spreadsheet.
- **Splashy Entrance**: A beautiful splash screen that makes the app feel like it cost a million bucks (even though it's for the humble street trader).

---

## 🛠️ Tech Stack (The "Nerdy" Part)

- **UI**: 100% Jetpack Compose (Declarative UI is the only way to live).
- **Database**: Room Persistence (Because SQLite is classic, but we like objects).
- **Concurrency**: Kotlin Coroutines & Flow (Keep that UI smooth while we crunch SMS data).
- **Hardware**: `usb-serial-for-android` (Talking to chips over a wire like it's 1999).
- **Architecture**: MVVM (Model-View-ViewModel—keeping things organized).

---

## 🔌 The Hardware (Embedded System)

*Documentation for the Arduino/ESP32 controller is coming soon!* 

Currently, the app listens for these serial commands at **115200 Baud**:
- `NEXT\n` - Move to the next transaction.
- `PREV\n` - Move to the previous transaction.
- `SEEN\n` - Mark the selected transaction as read.
- `UNSEEN\n` - Mark it as unread.

---

## 🛠️ Setup for Devs

1. Clone this repo.
2. Open in Android Studio (Ladybug or newer).
3. Connect an Android phone with USB Debugging enabled.
4. Hit the "Run" button and pray to the Gradle gods.

---

## ⚖️ License

Made with ❤️ and a slight sense of guilt by a developer who just wanted to pay for his plantain.

> "A trader's time is money. A developer's guilt is code." — Me, just now.
