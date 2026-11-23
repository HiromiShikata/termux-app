package com.termux.app.voicepatrol;

import java.io.IOException;

public interface LiveToolCallForwarder {

    String forward(String toolName, String argumentsJson, String sessionName) throws IOException;
}
