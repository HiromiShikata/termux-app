'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { test, expect } = require('@playwright/test');

const USERSCRIPT_PATH = path.join(__dirname, '..', 'google-meet-webrtc-clamp.user.js');
const LOOPBACK_FIXTURE_PATH = path.join(__dirname, 'fixtures', 'loopback.html');
const MEET_LOOPBACK_URL = 'https://meet.google.com/__test-loopback-fixture__';

async function gotoMeetLoopbackFixture(page) {
  await page.route(MEET_LOOPBACK_URL, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'text/html',
      body: fs.readFileSync(LOOPBACK_FIXTURE_PATH, 'utf8'),
    });
  });
  await page.addInitScript({ path: USERSCRIPT_PATH });
  await page.goto(MEET_LOOPBACK_URL);
}

test.describe('google-meet-webrtc-clamp userscript', () => {
  test('clamps getUserMedia video constraints via getSettings()', async ({ page }) => {
    await gotoMeetLoopbackFixture(page);

    const settings = await page.evaluate(() =>
      window.__loopbackTest.getUserMediaClampedSettings({
        video: { width: { ideal: 1920 }, height: { ideal: 1080 }, frameRate: { ideal: 30 } },
      }),
    );

    expect(settings.width).toBeLessThanOrEqual(640);
    expect(settings.height).toBeLessThanOrEqual(360);
    expect(settings.frameRate).toBeLessThanOrEqual(15);
  });

  test('clamps sender-side encoding parameters after addTrack over a loopback connection', async ({ page }) => {
    await gotoMeetLoopbackFixture(page);

    const result = await page.evaluate(() =>
      window.__loopbackTest.senderEncodingClampAfterAddTrack(1280, 720, 30),
    );

    expect(result.encodings.length).toBeGreaterThan(0);
    expect(result.encodings[0].maxFramerate).toBe(15);
    expect(result.encodings[0].scaleResolutionDownBy).toBeGreaterThanOrEqual(2);
  });

  test('setParameters on an already-connected sender applies and persists (Gecko reliability check)', async ({ page, browserName }) => {
    await gotoMeetLoopbackFixture(page);

    const result = await page.evaluate(() =>
      window.__loopbackTest.directSetParametersOnConnectedSenderReliability(1280, 720, 30),
    );

    expect(result.threw, `setParameters threw on ${browserName}: ${result.errorMessage}`).toBe(false);
    expect(result.appliedScaleResolutionDownBy).toBe(2);
    expect(result.appliedMaxFramerate).toBe(15);
  });

  test('does not apply on a page outside the meet.google.com origin', async ({ page }) => {
    const NON_MEET_URL = 'https://not-meet.invalid-test-origin/__test-fixture__';
    await page.route(NON_MEET_URL, async (route) => {
      await route.fulfill({ status: 200, contentType: 'text/html', body: '<!doctype html><html><body></body></html>' });
    });
    await page.addInitScript({ path: USERSCRIPT_PATH });
    await page.goto(NON_MEET_URL);

    const applied = await page.evaluate(() => window.__termuxMeetWebrtcClampApplied === true);
    const rtcPeerConnectionPatched = await page.evaluate(
      () => window.RTCPeerConnection && window.RTCPeerConnection.name === 'PatchedRTCPeerConnection',
    );

    expect(applied).toBe(true);
    expect(rtcPeerConnectionPatched).toBe(false);
  });
});
