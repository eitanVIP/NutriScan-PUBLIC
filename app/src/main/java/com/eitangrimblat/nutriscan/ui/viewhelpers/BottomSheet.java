package com.eitangrimblat.nutriscan.ui.viewhelpers;

import android.content.res.Resources;
import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

/**
 * Wrapper for Material BottomSheetBehavior to simplify opening and closing bottom sheets.
 * @param <T> The type of the View used as the bottom sheet.
 */
public class BottomSheet<T extends View> {
    private T sheetView;
    private BottomSheetBehavior<T> sheetBehavior;

    /**
     * Initializes the BottomSheetBehavior with default settings (hidden, non-skip collapsed, etc.).
     * @param sheetView The view to be used as a bottom sheet.
     * @param resources Resources used to calculate display metrics for expanded offset.
     */
    public BottomSheet(T sheetView, Resources resources) {
        this.sheetView = sheetView;

        sheetBehavior = BottomSheetBehavior.from(sheetView);
        sheetBehavior.setPeekHeight(0);
        sheetBehavior.setHideable(true);
        sheetBehavior.setFitToContents(false);
        sheetBehavior.setExpandedOffset((int)(resources.getDisplayMetrics().heightPixels * 0.15));
        sheetBehavior.setHalfExpandedRatio(0.001f);
        sheetBehavior.setDraggable(true);
        sheetBehavior.setSkipCollapsed(true);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    /**
     * Executes the given Runnable and expands the bottom sheet.
     * @param onOpen Runnable to run before expanding.
     */
    public void open(Runnable onOpen) {
        sheetView.post(() -> {
            onOpen.run();
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
    }

    /**
     * Executes the given Runnable and hides the bottom sheet.
     * @param onClose Runnable to run before hiding.
     */
    public void close(Runnable onClose) {
        sheetView.post(() -> {
            onClose.run();
            sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        });
    }
}
