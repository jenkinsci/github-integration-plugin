package com.github.kostyasha.github.integration.generic.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Used to wrap and retry calls to GitHub in the event an error is thrown.
 */
public class RetryableGitHubOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RetryableGitHubOperation.class);

    public interface GitOperation<T> {
        T execute() throws IOException;
    }

    private RetryableGitHubOperation() {
    }

    /**
     * Executes a GitHub operation up to 3 times with a 2 second delay.
     *
     * @param operation GitHub operation to execute
     * @return result of operation
     */
    public static <T> T execute(GitOperation<T> operation) throws IOException {
        return execute(3, 2000, operation);
    }

    public static <T> T execute(int retries, long delay, GitOperation<T> operation) throws IOException {
        T result = null;

        int count = 0;
        while (count++ < retries) {
            try {
                result = operation.execute();
                break;
            } catch (IOException e) {
                if (count == retries) {
                    throw e;
                }

                LOG.debug("Failed retrieving pull request(s), retrying...", e);
                sleep(delay);
            }
        }

        return result;
    }

    private static void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while delaying git operation", e);
        }
    }
}
