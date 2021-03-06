package com.launchdarkly.android;

import com.google.gson.annotations.Expose;

public class LDInvalidResponseCodeFailure extends LDFailure {
    @Expose
    private int responseCode;
    @Expose
    private boolean retryable;

    public LDInvalidResponseCodeFailure(String message, int responseCode, boolean retryable) {
        super(message, FailureType.UNEXPECTED_RESPONSE_CODE);
        this.responseCode = responseCode;
        this.retryable = retryable;
    }

    public LDInvalidResponseCodeFailure(String message, Throwable cause, int responseCode, boolean retryable) {
        super(message, cause, FailureType.UNEXPECTED_RESPONSE_CODE);
        this.responseCode = responseCode;
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public int getResponseCode() {
        return responseCode;
    }
}