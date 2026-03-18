# NStates

![NStates app icon](app/src/main/ic_launcher-playstore.png)

NStates is an unofficial Android client for [NationStates](https://www.nationstates.net), the online nation simulation game. It lets you log in with your nation, check your stats, read and answer issues, search other nations, and manage everything from your phone. NStates is not affiliated with or endorsed by NationStates or Max Barry.

## Features

- Secure login with encrypted local token storage
- Nation overview with flag, government, economy, and other key stats
- Issue browser with support for answering or dismissing issues
- Optional AI issue chat powered by your own OpenRouter API key
- Optional issue translation powered by your own DeepL API key
- Nation search with pinned favorites
- Multi-account support
- Notifications for new issues
- Simple Jetpack Compose interface built for quick day-to-day use

## Download

NStates is currently in testing.

Google Play requires **12 testers for 14 days**, so if you want to install the testing build from Google Play, please join the Google Group first:

- [Join the Google Group](https://groups.google.com/g/nstates)
- [Download from Google Play](https://play.google.com/store/apps/details?id=it.rfmariano.nstates)
- [Download from GitHub Releases](https://github.com/Biri0/NStates/releases)

## Requirements

- Android 8.0 or newer
- A NationStates account for login
- An OpenRouter API key if you want to use the optional AI issue chat
- A DeepL API key if you want to enable issue translation

## What the app does

NStates communicates with the official NationStates API to:

- authenticate your nation
- load your nation profile and flag
- fetch current issues
- submit issue choices
- search and view other nations

The app stores only the minimum data needed on your device for authentication and settings. Your password is not stored.
