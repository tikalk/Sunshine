package com.tikalk.sunshine.sunshine;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import timber.log.Timber;

/**
 * Created by oren on 03/02/16.
 */
public class LocationEditTextPreference extends EditTextPreference  {
    public static final Integer DEFAULT_MIN_LENGTH = 2;

    int mLength;

    public LocationEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs,R.styleable.LocationEditTextPreference, 0, 0);
        try {
            mLength = typedArray.getInteger(R.styleable.LocationEditTextPreference_minLength, DEFAULT_MIN_LENGTH);
        } catch (Exception ex) {
            Timber.e(ex,ex.getMessage());
        } finally {
            typedArray.recycle();
        }
    }

    public int getmLength() {
        return mLength;
    }

    public void setmLength(int mLength) {
        this.mLength = mLength;
    }

    @Override
    public boolean shouldDisableDependents() {
        return super.shouldDisableDependents() && getText().length() < mLength;
    }
}
