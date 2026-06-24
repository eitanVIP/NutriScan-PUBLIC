package com.eitangrimblat.nutriscan.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.eitangrimblat.nutriscan.R;

/**
 * Custom view representing a clickable item in the settings screen, typically containing an icon and text.
 */
public class SettingsItemView extends ConstraintLayout {
    public SettingsItemView(Context context) {
        super(context);
        init(context, null);
    }

    public SettingsItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SettingsItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * Propagates the click listener to the inner root view.
     * @param l The click listener to set.
     */
    @Override
    public void setOnClickListener(OnClickListener l) {
        super.setOnClickListener(l);

        View innerRoot = getChildAt(0);
        if (innerRoot != null)
            innerRoot.setOnClickListener(l);
    }

    /**
     * Initializes the view by inflating the layout and processing custom attributes.
     * @param context The application context.
     * @param attrs The attribute set from XML.
     */
    private void init(Context context, AttributeSet attrs) {
        // Inflate the item layout and attach it to this Custom View (root = this, attachToRoot = true)
        LayoutInflater.from(context).inflate(R.layout.settings_item_chevron, this, true);

        // Get references to the inner views
        TextView titleTextView = findViewById(R.id.tvSettingsItem);
        ImageView iconImageView = findViewById(R.id.icSettingsItem);

        // Process custom attributes
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SettingsItemView);

            try {
                // 1. Set the Title Text
                String titleText = a.getString(R.styleable.SettingsItemView_titleText);
                if (titleText != null) {
                    titleTextView.setText(titleText);
                }

                // 2. Set the Icon Source
                if (a.hasValue(R.styleable.SettingsItemView_iconSrc)) {
                    int iconResId = a.getResourceId(R.styleable.SettingsItemView_iconSrc, 0);
                    if (iconResId != 0) {
                        iconImageView.setImageResource(iconResId);
                    }
                }
            } finally {
                // IMPORTANT: Always recycle the TypedArray
                a.recycle();
            }
        }
    }
}
