package com.sanbot.capaBot;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static com.sanbot.capaBot.MyWeatherActivity.cityField;
import static com.sanbot.capaBot.MyWeatherActivity.summaryField;
import static com.sanbot.capaBot.MyWeatherActivity.loader;
import static com.sanbot.capaBot.MyWeatherActivity.updatedField;

public class MyWeatherDownloadAsyncTask extends AsyncTask< String, Void, String > {

    private final static String TAG = "IGOR-WEATHER-ASYNC";

    private static String OPEN_WEATHER_MAP_API = "c80605bac44c38a9af00b1b18a420bd0";
    //private static String DARKSKY_API = "9c096d766f224ca50a27b01941c69c02";

    private static LinearLayout forecastContainerLL;
    private static Context context;

    private AsyncTaskListener listener;
    String summaryToSpeak = "no weekly summary available";

    MyWeatherDownloadAsyncTask(Context cont, LinearLayout containerLinearLayout) {
        context = cont;
        listener = (AsyncTaskListener)cont;
        forecastContainerLL = containerLinearLayout;
    }

    @Override
    protected void onPreExecute() {
        Log.i(TAG, "setting visible the loader icon");
        super.onPreExecute();
        loader.setVisibility(View.VISIBLE);
    }


    protected String doInBackground(String... args) {
        String latitude = "0";
        String longitude = "0";
        //args has the query string passed, to geocode
        String cityName = args[0];
        //GEOCODE
        String requestGeocode = "https://api.openweathermap.org/geo/1.0/direct?q=" + Uri.encode(cityName) + "&limit=1&appid=" + OPEN_WEATHER_MAP_API;
        Log.i(TAG, "requestGeocode: "+requestGeocode);
        String xmlGeo = null;
        try {
            xmlGeo = getJSONStringFromURL(requestGeocode);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (xmlGeo != null) {
                JSONArray jsonArray = new JSONArray(xmlGeo);
                if (jsonArray.length() > 0) {
                    JSONObject json = jsonArray.getJSONObject(0);
                    latitude = String.valueOf(json.getDouble("lat"));
                    longitude = String.valueOf(json.getDouble("lon"));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG,"Error, Check Geocoding", e);
        } catch (NullPointerException npe) {
            Log.e(TAG,"Error, geocoding data not available", npe);
        }
        Log.i(TAG, "Geocoded Lat: "+ latitude);
        Log.i(TAG, "Geocoded Lon: "+ longitude);
        //REQUEST
        String request = "https://api.openweathermap.org/data/2.5/forecast?lat="+latitude+"&lon="+longitude+"&units=metric&appid="+OPEN_WEATHER_MAP_API;
        //String todayrequest = "http://api.openweathermap.org/data/2.5/weather?q=" + cityName + "&units=metric&appid="+OPEN_WEATHER_MAP_API;
        //String request = "https://api.darksky.net/forecast/"+DARKSKY_API+"/"+latitude+","+longitude+"?units=si";
        Log.i(TAG, "request: "+request);
        String xml = null;
        try {
            xml = getJSONStringFromURL(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "XML RECEIVED:"+xml);
        return xml;
    }

    @Override
    protected void onPostExecute(String xml) {
        Log.i(TAG, "XML PROCESSED:"+xml);
        try {
            if (xml != null) {
                JSONObject json = new JSONObject(xml);

                processOpenWeatherJSON(json);
                //processTodayOpenWeatherJSON(json);
                //processDarkSkyJSON(json);

                loader.setVisibility(View.GONE);
                Log.i(TAG, "Progress OK");
                listener.giveProgress("OK", summaryToSpeak);
            } else {
                Log.e(TAG, "Error, xml null");
            }
        } catch (JSONException e) {
            Log.e(TAG,"Error, Check City", e);
        } catch (NullPointerException npe) {
            Log.e(TAG, "Error, data not available", npe);
            npe.printStackTrace();
        }
    }

    void processOpenWeatherJSON(JSONObject json) {
        Log.i(TAG, "Processing OpenWeather Json");
        try{
            JSONObject cityObj = json.getJSONObject("city");
            JSONArray forecastArray = json.getJSONArray("list");
            cityField.setText(cityObj.getString("name").toUpperCase(Locale.US) + ", " + cityObj.getString("country"));
            updatedField.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(new Date(forecastArray.getJSONObject(0).getLong("dt") * 1000)));
            //string builder for text summary of the next days
            StringBuilder summary = new StringBuilder();
            //inflater
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            //crunch data of every 3h in a daily data
            Map<String, List<Double>> tempMap = new LinkedHashMap<>();
            Map<String, List<Integer>> idMap = new LinkedHashMap<>();
            Map<String, List<String>> descMap = new LinkedHashMap<>();
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            for (int i = 0; i < forecastArray.length(); i++) {
                //GET THE DAY and put it AS HASH
                JSONObject entry = forecastArray.getJSONObject(i);
                String dateTime = entry.getString("dt_txt");
                Date date = inputFormat.parse(dateTime);
                String day = dayFormat.format(date);
                if (!tempMap.containsKey(day)) {
                    tempMap.put(day, new ArrayList<Double>());
                    idMap.put(day, new ArrayList<Integer>());
                    descMap.put(day, new ArrayList<String>());
                }
                //PUT TEMP, ID, and DESCRIPTION IN THE TEMP LIST AND DESC LIST
                double temp = entry.getJSONObject("main").getDouble("temp");
                int id = entry.getJSONArray("weather").getJSONObject(0).getInt("id");
                String desc = entry.getJSONArray("weather").getJSONObject(0).getString("description");
                tempMap.get(day).add(temp);
                idMap.get(day).add(id);
                descMap.get(day).add(desc);
            }
            //FOR EVERY DAY
            int dayIndex=0;
            for (String dayString : tempMap.keySet()) {
                List<Double> temps = tempMap.get(dayString);
                List<Integer> ids = idMap.get(dayString);
                List<String> descs = descMap.get(dayString);
                //average temp of day
                double totalTemp = 0.0;
                for (double t : temps) totalTemp += t;
                double avgDayTemp = temps.size() > 0 ? totalTemp / temps.size() : 0;
                //Find most frequent id
                Map<Integer, Integer> freqIdMap = new HashMap<Integer, Integer>();
                for (int id : ids) {
                    if (!freqIdMap.containsKey(id)) freqIdMap.put(id, 1);
                    else freqIdMap.put(id, freqIdMap.get(id) + 1);
                }
                int mostCommonId = 800;
                int maxIdCount = 0;
                for (Map.Entry<Integer, Integer> entry : freqIdMap.entrySet()) {
                    if (entry.getValue() > maxIdCount) {
                        maxIdCount = entry.getValue();
                        mostCommonId = entry.getKey();
                    }
                }
                //Find most frequent description
                Map<String, Integer> freqMap = new HashMap<String, Integer>();
                for (String d : descs) {
                    if (!freqMap.containsKey(d)) freqMap.put(d, 1);
                    else freqMap.put(d, freqMap.get(d) + 1);
                }
                String mostCommonDesc = "";
                int maxDescCount = 0;
                for (Map.Entry<String, Integer> entry : freqMap.entrySet()) {
                    if (entry.getValue() > maxDescCount) {
                        maxDescCount = entry.getValue();
                        mostCommonDesc = entry.getKey();
                    }
                }
                //inflate day with element
                LinearLayout clickableColumn = (LinearLayout) inflater.inflate(
                        R.layout.weather_element_layout, forecastContainerLL);
                //put day date
                TextView date = forecastContainerLL.getChildAt(dayIndex).findViewById(R.id.date_field);
                if(dayIndex==0) {
                    date.setText("today");
                    summary.append("today ");
                } else if (dayIndex==1) {
                    date.setText("tomorrow");
                    summary.append("tomorrow ");
                } else {
                    date.setText(dayString);
                }
                //put day icon
                TextView icon = forecastContainerLL.getChildAt(dayIndex).findViewById(R.id.weather_icon);
                Typeface typeface = ResourcesCompat.getFont(context, R.font.weathericons);
                icon.setTypeface(typeface);
                icon.setText(Html.fromHtml(setOpenWeatherIcon(mostCommonId,new Date().getTime(),new Date().getTime()+100000)));
                //day details
                TextView details = forecastContainerLL.getChildAt(dayIndex).findViewById(R.id.details_field);
                details.setText(mostCommonDesc);
                //day temperature
                TextView temperature = forecastContainerLL.getChildAt(dayIndex).findViewById(R.id.current_temperature_field);
                temperature.setText(String.format(Locale.US,"%.0f°", avgDayTemp));
                //add day to summary to speak
                if(dayIndex==0 || dayIndex==1) { //only for today and tomorrow
                    summary.append(String.format(Locale.US, "%s %.0f°. ", mostCommonDesc, avgDayTemp));
                }
                //increment day index counter
                dayIndex++;
            }
            summaryToSpeak = summary.toString();
            summaryField.setText(summaryToSpeak);
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error", e);
        } catch (ParseException e) {
            Log.e(TAG, "Date parsing error", e);
        }
    }

    void processTodayOpenWeatherJSON(JSONObject json) {
        Log.i(TAG, "Processing TodayOpenWeather Json");
        try{
            JSONObject weatherObj = json.getJSONArray("weather").getJSONObject(0);
            JSONObject mainObj = json.getJSONObject("main");
            JSONObject sysObj = json.getJSONObject("sys");
            cityField.setText(json.getString("name").toUpperCase(Locale.US) + ", " + sysObj.getString("country"));
            summaryToSpeak = weatherObj.getString("description").toUpperCase(Locale.US);
            summaryField.setText(summaryToSpeak);
            updatedField.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(new Date(json.getLong("dt") * 1000)));
            //process inflated layout
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            LinearLayout clickableColumn = (LinearLayout) inflater.inflate(R.layout.weather_element_layout, forecastContainerLL);
            //put day date
            TextView date = forecastContainerLL.getChildAt(0).findViewById(R.id.date_field);
            date.setText("today");
            //put day icon
            TextView icon = forecastContainerLL.getChildAt(0).findViewById(R.id.weather_icon);
            Typeface typeface = ResourcesCompat.getFont(context, R.font.weathericons);
            icon.setTypeface(typeface);
            icon.setText(Html.fromHtml(setOpenWeatherIcon(weatherObj.getInt("id"),
                    sysObj.getLong("sunrise") * 1000,
                    sysObj.getLong("sunset") * 1000)));
            //day details
            TextView details = forecastContainerLL.getChildAt(0).findViewById(R.id.details_field);
            details.setText(weatherObj.getString("description").toUpperCase(Locale.US));
            //day temperature
            TextView temperature = forecastContainerLL.getChildAt(0).findViewById(R.id.current_temperature_field);
            Double temp = mainObj.getDouble("temp");
            temperature.setText("Temp: "+temp+"°");
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error", e);
        }
    }


    void processDarkSkyJSON (JSONObject json) {
        Log.i(TAG, "Processing DARKSKY Json");
        try {
            //take stuff from json
            //position
            cityField.setText(json.getString("timezone"));
            //update time
            JSONObject currently = json.getJSONObject("currently");
            String updated = new SimpleDateFormat("HH:mm:ss", Locale.ITALY).format(new Date(currently.getLong("time") * 1000));
            updatedField.setText("Last update: " + updated);
            //daily
            JSONObject daily = json.getJSONObject("daily");
            summaryToSpeak = daily.getString("summary");
            summaryField.setText(summaryToSpeak);
            JSONArray daysJSONArray = daily.getJSONArray("data");
            //process inflated layout
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            int size = daysJSONArray.length();
            for (int i = 0; i < size; i++) {
                JSONObject day = daysJSONArray.getJSONObject(i);
                // create dynamic LinearLayout
                if (day != null) {
                    Log.i(TAG, "Creating day "+ i);
                    //inflate day with element
                    LinearLayout clickableColumn = (LinearLayout) inflater.inflate(
                            R.layout.weather_element_layout, forecastContainerLL);
                    //put day date
                    TextView date = forecastContainerLL.getChildAt(i).findViewById(R.id.date_field);
                    if(i==0) {
                        date.setText("today");
                    } else if (i==1) {
                        date.setText("tomorrow");
                    } else {
                        date.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(new Date(day.getLong("time") * 1000)));
                    }

                    //put day icon
                    TextView icon = forecastContainerLL.getChildAt(i).findViewById(R.id.weather_icon);
                    Typeface typeface = ResourcesCompat.getFont(context, R.font.weathericons);
                    icon.setTypeface(typeface);
                    icon.setText(Html.fromHtml(setDarkSkyIcon(day.getString("icon"))));

                    //day details
                    TextView details = forecastContainerLL.getChildAt(i).findViewById(R.id.details_field);
                    details.setText(day.getString("icon"));

                    //day temperature
                    TextView temperature = forecastContainerLL.getChildAt(i).findViewById(R.id.current_temperature_field);
                    Double min = day.getDouble("temperatureMin");
                    Double max = day.getDouble("temperatureMax");
                    temperature.setText("Min "+min.intValue()+"°\nMax "+max.intValue()+"°");

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    public static String getJSONStringFromURL(String urlString) throws IOException {
        HttpURLConnection urlConnection = null;
        URL url = new URL(urlString);
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000 /* milliseconds */ );
        urlConnection.setConnectTimeout(15000 /* milliseconds */ );
        urlConnection.setDoOutput(true);
        urlConnection.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();

        String jsonString = sb.toString();
        System.out.println("JSON: " + jsonString);

        return jsonString;
    }


    private static String setOpenWeatherIcon(int actualId, long sunrise, long sunset){
        int groupid = actualId / 100;
        String icon = "";
        if(actualId == 800){ //clear
            long currentTime = new Date().getTime();
            if(currentTime>=sunrise && currentTime<sunset) {
                icon = "&#xf00d;";
            } else {
                icon = "&#xf02e;";
            }
        } else {
            switch(groupid) {
                case 2 : icon = "&#xf01e;";
                    break;
                case 3 : icon = "&#xf01c;";
                    break;
                case 7 : icon = "&#xf014;";
                    break;
                case 8 : icon = "&#xf013;";
                    break;
                case 6 : icon = "&#xf01b;";
                    break;
                case 5 : icon = "&#xf019;";
                    break;
            }
        }
        return icon;
    }

     private static String setDarkSkyIcon(String iconDarkSky){
        String icon;
        switch(iconDarkSky) {
            case "clear-day" : icon = "&#xf00d;";
                break;
            case "clear-night" : icon = "&#xf02e;";
                break;
            case "rain" : icon = "&#xf01c;";
                break;
            case "snow" : icon = "&#xf076;";
                break;
            case "sleet" : icon = "&#xf064;";
                break;
            case "wind" : icon = "&#xf050;";
                break;
            case "fog" : icon = "&#xf021;";
                break;
            case "cloudy" : icon = "&#xf013;";
                break;
            case "partly-cloudy-day" : icon = "&#xf002;";
                break;
            case "hail" : icon = "&#xf0b5;";
                break;
            case "thunderstorm" : icon = "&#xf016;";
                break;
            default: icon = "&#xf07b;";
                break;
        }
        return icon;
    }


    public interface AsyncTaskListener{
        void giveProgress(String progress, String summary);
    }

}

