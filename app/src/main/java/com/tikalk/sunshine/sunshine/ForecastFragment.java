package com.tikalk.sunshine.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.tikalk.sunshine.sunshine.data.db.WeatherContract;
import com.tikalk.sunshine.sunshine.sync.SunshineSyncAdapter;
import com.tikalk.sunshine.utils.Utility;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnItemClick;


public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static String FORECAST_FRAGMENT_TAG = ForecastFragment.class.getName();
    private static final String LAST_POSITION = "lastPosition";
    public static final String LOCATION_TAG="location";
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
    @Bind(R.id.listview_forecast)  ListView listView;
    @Bind(R.id.emptyList)    TextView emptyTextView;
    public ForecastFragment() {

    }

    @Override
    public void onPause() {
        Log.d(FORECAST_FRAGMENT_TAG, "onPause");
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
        Log.d(FORECAST_FRAGMENT_TAG, "onStop");
        super.onStop();
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(FORECAST_FRAGMENT_TAG, "onCreateOptionsMenu");
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
        if ( null != this.forecastAdapter ) {
            Cursor c = forecastAdapter.getCursor();
            if ( null != c ) {
                c.moveToPosition(0);
                String posLat = c.getString(COL_COORD_LAT);
                String posLong = c.getString(COL_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(FORECAST_FRAGMENT_TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
                }
            }

        }
    }
    @Override
    public void onStart() {
        Log.d(FORECAST_FRAGMENT_TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d(FORECAST_FRAGMENT_TAG, "onResume");
        super.onResume();
    }

    private void updateWeather() {
        Log.d(FORECAST_FRAGMENT_TAG, "updateWeather");
        SunshineSyncAdapter.syncImmediately(getContext());
//        AlarmHelper.setAlarm(getContext(), SunshineService.AlarmReceiver.class);

    }

    public void setTodayLayout(boolean todayLayout){
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
        Log.d(FORECAST_FRAGMENT_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        listView.setEmptyView(emptyTextView);

    }

    @OnItemClick({R.id.listview_forecast})
    public void selectNewDay(int position){
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
        Log.d(FORECAST_FRAGMENT_TAG, "onCreateView");
        final View mainView = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this,mainView);
        this.forecastAdapter = new ForecastAdapter(getActivity(), null, 0);
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
        Log.d(FORECAST_FRAGMENT_TAG, "onCreateLoader");
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
        Log.d(FORECAST_FRAGMENT_TAG, "onLoadFinished");
        forecastAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            listView.setSelection(mPosition);
            listView.smoothScrollToPosition(mPosition);

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(FORECAST_FRAGMENT_TAG, "onLoaderReset");
        forecastAdapter.swapCursor(null);
    }

    public void onLocationChanged() {
        Log.d(FORECAST_FRAGMENT_TAG, "onLocationChanged");
        updateWeather();
        getLoaderManager().initLoader(FORECAST_LOADER_ID, null, this);
    }
}

