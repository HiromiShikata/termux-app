package com.termux.app.terminal.session;

import com.termux.app.terminal.SessionNewActivityState;

import org.json.JSONException;

import java.util.List;

public interface SessionNewActivityStateSerialization {

    String serialize(List<SessionNewActivityState> states) throws JSONException;

    List<SessionNewActivityState> deserialize(String serialized) throws JSONException;
}
