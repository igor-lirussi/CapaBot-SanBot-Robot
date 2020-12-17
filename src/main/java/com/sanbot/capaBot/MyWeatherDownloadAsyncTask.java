package com.sanbot.capaBot;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static com.sanbot.capaBot.MyWeatherActivity.cityField;
import static com.sanbot.capaBot.MyWeatherActivity.summaryField;
import static com.sanbot.capaBot.MyWeatherActivity.loader;
import static com.sanbot.capaBot.MyWeatherActivity.updatedField;

public class MyWeatherDownloadAsyncTask extends AsyncTask< String, Void, String > {


    private static String OPEN_WEATHER_MAP_API = "ebef12b878c57f10161c411d4620ce5e";
    private static String DARKSKY_API = "9c096d766f224ca50a27b01941c69c02";
    private static String latitude = "38.737231";
    private static String longitude = "-9.138742";
    private static LinearLayout forecastContainerLL;
    private static Context context;

    private AsyncTaskListener listener;
    String weeklySummary = "no weekly summary available";

    MyWeatherDownloadAsyncTask(Context cont, LinearLayout containerLinearLayout) {
        context = cont;
        listener = (AsyncTaskListener)cont;
        forecastContainerLL = containerLinearLayout;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        loader.setVisibility(View.VISIBLE);
    }


    protected String doInBackground(String... args) {
        String request = "https://api.darksky.net/forecast/"+DARKSKY_API+"/"+latitude+","+longitude+"?units=si";
        //String request = "http://api.openweathermap.org/data/2.5/weather?q=" + args[0] + "&units=metric&appid=" + OPEN_WEATHER_MAP_API;
        Log.i("IGORWEATHER", "request:"+request);
        String xml = null;
        try {
            xml = getJSONStringFromURL(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i("IGORWEATHER", "XML RECEIVED:"+xml);
        return xml;
    }

    @Override
    protected void onPostExecute(String xml) {
        Log.i("IGORWEATHER", "XML PROCESSED:"+xml);
        try {
            if (xml != null) {
                JSONObject json = new JSONObject(xml);

                //processOpenWeatherJSON(json);
                processDarkSkyJSON(json);

                loader.setVisibility(View.GONE);

                listener.giveProgress("OK", weeklySummary);
            } else {
                Log.i("IGORWEATHER", "Error, xml null");
                Toast.makeText(context, "Error, xml null", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Toast.makeText(context, "Error, Check City", Toast.LENGTH_SHORT).show();
        } catch (NullPointerException npe) {
            Toast.makeText(context, "Error, data not available", Toast.LENGTH_SHORT).show();
        }


    }

    void processDarkSkyJSON (JSONObject json) {
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
            weeklySummary = daily.getString("summary");
            summaryField.setText(weeklySummary);
            JSONArray daysJSONArray = daily.getJSONArray("data");
            //todo put the alerts array
            //process
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            int size = daysJSONArray.length();
            for (int i = 0; i < size; i++) {
                JSONObject day = daysJSONArray.getJSONObject(i);
                // create dynamic LinearLayout
                if (day != null) {
                    Log.i("IGORWEATHER", "Creating day "+ i);
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

    void processOpenWeatherJSON(JSONObject json) {
        try{
            JSONObject details = json.getJSONArray("weather").getJSONObject(0);
            JSONObject main = json.getJSONObject("main");
            DateFormat df = DateFormat.getDateTimeInstance();

            cityField.setText(json.getString("name").toUpperCase(Locale.US) + ", " + json.getJSONObject("sys").getString("country"));
            summaryField.setText(details.getString("description").toUpperCase(Locale.US));
            updatedField.setText(df.format(new Date(json.getLong("dt") * 1000)));
            /*weatherIcon.setText(Html.fromHtml(setWeatherIcon(details.getInt("id"),
                    json.getJSONObject("sys").getLong("sunrise") * 1000,
                    json.getJSONObject("sys").getLong("sunset") * 1000)));*/
        } catch (JSONException e) {
        //Toast.makeText(getApplicationContext(), "Error, Check City", Toast.LENGTH_SHORT).show();
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


    private static String setWeatherIcon(int actualId, long sunrise, long sunset){
        int id = actualId / 100;
        String icon = "";
        if(actualId == 800){
            long currentTime = new Date().getTime();
            if(currentTime>=sunrise && currentTime<sunset) {
                icon = "&#xf00d;";
            } else {
                icon = "&#xf02e;";
            }
        } else {
            switch(id) {
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

