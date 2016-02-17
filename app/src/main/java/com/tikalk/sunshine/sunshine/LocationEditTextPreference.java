package com.tikalk.sunshine.sunshine;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

import timber.log.Timber;

/**
 * Created by oren on 03/02/16.
 */
public class LocationEditTextPreference extends EditTextPreference  {
    public static final Integer DEFAULT_MIN_LENGTH = 2;

    int minLength;

    public LocationEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs,R.styleable.LocationEditTextPreference, 0, 0);
        try {
            minLength = typedArray.getInteger(R.styleable.LocationEditTextPreference_minLength, DEFAULT_MIN_LENGTH);
        } catch (Exception ex) {
            Timber.e(ex,ex.getMessage());
        } finally {
            typedArray.recycle();
        }
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    @Override
    public boolean shouldDisableDependents() {
        boolean enableDependents = getText() != null && getText().length() >= minLength;
        return super.shouldDisableDependents() || !enableDependents;
    }
}
