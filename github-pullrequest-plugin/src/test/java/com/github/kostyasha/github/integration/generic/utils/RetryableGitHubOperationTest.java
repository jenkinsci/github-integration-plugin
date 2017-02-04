package com.github.kostyasha.github.integration.generic.utils;

import com.github.kostyasha.github.integration.generic.utils.RetryableGitHubOperation.GitOperation;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RetryableGitHubOperationTest {

    private Object actualResult;

    @Mock
    private GitOperation<Object> mockOperation;

    private Object mockResult;

    private int retries;

    private Exception thrownException;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testNoRetryRequired() throws Exception {
        givenOperationSucceeds();
        whenExecuteOperation();
        thenOperationIsSuccessful();
    }

    @Test
    public void testRetryWasExceeded() throws Exception {
        givenOperationExceedsRetry();
        whenExecuteOperation();
        thenOperationWasNotSuccessful();
    }

    @Test
    public void testRetryWasRequired() throws Exception {
        givenOperationRequiresRetry();
        whenExecuteOperation();
        thenOperationIsSuccessful();
    }

    private void givenOperationExceedsRetry() throws IOException {
        retries = 2;
        when(mockOperation.execute()).thenThrow(new FileNotFoundException());
    }

    private void givenOperationRequiresRetry() throws IOException {
        retries = 2;
        when(mockOperation.execute()).thenThrow(new FileNotFoundException())
                .thenReturn(mockResult);
    }

    private void givenOperationSucceeds() throws IOException {
        retries = 1;
        when(mockOperation.execute()).thenReturn(mockResult);
    }

    private void thenOperationIsSuccessful() throws IOException {
        assertThat(actualResult, is(mockResult));
        verify(mockOperation, times(retries)).execute();
    }

    private void thenOperationWasNotSuccessful() {
        assertThat(thrownException, instanceOf(FileNotFoundException.class));
    }

    private void whenExecuteOperation() {
        try {
            actualResult = RetryableGitHubOperation.execute(retries, 1L, mockOperation);
        } catch (Exception e) {
            thrownException = e;
        }
    }
}
