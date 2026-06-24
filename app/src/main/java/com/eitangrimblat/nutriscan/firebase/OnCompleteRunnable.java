package com.eitangrimblat.nutriscan.firebase;

/**
 * Generic callback interface for asynchronous operations.
 *
 * @param <TResult> The type of the result returned on success.
 */
public interface OnCompleteRunnable<TResult> {
    /**
     * Called when an asynchronous task completes.
     *
     * @param result    The result of the task, if successful.
     * @param success   True if the task completed successfully.
     * @param exception A descriptive error message if the task failed.
     */
    void onComplete(TResult result, boolean success, String exception);
}
