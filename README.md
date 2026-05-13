# Ride-Hailing Search Automation (RHSA)
An Android accessibility tool that acts as a **price aggregator** across different ride-hailing apps, automating the search and price comparison process.

Currently supported apps:
- Grab
- Gojek
- Tada
- Zig

Built using **Jetpack Compose** and **Android Accessibility Services** on **Android Studio**.

## Core Features
- **Sequential Automation**: Automatically searches for fares across Grab, Gojek, Tada, and Zig sequentially with a single "Check Prices" trigger.
- **State Management**:
    - **Global Reset**: Automatically clears automation states when the main app is killed or restarted via a `BroadcastReceiver` handshake.
    - **Watchdog Timer**: A 15-second `Handler` timeout per app to prevent getting stuck on pop-ups, network lag, or unforseen issues.
- **Token Matching Search**: Selects the first search result that contains all words in the provided destination that is searched for.

## Installation & Usage
1. **Build & Install**: Open the project in Android Studio and deploy to a physical Android device.
2. **Enable Accessibility Service**:
    - Go to **Settings > Accessibility > Installed Apps**.
    - Enable **UniversalRideScraperService**.
    - Use the in-app status badge (✅/❌) to verify status.
3. **Search**: Enter a destination (e.g. "Bedok Mall") and tap **Check Prices**.

## Future TODOs

- [ ] **Postal Code Search**: Allow for search by postal code (6 digits) without token matching.
- [ ] **Re-search Feature**: Implement a feature that allows users to refresh the search for all apps, or individually.
- [ ] **Saved Destinations**: Allow users to input and edit saved destinations for easy access.
- [ ] **Toggling of Apps**: Allow users to toggle which apps to search for when the "Check Prices" button is pressed (default all `on`).
- [ ] **Headless Mode**: Implement a background "overlay" mode to display results without leaving the automation app.
