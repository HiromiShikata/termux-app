'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const {
  DEFAULT_MAX_WIDTH,
  DEFAULT_MAX_HEIGHT,
  DEFAULT_MAX_FRAMERATE,
  defaultGoogleMeetWebrtcClampConfig,
  buildGoogleMeetWebrtcClampScript,
} = require('./googleMeetWebrtcClampScript');
const { renderGoogleMeetWebrtcClampUserscript } = require('../scripts/build-userscript');

test('disabled script injects disabled flag and returns early', () => {
  const script = buildGoogleMeetWebrtcClampScript({ enabled: false, maxWidth: 640, maxHeight: 360, maxFramerate: 15 });

  assert.ok(script.includes('var ENABLED=false;'));
  assert.ok(script.includes('if(!ENABLED){return;}'));
});

test('enabled script injects configured caps as constants', () => {
  const script = buildGoogleMeetWebrtcClampScript({ enabled: true, maxWidth: 480, maxHeight: 270, maxFramerate: 10 });

  assert.ok(script.includes('var ENABLED=true;'));
  assert.ok(script.includes('var MAX_WIDTH=480;'));
  assert.ok(script.includes('var MAX_HEIGHT=270;'));
  assert.ok(script.includes('var MAX_FRAMERATE=10;'));
});

test('script scopes itself to the meet origin', () => {
  const script = buildGoogleMeetWebrtcClampScript({ enabled: true, maxWidth: 320, maxHeight: 180, maxFramerate: 5 });

  assert.ok(script.includes('location.origin!==MEET_ORIGIN'));
  assert.ok(script.includes('var MEET_ORIGIN="https://meet.google.com";'));
});

test('script wraps getUserMedia and RTCPeerConnection', () => {
  const script = buildGoogleMeetWebrtcClampScript(defaultGoogleMeetWebrtcClampConfig());

  assert.ok(script.includes('navigator.mediaDevices.getUserMedia=function'));
  assert.ok(script.includes('window.RTCPeerConnection=PatchedRTCPeerConnection;'));
  assert.ok(script.includes('scaleResolutionDownBy'));
  assert.ok(script.includes('encoding.maxFramerate=MAX_FRAMERATE;'));
});

test('script also clamps sendEncodings at addTransceiver time', () => {
  const script = buildGoogleMeetWebrtcClampScript(defaultGoogleMeetWebrtcClampConfig());

  assert.ok(script.includes('clampTransceiverInitEncodings'));
  assert.ok(script.includes('next.sendEncodings=buildClampedEncodings(init.sendEncodings,track);'));
});

test('script is guarded by try/catch and a reentry flag', () => {
  const script = buildGoogleMeetWebrtcClampScript({ enabled: true, maxWidth: 640, maxHeight: 360, maxFramerate: 24 });

  assert.ok(script.startsWith('(function(){try{'));
  assert.ok(script.endsWith('catch(e){}})();'));
  assert.ok(script.includes('if(window.__termuxMeetWebrtcClampApplied){return;}'));
});

test('defaults produce an enabled script with baseline caps', () => {
  const script = buildGoogleMeetWebrtcClampScript(defaultGoogleMeetWebrtcClampConfig());

  assert.ok(script.includes('var ENABLED=true;'));
  assert.ok(script.includes('var MAX_WIDTH=' + DEFAULT_MAX_WIDTH + ';'));
  assert.ok(script.includes('var MAX_HEIGHT=' + DEFAULT_MAX_HEIGHT + ';'));
  assert.ok(script.includes('var MAX_FRAMERATE=' + DEFAULT_MAX_FRAMERATE + ';'));
});

test('checked-in userscript file matches the generator output', () => {
  const generated = renderGoogleMeetWebrtcClampUserscript();
  const checkedIn = fs.readFileSync(
    path.join(__dirname, '..', 'google-meet-webrtc-clamp.user.js'),
    'utf8',
  );

  assert.equal(checkedIn, generated);
});

test('checked-in userscript file carries userscript-manager metadata', () => {
  const checkedIn = fs.readFileSync(
    path.join(__dirname, '..', 'google-meet-webrtc-clamp.user.js'),
    'utf8',
  );

  assert.ok(checkedIn.includes('// ==UserScript=='));
  assert.ok(checkedIn.includes('// @match        https://meet.google.com/*'));
  assert.ok(checkedIn.includes('// @run-at       document-start'));
  assert.ok(checkedIn.includes('// ==/UserScript=='));
});
