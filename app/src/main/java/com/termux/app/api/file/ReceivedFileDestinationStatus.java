package com.termux.app.api.file;

public enum ReceivedFileDestinationStatus {
    ACCEPTED,
    EMPTY_NAME,
    CURRENT_OR_PARENT_DIRECTORY_NAME,
    OUTSIDE_RECEIVE_DIRECTORY
}
