# line-auto-answer

This app detects the notification of incoming VoIP call of LINE application, and automatically answer to the call after specified delay time. This app also posts the notifications to answer, decline and end call especially useful when opening on a smart watch.

## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
- [Limitations](#limitations)
- [License](#license)

## Installation

Build using Android Studio and generate apk, and install it manually. Currently this app is not on Google Play Store.

## Usage

+ This app needs permission to read notification. After installed, click "Permission to read notification" switch and grant permission to this app. It may be required to set `Allow restricted settings` before granting the permission.

+ After Android 12, Bluetooth permission is required. After Android 13, notification post permission is required. Grant them on first startup of application.

## Limitations

+ This app assumes of the behavior of "current" LINE app. Change of LINE app behavior may make this app not work correctly. 

+ If `Display over other app` is not permitted to LINE app, auto answer may not work correctly.

+ On Android 14, this app may not work correctly due to restrictions on starting activities from the background. I cannot check the behavior because I do not have Android 14 device.

## License

MIT License