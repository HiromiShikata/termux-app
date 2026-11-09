package com.termux.app.voicepatrol;

public final class VoicePatrolFailureMessages {

    static final String HTTP_PREFIX = "HTTP ";

    private VoicePatrolFailureMessages() {
    }

    public static String japaneseFor(String reasonCode) {
        if (reasonCode.startsWith(HTTP_PREFIX)) {
            return japaneseForHttpStatus(reasonCode.substring(HTTP_PREFIX.length()));
        }
        switch (reasonCode) {
            case "ice_gathering_timeout":
                return "接続の準備に時間がかかりすぎました";
            case "network_error":
                return "ネットワークに接続できませんでした";
            case "openai_api_key_missing":
                return "OpenAI の API キーが設定されていません";
            case "openai_live_session_failed":
                return "OpenAI との音声セッションを開始できませんでした";
            case "empty_sdp_answer":
                return "サーバーから応答がありませんでした";
            case "remote_description_failed":
            case "local_description_failed":
            case "offer_failed":
            case "ice_failed":
                return "接続の設定に失敗しました";
            case "peer_connection_unavailable":
                return "音声接続を作成できませんでした";
            case "event_channel_closed":
                return "音声の接続が切れました";
            case "tts_engine_unavailable":
            case "tts_engine_not_initialized":
                return "音声エンジンの準備ができていません";
            case "tts_engine_error":
                return "音声エンジンでエラーが起きました";
            case "tts_interrupted":
                return "読み上げが途中で止まりました";
            default:
                return "原因不明のエラーが起きました";
        }
    }

    private static String japaneseForHttpStatus(String statusText) {
        if (statusText.startsWith("4")) {
            return statusText.equals("401") || statusText.equals("403")
                ? "アクセスが許可されませんでした"
                : "サーバーがリクエストを受け付けませんでした";
        }
        if (statusText.startsWith("5")) {
            return "サーバーでエラーが起きました";
        }
        return "原因不明のエラーが起きました";
    }
}
