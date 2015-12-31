package com.tikalk.sunshine.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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

import com.tikalk.sunshine.sunshine.data.db.WeatherContract;
import com.tikalk.sunshine.sunshine.tasks.FetchWeatherTask;
import com.tikalk.sunshine.utils.Utility;


public class MainActivityFragment extends Fragment {
    public static final String WEATHER_DATA = "WEATHER_DATA";
    private ForecastAdapter forecastAdapter;
    private FetchWeatherTask fetchWeatherTask;

    public MainActivityFragment() {

    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
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
        fetchWeatherTask.execute();
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
        String locationSetting = Utility.getPreferredLocation(getActivity());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        Cursor cur = getActivity().getContentResolver().query(weatherForLocationUri,
                null, null, null, sortOrder);
        this.forecastAdapter = new ForecastAdapter(getActivity(), cur, 0);
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
}

