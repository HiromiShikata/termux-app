'use strict';

const fs = require('node:fs');
const path = require('node:path');
const {
  defaultGoogleMeetWebrtcClampConfig,
  buildGoogleMeetWebrtcClampScript,
} = require('../src/googleMeetWebrtcClampScript');

const USERSCRIPT_METADATA_BLOCK = [
  '// ==UserScript==',
  '// @name         Google Meet WebRTC Send-Side Clamp',
  '// @namespace    https://github.com/HiromiShikata/termux-app',
  '// @version      1.0.0',
  '// @description  Clamps outgoing Google Meet video resolution/framerate/bitrate to reduce CPU, battery, and network usage.',
  '// @author       HiromiShikata/termux-app',
  '// @match        https://meet.google.com/*',
  '// @run-at       document-start',
  '// @grant        none',
  '// ==/UserScript==',
].join('\n');

function renderGoogleMeetWebrtcClampUserscript() {
  const script = buildGoogleMeetWebrtcClampScript(defaultGoogleMeetWebrtcClampConfig());
  return USERSCRIPT_METADATA_BLOCK + '\n' + script + '\n';
}

function writeGoogleMeetWebrtcClampUserscript(outputPath) {
  fs.writeFileSync(outputPath, renderGoogleMeetWebrtcClampUserscript());
}

if (require.main === module) {
  const outputPath = path.join(__dirname, '..', 'google-meet-webrtc-clamp.user.js');
  writeGoogleMeetWebrtcClampUserscript(outputPath);
  process.stdout.write('Wrote ' + outputPath + '\n');
}

module.exports = {
  USERSCRIPT_METADATA_BLOCK,
  renderGoogleMeetWebrtcClampUserscript,
  writeGoogleMeetWebrtcClampUserscript,
};
