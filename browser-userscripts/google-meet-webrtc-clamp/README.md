# Google Meet WebRTC Send-Side Clamp (userscript)

A standalone Violentmonkey/Tampermonkey userscript that clamps outgoing Google
Meet video resolution, framerate, and bitrate, to reduce CPU, battery, and
network usage. It applies the same clamp technique already used by this
repository's in-app WebView browser
(`app/src/main/java/com/termux/app/browser/BrowserMeetLowPowerVideoSettings.java`),
packaged for use in an external browser (e.g. Firefox for Android) via a
userscript manager extension.

## Install

Install a userscript manager extension (Violentmonkey or Tampermonkey), then
install `google-meet-webrtc-clamp.user.js` from this directory.

## How it works

- Wraps `navigator.mediaDevices.getUserMedia` so requested video constraints
  (width/height/frameRate) are clamped before reaching the camera.
- Wraps `RTCPeerConnection` so that, once a video sender exists (via
  `addTrack`, `addTransceiver`, or renegotiation), its encoding parameters are
  clamped through `RTCRtpSender.setParameters` (`scaleResolutionDownBy`,
  `maxFramerate`), and — when the transceiver API is used — pre-clamped via
  `sendEncodings` at `addTransceiver` time as well.
- Restricted to `https://meet.google.com` both via the userscript `@match`
  metadata and a runtime origin check.

## Development

```sh
npm install
npx playwright install chromium firefox
npm run build     # regenerate google-meet-webrtc-clamp.user.js from src/
npm run test:unit # unit tests for the script generator (Node's built-in test runner)
npm run test:e2e  # cross-engine (Chromium + Firefox) loopback tests (Playwright)
npm test          # both
```

`src/googleMeetWebrtcClampScript.js` is the single source of truth for the
clamp logic; `google-meet-webrtc-clamp.user.js` is generated from it by
`scripts/build-userscript.js` and is checked in so it can be installed
directly without a build step. CI verifies the checked-in file stays in sync
with the generator.

The Playwright suite (`tests/googleMeetWebrtcClamp.spec.js`) uses a same-page
loopback pair of `RTCPeerConnection` objects (no external signaling or real
Google Meet infrastructure) served under the `https://meet.google.com` origin
via request interception, and runs against both a Chromium and a Firefox
project to verify constraint clamping (`getUserMedia` + `getSettings()`) and
sender-side encoding caps (`RTCRtpSender.setParameters` /
`getParameters()`), including a dedicated check of whether calling
`setParameters` on an already-connected sender is reliable in Firefox.
