package com.tikalk.sunshine.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.CursorLoader;
import com.tikalk.sunshine.sunshine.data.db.WeatherContract;
import com.tikalk.sunshine.sunshine.tasks.FetchWeatherTask;
import com.tikalk.sunshine.utils.Utility;


public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String WEATHER_DATA = "WEATHER_DATA";
    public static final int FORECAST_LOADER_ID = 1;
    private ForecastAdapter forecastAdapter;
    private FetchWeatherTask fetchWeatherTask;
    private CursorLoader forecastLoader;

    public ForecastFragment() {

    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER_ID,null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStop() {
        if (fetchWeatherTask != null) {
            fetchWeatherTask.cancel(true);
        }
        fetchWeatherTask = null;
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.action_refresh) {
            updateWeather();
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWeather() {
        fetchWeatherTask = new FetchWeatherTask( getActivity());

        String location = Utility.getPreferredLocation(getActivity());

        fetchWeatherTask.execute(location);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View mainView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView listView = (ListView) mainView.findViewById(R.id.listview_forecast);

        this.forecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        listView.setAdapter(forecastAdapter);
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                final String item = forecastAdapter.getItem(position);
//                Intent detailedIntent = new Intent(getActivity(), DetailedActivity.class);
//                detailedIntent.putExtra(WEATHER_DATA, item);
//                startActivity(detailedIntent);
//            }
//        });
        return mainView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String locationSetting = Utility.getPreferredLocation(getActivity());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

         return new CursorLoader(getContext(),weatherForLocationUri,
                null, null, null, sortOrder);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        forecastAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        forecastAdapter.swapCursor(null);
    }
}

