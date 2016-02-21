package com.tikalk.sunshine.sunshine;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.tikalk.sunshine.sunshine.data.db.WeatherContract;
import com.tikalk.sunshine.sunshine.sync.SunshineSyncAdapter;
import com.tikalk.sunshine.utils.Utility;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import timber.log.Timber;


public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String LAST_POSITION = "lastPosition";
    public static final String LOCATION_TAG = "location";
    private int mPosition;
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    public static final String WEATHER_DATA = "WEATHER_DATA";
    public static final int FORECAST_LOADER_ID = 0;
    private ForecastAdapter forecastAdapter;
    @Bind(R.id.listview_forecast)
    ListView listView;
    @Bind(R.id.emptyList)
    TextView emptyTextView;
    private SharedPreferences mSharedPreferences;

    public ForecastFragment() {

    }

    @Override
    public void onPause() {
        Timber.d( "onPause");
        super.onPause();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            int lastPosition = savedInstanceState.getInt(LAST_POSITION);
            if (lastPosition >= 0) {
                mPosition = lastPosition;
            }
        }
        getLoaderManager().initLoader(FORECAST_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStop() {
        Timber.d( "onStop");
        super.onStop();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Timber.d( "onCreateOptionsMenu");
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
//        if (itemId == R.id.action_refresh) {
//            updateWeather();
//        }
        if (itemId == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openPreferredLocationInMap() {
        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        if (null != this.forecastAdapter) {
            Cursor c = forecastAdapter.getCursor();
            if (null != c) {
                c.moveToPosition(0);
                String posLat = c.getString(COL_COORD_LAT);
                String posLong = c.getString(COL_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Timber.d( "Couldn't call %s  , no receiving apps installed!", geoLocation.toString());
                }
            }

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        Timber.d( "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        Timber.d( "onResume");
        super.onResume();
    }

    private void updateWeather() {
        Timber.d( "updateWeather");
        SunshineSyncAdapter.syncImmediately(getContext());
//        AlarmHelper.setAlarm(getContext(), SunshineService.AlarmReceiver.class);

    }

    public void setTodayLayout(boolean todayLayout) {
        forecastAdapter.setUseTodayLayout(todayLayout);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(LAST_POSITION, mPosition);
        }
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate");
        super.onCreate(savedInstanceState);
        mSharedPreferences = Utility.getLocationSharedPreferences(getContext());
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        setHasOptionsMenu(true);
    }

    @OnItemClick({R.id.listview_forecast})
    public void selectNewDay(int position) {
        // if it cannot seek to that position.
        Cursor cursor = (Cursor) listView.getItemAtPosition(position);
        if (cursor != null) {
            String locationSetting = Utility.getPreferredLocation(getActivity());
            ((Callback) getActivity()).onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                    locationSetting, cursor.getLong(COL_WEATHER_DATE)));

        }
        mPosition = position;
        listView.setSelection(mPosition);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d( "onCreateView");
        final View mainView = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, mainView);
        this.forecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        listView.setEmptyView(emptyTextView);
        listView.setAdapter(forecastAdapter);
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView adapterView, View view, int position, long l) {
//                // CursorAdapter returns a cursor at the correct position for getItem(), or null
//
//            }
//        });
        if (savedInstanceState != null && savedInstanceState.containsKey(LAST_POSITION)) {
            mPosition = savedInstanceState.getInt(LAST_POSITION);
        }
        return mainView;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
        forecastAdapter.changeCursor(null);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Timber.d( "onCreateLoader");
        String locationSetting = Utility.getPreferredLocation(getActivity());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        return new CursorLoader(getContext(), weatherForLocationUri,
                FORECAST_COLUMNS, null, null, sortOrder);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Timber.d( "onLoadFinished");
        forecastAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION && !forecastAdapter.isEmpty()) {
            listView.setSelection(mPosition);
            listView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Timber.d( "onLoaderReset");
        forecastAdapter.swapCursor(null);
    }

    public void onLocationChanged() {
        Timber.d( "onLocationChanged");
        updateWeather();
        getLoaderManager().initLoader(FORECAST_LOADER_ID, null, this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.last_location_long))) {
            updateEmptyMessageStatus();
        }


    }

    private void updateEmptyMessageStatus() {
        if (forecastAdapter.getCount() != 0) {
            return;
        }
        int locationStatus = Utility.getLocationStatus(getContext());
        String locationString = getString(R.string.empty_forecast);
        switch (locationStatus) {
            case SunshineSyncAdapter.LOCATION_STATUS_OK:
                locationString = "";
                break;
            case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                locationString = getString(R.string.empty_forecast_server_down);
                break;
            case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID:
                locationString = getString(R.string.empty_forecast_server_invalid);
                break;
            case SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN:
                locationString = getString(R.string.empty_forecast_server_unknown);
                break;
            case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                locationString = getString(R.string.empty_forecast_invalid_location);
                break;
            default:
                if (!Utility.isNetworkConnected(getContext())) {
                    locationString = getContext().getString(R.string.empty_forecast_no_internet);
                }
             break;
        }
        emptyTextView.setText(locationString);
    }
}



