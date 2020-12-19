package com.sanbot.capaBot;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class MyCalendarDownloadAsyncTask extends AsyncTask< String, Void, String > {


    private AsyncTaskListener listener;

    InputStream is;
    String urlCalendar;

    MyCalendarDownloadAsyncTask(Context cont) {
        //registration of the listener to call when ready
        listener = (MyCalendarDownloadAsyncTask.AsyncTaskListener)cont;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }


    protected String doInBackground(String... args) {
        urlCalendar = args[0];
        Log.i("IGOR-CAL-ASYNC", "request Calendar:" + urlCalendar);
        Calendar calendar;
        //open stream
        try {
            is = new URL(urlCalendar).openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //build
        try {
            calendar = new CalendarBuilder().build(is);
            //Log.i("IGOR-CAL-ASYNC", "calendar RECEIVED:" +  calendar.toString());
            //give calendar to all the classes that implement AsyncTaskListener
            listener.giveCalendar(calendar);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        }
        //close
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }




    public interface AsyncTaskListener{
        void giveCalendar(Calendar calendar);
    }

}

