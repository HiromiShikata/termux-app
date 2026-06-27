package com.termux.app;

import android.util.Log;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class RetryRule implements TestRule {

    private static final String LOG_TAG = "RetryRule";

    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final int maxAttempts;

    public RetryRule() {
        this(DEFAULT_MAX_ATTEMPTS);
    }

    public RetryRule(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1, was " + maxAttempts);
        }
        this.maxAttempts = maxAttempts;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Throwable lastFailure = null;
                for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                    try {
                        base.evaluate();
                        return;
                    } catch (Throwable failure) {
                        lastFailure = failure;
                        Log.w(LOG_TAG, "Attempt " + attempt + " of " + maxAttempts
                            + " failed for " + description.getDisplayName(), failure);
                    }
                }
                throw lastFailure;
            }
        };
    }
}
