package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VoicePatrolFailureMessagesTest {
    @Test
    public void knownReasonCodesMapToJapanesePhrases() {
        String[][] cases = {
            {"ice_gathering_timeout", "接続の準備に時間がかかりすぎました"},
            {"network_error", "ネットワークに接続できませんでした"},
            {"openai_api_key_missing", "OpenAI の API キーが設定されていません"},
            {"openai_live_session_failed", "OpenAI との音声セッションを開始できませんでした"},
            {"empty_sdp_answer", "サーバーから応答がありませんでした"},
            {"remote_description_failed", "接続の設定に失敗しました"},
            {"local_description_failed", "接続の設定に失敗しました"},
            {"offer_failed", "接続の設定に失敗しました"},
            {"ice_failed", "接続の設定に失敗しました"},
            {"peer_connection_unavailable", "音声接続を作成できませんでした"},
            {"event_channel_closed", "音声の接続が切れました"},
            {"tts_engine_unavailable", "音声エンジンの準備ができていません"},
            {"tts_engine_not_initialized", "音声エンジンの準備ができていません"},
            {"tts_engine_error", "音声エンジンでエラーが起きました"},
            {"tts_interrupted", "読み上げが途中で止まりました"}
        };
        for (String[] testCase : cases) {
            assertEquals(testCase[0], testCase[1], VoicePatrolFailureMessages.japaneseFor(testCase[0]));
        }
    }

    @Test
    public void httpStatusesMapToJapanesePhrases() {
        assertEquals("サーバーがリクエストを受け付けませんでした", VoicePatrolFailureMessages.japaneseFor("HTTP 400"));
        assertEquals("アクセスが許可されませんでした", VoicePatrolFailureMessages.japaneseFor("HTTP 401"));
        assertEquals("アクセスが許可されませんでした", VoicePatrolFailureMessages.japaneseFor("HTTP 403"));
        assertEquals("サーバーでエラーが起きました", VoicePatrolFailureMessages.japaneseFor("HTTP 503"));
    }

    @Test
    public void unknownReasonCodeMapsToDefaultPhrase() {
        assertEquals("原因不明のエラーが起きました", VoicePatrolFailureMessages.japaneseFor("unknown_reason"));
    }
}
