package com.sanbot.capaBot;

import android.content.Intent;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alamkanak.weekview.DateTimeInterpreter;
import com.alamkanak.weekview.MonthLoader;
import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.EmotionsType;
import com.sanbot.opensdk.function.beans.speech.Grammar;
import com.sanbot.opensdk.function.beans.speech.RecognizeTextBean;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.interfaces.speech.RecognizeListener;
import com.sanbot.opensdk.function.unit.interfaces.speech.WakenListener;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.util.MapTimeZoneCache;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.sanbot.capaBot.MyUtils.concludeSpeak;
import static com.sanbot.capaBot.MyWeatherActivity.isNetworkAvailable;

public class MyCalendarActivity extends TopBaseActivity implements MyCalendarDownloadAsyncTask.AsyncTaskListener{

    private final static String TAG = "IGOR-CAL";

    @BindView(R.id.exit)
    Button exitButton;

    @BindView(R.id.today)
    Button todayButton;

    @BindView(R.id.weekView)
    WeekView mWeekView;

    @BindView(R.id.loader_cal)
    ProgressBar loader_cal;

    @BindView(R.id.text_loader_cal)
    TextView text_loader_cal;

    private SpeechManager speechManager; //voice, speechRec
    private SystemManager systemManager; //emotions

    //calendars to show
    String[] urlsCalendar = {
            "https://ics.teamup.com/feed/ksdxka67t86rufdom3/0.ics",
           /* "https://calendar.google.com/calendar/ical/it.portuguese%23holiday%40group.v.calendar.google.com/public/basic.ics"*/
    };

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd", Locale.ITALY);
    private static final SimpleDateFormat SDFH = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ITALY);

    String lastRecognizedSentence = "";

    boolean infiniteWakeup = true;
    boolean shortDate = false;
    int finishedThreadsCount = 0;

    List<WeekViewEvent> events = new ArrayList<WeekViewEvent>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(MyCalendarActivity.class);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_calendar);

        ButterKnife.bind(this);
        //initialize managers
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);

        //for the iCal4J
        System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache.class.getName());

        //view
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToDialogAndExit(true);
            }
        });
        todayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWeekView.goToToday();
            }
        });
        mWeekView.setVisibility(View.GONE);
        text_loader_cal.setText("Loading 1/" + urlsCalendar.length );

        //robot listeners
        initListeners();

        //load the task to download the calendars
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                taskLoadUp();
            }
        }, 100);

        //fill with fake events to test
        //events = getFakeEvents(2019, 7);

        MonthLoader.MonthChangeListener mMonthChangeListener = new MonthLoader.MonthChangeListener() {
            @Override
            public List<WeekViewEvent> onMonthChange(int newYear, int newMonth) {
                Log.i(TAG, "onMonthChange called "+newMonth+" "+newYear);/*
                ArrayList<WeekViewEvent> eventsMonth = new ArrayList<WeekViewEvent>();
                //put in the list only the events of the month required
                for (int i = 0; i < events.size(); i++) {
                    if (events.get(i).getStartTime().get(java.util.Calendar.MONTH) == newMonth) {
                        eventsMonth.add(events.get(i));
                    }
                    if ((events.get(i).getStartTime().get(java.util.Calendar.MONTH)-1) == newMonth) {
                        eventsMonth.add(events.get(i));
                    }
                }*/
                //todo test
                java.util.Calendar today = java.util.Calendar.getInstance();
                today.setTime(new Date());

                if(today.get(java.util.Calendar.MONTH) != newMonth)
                    return new ArrayList<>();

                return events;
            }
        };

        // The week view has infinite scrolling horizontally. We have to provide the events of a
        // month every time the month changes on the week view.
        mWeekView.setMonthChangeListener(mMonthChangeListener);

        // Show a toast message about the touched event.
        mWeekView.setOnEventClickListener(new WeekView.EventClickListener() {
            @Override
            public void onEventClick(WeekViewEvent event, RectF eventRect) {
                Toast.makeText(MyCalendarActivity.this, "Clicked " + event.getName(), Toast.LENGTH_SHORT).show();
                speechManager.startSpeak(event.getName()+ " starts at " + getTimeString(event.getStartTime()), MySettings.getSpeakDefaultOption());
            }
        });

        // Set long press listener for events.
        mWeekView.setEventLongPressListener(new WeekView.EventLongPressListener() {
            @Override
            public void onEventLongPress(WeekViewEvent event, RectF eventRect) {
                Toast.makeText(MyCalendarActivity.this, "Long pressed event: " + event.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        // Set long press listener for empty view
        mWeekView.setEmptyViewLongPressListener(new WeekView.EmptyViewLongPressListener() {
            @Override
            public void onEmptyViewLongPress(java.util.Calendar time) {
                Toast.makeText(MyCalendarActivity.this, "Empty view long-pressed: " + getTimeString(time), Toast.LENGTH_SHORT).show();
                speechManager.startSpeak("there is nothing on " + getTimeString(time) , MySettings.getSpeakDefaultOption());
            }

        });

        mWeekView.setEmptyViewClickListener(new WeekView.EmptyViewClickListener() {
            @Override
            public void onEmptyViewClicked(java.util.Calendar time) {
                Toast.makeText(MyCalendarActivity.this, "Empty view clicked: " + getTimeString(time), Toast.LENGTH_SHORT).show();
            }
        });

        mWeekView.setDateTimeInterpreter(new DateTimeInterpreter() {
            @Override
            public String interpretDate(java.util.Calendar date) {
                //week day
                SimpleDateFormat weekdayNameFormat = new SimpleDateFormat("EEE", Locale.getDefault());
                String weekday = weekdayNameFormat.format(date.getTime());
                //format date
                SimpleDateFormat format = new SimpleDateFormat(" d/M", Locale.getDefault());
                if (shortDate)
                    weekday = String.valueOf(weekday.charAt(0));
                //united strings
                return weekday.toUpperCase() + format.format(date.getTime());
            }

            @Override
            public String interpretTime(int hour) {
                return hour > 11 ? (hour - 12) + " PM" : (hour == 0 ? "12 AM" : hour + " AM");
            }
        });
    //end onCreate()
    }

    protected String getTimeString(java.util.Calendar time) {
        return String.format("%02d:%02d of %d/%s", time.get(java.util.Calendar.HOUR_OF_DAY), time.get(java.util.Calendar.MINUTE), time.get(java.util.Calendar.DAY_OF_MONTH), time.get(java.util.Calendar.MONTH)+1);
    }

    private List<WeekViewEvent> getFakeEvents(int newYear, int newMonth) {
        // Populate the week view with some events.
        List<WeekViewEvent> events = new ArrayList<WeekViewEvent>();

        java.util.Calendar startTime = java.util.Calendar.getInstance();
        startTime.set(java.util.Calendar.HOUR_OF_DAY, 3);
        startTime.set(java.util.Calendar.MINUTE, 0);
        startTime.set(java.util.Calendar.MONTH, newMonth-1);
        startTime.set(java.util.Calendar.YEAR, newYear);
        java.util.Calendar endTime = (java.util.Calendar) startTime.clone();
        endTime.add(java.util.Calendar.HOUR, 1);
        endTime.set(java.util.Calendar.MONTH, newMonth-1);
        WeekViewEvent event = new WeekViewEvent(1, "demo-event 1", startTime, endTime);
        event.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        events.add(event);

        startTime = java.util.Calendar.getInstance();
        startTime.set(java.util.Calendar.HOUR_OF_DAY, 3);
        startTime.set(java.util.Calendar.MINUTE, 30);
        startTime.set(java.util.Calendar.MONTH, newMonth-1);
        startTime.set(java.util.Calendar.YEAR, newYear);
        endTime = (java.util.Calendar) startTime.clone();
        endTime.set(java.util.Calendar.HOUR_OF_DAY, 4);
        endTime.set(java.util.Calendar.MINUTE, 30);
        endTime.set(java.util.Calendar.MONTH, newMonth-1);
        event = new WeekViewEvent(10, "demo-event 2", startTime, endTime);
        events.add(event);

        startTime = java.util.Calendar.getInstance();
        startTime.set(java.util.Calendar.HOUR_OF_DAY, 4);
        startTime.set(java.util.Calendar.MINUTE, 20);
        startTime.set(java.util.Calendar.MONTH, newMonth-1);
        startTime.set(java.util.Calendar.YEAR, newYear);
        endTime = (java.util.Calendar) startTime.clone();
        endTime.set(java.util.Calendar.HOUR_OF_DAY, 5);
        endTime.set(java.util.Calendar.MINUTE, 0);
        event = new WeekViewEvent(10, "demo-event 3", startTime, endTime);
        events.add(event);

        startTime = java.util.Calendar.getInstance();
        startTime.set(java.util.Calendar.HOUR_OF_DAY, 5);
        startTime.set(java.util.Calendar.MINUTE, 30);
        startTime.set(java.util.Calendar.MONTH, newMonth-1);
        startTime.set(java.util.Calendar.YEAR, newYear);
        endTime = (java.util.Calendar) startTime.clone();
        endTime.add(java.util.Calendar.HOUR_OF_DAY, 2);
        endTime.set(java.util.Calendar.MONTH, newMonth-1);
        event = new WeekViewEvent(2, "demo-event 4", startTime, endTime);
        events.add(event);

        startTime = java.util.Calendar.getInstance();
        startTime.set(java.util.Calendar.HOUR_OF_DAY, 5);
        startTime.set(java.util.Calendar.MINUTE, 0);
        startTime.set(java.util.Calendar.MONTH, newMonth-1);
        startTime.set(java.util.Calendar.YEAR, newYear);
        startTime.add(java.util.Calendar.DATE, 1);
        endTime = (java.util.Calendar) startTime.clone();
        endTime.add(java.util.Calendar.HOUR_OF_DAY, 3);
        endTime.set(java.util.Calendar.MONTH, newMonth - 1);
        event = new WeekViewEvent(3, "demo-event 5", startTime, endTime);
        events.add(event);

        return events;
    }

    @Override
    protected void onMainServiceConnected() {}


    void initListeners() {
        //Set wakeup, sleep callback
        speechManager.setOnSpeechListener(new WakenListener() {
            @Override
            public void onWakeUpStatus(boolean b) {
            }

            @Override
            public void onWakeUp() {
                //Log.i(TAG, "WAKE UP callback");
            }

            @Override
            public void onSleep() {
                //Log.i(TAG, "SLEEP callback");
                if (infiniteWakeup) {
                    //recalling wake up to stay awake (not wake-Up-Listening() that resets the Handler)
                    speechManager.doWakeUp();
                }
            }
        });
        //voice listener
        speechManager.setOnSpeechListener(new RecognizeListener() {
            @Override
            public void onRecognizeText(@NonNull RecognizeTextBean recognizeTextBean) {

            }

            @Override
            public boolean onRecognizeResult(@NonNull Grammar grammar) {
                lastRecognizedSentence = Objects.requireNonNull(grammar.getText()).toLowerCase();
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (lastRecognizedSentence.contains("today") ) {
                            speechManager.startSpeak("These are the events of today", MySettings.getSpeakDefaultOption());
                            systemManager.showEmotion(EmotionsType.SMILE);
                            mWeekView.goToToday();
                        }
                        if (lastRecognizedSentence.contains("ok") ||lastRecognizedSentence.contains("yes") ||lastRecognizedSentence.contains("i am")||lastRecognizedSentence.contains("we are")
                                ||lastRecognizedSentence.contains("sure") ||lastRecognizedSentence.contains("of course") ||lastRecognizedSentence.contains("thank")) {
                            if (lastRecognizedSentence.contains("thank")) {
                                //thanks
                                speechManager.startSpeak("thank you", MySettings.getSpeakDefaultOption());
                                concludeSpeak(speechManager);
                            }
                            //happy
                            systemManager.showEmotion(EmotionsType.KISS);
                            speechManager.startSpeak("I'm happy to hear about it", MySettings.getSpeakDefaultOption());
                            boolean res = concludeSpeak(speechManager);
                            //finish
                            goToDialogAndExit(res);
                        }
                        if (lastRecognizedSentence.equals("no") ||lastRecognizedSentence.contains("so and so") ) {
                            //sad
                            speechManager.startSpeak("I'm sad to hear this", MySettings.getSpeakDefaultOption());
                            systemManager.showEmotion(EmotionsType.GOODBYE);
                            boolean res = concludeSpeak(speechManager);
                            //finish
                            goToDialogAndExit(res);
                        }
                        if (lastRecognizedSentence.contains("exit") ) {
                            speechManager.startSpeak("OK", MySettings.getSpeakDefaultOption());
                            systemManager.showEmotion(EmotionsType.SMILE);
                            boolean res = concludeSpeak(speechManager);
                            //finish
                            goToDialogAndExit(res);
                        }
                    }
                });

                return true;
            }

            @Override
            public void onRecognizeVolume(int i) {
            }

            @Override
            public void onStartRecognize() {

            }

            @Override
            public void onStopRecognize() {

            }

            @Override
            public void onError(int i, int i1) {

            }
        });

    }


    public void taskLoadUp() {
        if (isNetworkAvailable(getApplicationContext())) {
            Log.i(TAG, "network ok");
            //for every url
            for (String anUrlsCalendar : urlsCalendar) {
                //launch a task
                MyCalendarDownloadAsyncTask task = new MyCalendarDownloadAsyncTask(this);
                task.execute(anUrlsCalendar);
            }
        } else {
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void giveCalendar(Calendar calendar) {
        //calendar passed
        Log.i(TAG,"calendar number components: " + calendar.getComponents().size());
        //for every event
        for (Object o : calendar.getComponents()) {
            Component component = (Component) o;
            Date startDate;
            Date endDate;
            //grab summary string
            String summary = "Not specified";
            if (component.getProperties("SUMMARY").size()>0) {
                //summary event
                summary = component.getProperties("SUMMARY").get(0).getValue();
            }
            //Log.i(TAG, "-component>>>" + summary);
            try {
                //if has a start time
                if (component.getProperties("DTSTART").size()>0) {
                    String start_str = null, end_str = null;
                    //iterator gets start-time String
                    for (Iterator j = component.getProperties("DTSTART").iterator(); j.hasNext();) {
                        Property property = (Property) j.next();
                        start_str = property.getValue();
                        //Log.i(TAG, "Property DTSTART [" + property.getName() + " <<>> " + property.getValue() + "]");
                    }
                    try {
                        //parsing the string to date with hour
                        startDate = SDFH.parse(start_str);
                        //Log.i(TAG, "Property DTSTART parsed with hour: " + startDate);
                    } catch (ParseException e) {
                        //parsing the string to date
                        startDate = SDF.parse(start_str);
                        //Log.i(TAG, "Property DTSTART parsed only day: " + startDate);
                    }
                    java.util.Calendar startCal = java.util.Calendar.getInstance();
                    startCal.setTime(startDate);

                    //end time part
                    java.util.Calendar endCal;
                    //if it has the end time
                    if (component.getProperties("DTEND").size()>0) {
                        //iterator gets end-time String
                        for (Iterator j = component.getProperties("DTEND").iterator(); j.hasNext();) {
                            Property property = (Property) j.next();
                            end_str = property.getValue();
                            //Log.i(TAG, "Property DTEND   [" + property.getName() + " <<>> " + property.getValue() + "]");
                        }
                        try {
                            //parsing the string to date with hour
                            endDate = SDFH.parse(end_str);
                            //Log.i(TAG, "Property DTEND parsed with hour: " + endDate);
                            endCal = java.util.Calendar.getInstance();
                            endCal.setTime(endDate);
                        } catch (ParseException e) {
                            //parsing the string to date
                            endDate = SDF.parse(end_str);
                            //Log.i(TAG, "Property DTSTART parsed only day: " + endDate);
                            endCal = java.util.Calendar.getInstance();
                            endCal.setTime(endDate);
                            //if the end day is the same I put the end hour at 24
                            if (startCal.get(java.util.Calendar.DAY_OF_MONTH) == endCal.get(java.util.Calendar.DAY_OF_MONTH )) {
                                endCal.set(java.util.Calendar.HOUR_OF_DAY, 24);
                            }
                        }

                    } else {
                        //Log.i(TAG, "Property DTEND NOT FOUND");
                        //end time at the end of the same day
                        endCal = (java.util.Calendar) startCal.clone();
                        endCal.set(java.util.Calendar.HOUR_OF_DAY, 24);
                    }

                    //create event
                    WeekViewEvent event = new WeekViewEvent(1, summary, startCal, endCal);
                    //todo put color depending on the events
                    //event.setColor(getResources().getColor(R.color.colorPrimary, null));
                    //put the event in the list
                    events.add(event);
                    //Log.i(TAG, "added: " + startDate + ">->" + endDate + "  Summary: " + summary);
                } else{
                    //no start-time not added
                    Log.e(TAG, "NOT ADDED (DTSTART not found) component details:" + component.toString());
                }
            } catch (ParseException e) {
                e.printStackTrace();
                Log.e(TAG, "PARSE EXCEPTION component details:" + component.toString());
            }
        }
        //finished this thread
        finishedThreadsCount++;
        Log.i(TAG, "FINISHED THREAD " + finishedThreadsCount);

        //notify update to UI
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //if last thread finished
                if (finishedThreadsCount == urlsCalendar.length) {
                    //update ui
                    mWeekView.notifyDatasetChanged();
                    //go to 8 am today
                    mWeekView.goToToday();
                    mWeekView.goToHour(8);
                    //visible
                    mWeekView.setVisibility(View.VISIBLE);
                    loader_cal.setVisibility(View.GONE);
                    text_loader_cal.setVisibility(View.GONE);
                    //new thread not to lock the UI with the sleep
                    new Thread(new Runnable() {
                        public void run() {
                            speechManager.startSpeak("These are the events of the ISR", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            speechManager.startSpeak("Are you satisfied?", MySettings.getSpeakDefaultOption());
                            concludeSpeak(speechManager);
                            speechManager.doWakeUp();
                        }
                    }).start();
                } else {
                    //update loading
                    text_loader_cal.setText("Loading "+ (finishedThreadsCount + 1) +"/" + urlsCalendar.length );
                }
            }
        });


    }


    private void goToDialogAndExit(boolean result) {
        if(result) {
            //force sleep
            infiniteWakeup = false;
            speechManager.doSleep();
            //starts dialog activity
            Intent myIntent = new Intent(MyCalendarActivity.this, MyDialogActivity.class);
            MyCalendarActivity.this.startActivity(myIntent);
            //finish
            finish();
        }
    }


}
