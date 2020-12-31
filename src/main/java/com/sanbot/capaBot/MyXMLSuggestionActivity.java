package com.sanbot.capaBot;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.speech.Grammar;
import com.sanbot.opensdk.function.beans.speech.RecognizeTextBean;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.interfaces.speech.RecognizeListener;
import com.sanbot.opensdk.function.unit.interfaces.speech.WakenListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.sanbot.capaBot.MyUtils.concludeSpeak;
import static com.sanbot.capaBot.MyUtilsXML.createFileInXMLDirectory;
import static com.sanbot.capaBot.MyUtilsXML.getStringInside;
import static com.sanbot.capaBot.MyUtilsXML.xmlAddSuggestion;

public class MyXMLSuggestionActivity extends TopBaseActivity {

    private final static String TAG = "IGOR-XML";

    @BindView(R.id.inputText)
    EditText inputText;
    @BindView(R.id.inputName)
    EditText inputName;

    @BindView(R.id.imageListen)
    TextView imageListen;
    @BindView(R.id.wake)
    Button wakeButton;

    @BindView(R.id.exit)
    Button exitButton;

    String xmlFileName = "xml_suggestions.xml";
    File fileXML;


    private SpeechManager speechManager;    //speech
    String lastRecognizedSentence = "";


    boolean infiniteWakeup = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        register(MyShakeActivity.class);
        //screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        //set view
        setContentView(R.layout.activity_xmlsuggestion);
        ButterKnife.bind(this);
        //managers
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);

        //initialize listeners
        initListener();

        //wake button
        wakeButton.setVisibility(View.GONE);
        wakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageListen.setVisibility(View.VISIBLE);
                wakeButton.setVisibility(View.GONE);
                speechManager.doWakeUp();
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //force sleep
                infiniteWakeup = false;
                speechManager.doSleep();
                //starts dialog activity
                Intent myIntent = new Intent(MyXMLSuggestionActivity.this, MyDialogActivity.class);
                MyXMLSuggestionActivity.this.startActivity(myIntent);
                //terminates activity
                finish();
            }
        });

        //create file in the XML directory
        fileXML = createFileInXMLDirectory(xmlFileName);

        //ask and wake up
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                speechManager.startSpeak(getString(R.string.can_speak), MySettings.getSpeakDefaultOption());
                concludeSpeak(speechManager);
                speechManager.doWakeUp();
            }
        }, 200);
    }


    @OnClick({R.id.send})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.send:
                //on save click
                Log.i(TAG, "SAVE clicked");
                //controls suggestion is not empty
                if (!inputText.getText().toString().isEmpty()) {
                    //name anonymous
                    String name = getString(R.string.anonymous);
                    //if name is insert -> name updated
                    if (!inputName.getText().toString().isEmpty()) {
                        name = inputName.getText().toString();
                    }
                    //thanks the user
                    speechManager.startSpeak("Thank you", MySettings.getSpeakDefaultOption());
                    concludeSpeak(speechManager);
                    //adding the suggestion to the xml
                    xmlAddSuggestion(fileXML, name, inputText.getText().toString().toLowerCase());
                    //resetting
                    inputName.setText(getString(R.string.anonymous));
                    inputText.getText().clear();

                    //toast to tell saved
                    Toast.makeText(MyXMLSuggestionActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Suggestion saved");

                    //force sleep
                    infiniteWakeup = false;
                    speechManager.doSleep();

                    //starts dialog activity
                    Intent myIntent = new Intent(MyXMLSuggestionActivity.this, MyDialogActivity.class);
                    MyXMLSuggestionActivity.this.startActivity(myIntent);

                    //terminates activity
                    finish();
                } else {
                    //toast to ask filling suggestion
                    Toast.makeText(MyXMLSuggestionActivity.this, getString(R.string.leave_a_suggestion_or_a_comment), Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Text not present");
                }
                break;
        }
    }

    /**
     * initialize listeners
     */
    private void initListener() {
        //Set wakeup, sleep callback
        speechManager.setOnSpeechListener(new WakenListener() {
            @Override
            public void onWakeUpStatus(boolean b) {

            }

            @Override
            public void onWakeUp() {
                Log.i(TAG, "WAKEUP callback");
            }

            @Override
            public void onSleep() {
                Log.i(TAG, "SLEEP callback");
                if (infiniteWakeup) {
                    //recalling wake up to stay awake (not wake-Up-Listening() that resets the Handler)
                    speechManager.doWakeUp();
                } else {
                    //change button
                    imageListen.setVisibility(View.GONE);
                    wakeButton.setVisibility(View.VISIBLE);
                }
            }
        });
        //Speech recognition callback
        speechManager.setOnSpeechListener(new RecognizeListener() {
            @Override
            public void onRecognizeText(RecognizeTextBean recognizeTextBean) {

            }

            @Override
            public boolean onRecognizeResult(Grammar grammar) {

                //long startTime = System.nanoTime();
                lastRecognizedSentence = grammar.getText().toLowerCase();
                //IGOR: must not exceed 200ms (or less?) don't trust the documentation(500ms), I had to create an handler
                //handler so the function could return quickly true, otherwise the robot answers random things over your answers.
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        //recognized
                        inputText.append(lastRecognizedSentence + " ");
                        Log.i(TAG, ">>>>Recognized voice: "+ lastRecognizedSentence);

                    }
                });

                //Log.i(TAG, "DURATION millisec: " + (System.nanoTime() - startTime)/1000000);
                return true;
            }

            @Override
            public void onRecognizeVolume(int i) {}

            @Override
            public void onStartRecognize() {}

            @Override
            public void onStopRecognize() {}

            @Override
            public void onError(int i, int i1) {}
        });
    }

    @Override
    protected void onMainServiceConnected() {
    }


    private String xmlAllTest() {
        String xmlstrResult = "failed";
        //WRITE
        try {
            Log.i(TAG, "Testing WRITE");
            fileXML.createNewFile();
            FileOutputStream fileos = new FileOutputStream(fileXML);
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.startTag(null, "userData");

            boolean exampleCreatingXML = true;
            if (exampleCreatingXML) {
                xmlSerializer.startTag(null, "suggestion");

                xmlSerializer.startTag(null, "name");
                xmlSerializer.text("name example");
                xmlSerializer.endTag(null, "name");

                xmlSerializer.startTag(null, "text");
                xmlSerializer.text("suggestion text example");
                xmlSerializer.endTag(null, "text");

                xmlSerializer.endTag(null, "suggestion");
            }

            xmlSerializer.endTag(null, "userData");
            xmlSerializer.endDocument();
            xmlSerializer.flush();
            String dataWrite = writer.toString();
            fileos.write(dataWrite.getBytes());
            fileos.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //ADD
        try {
            Log.i(TAG, "Testing ADD");
            InputStream inpstr = new FileInputStream(fileXML);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inpstr);
            Element root = doc.getDocumentElement();
            // new elements
            Element newElement = doc.createElement("suggestion_test");

            newElement.appendChild(doc.createTextNode("adding test ok "));
            //element inside with string inside
            /*
            Element name = document.createElement("name");
            name.appendChild(document.createTextNode("nmoeserver"));
            newElement.appendChild(name);
            */

            //append new
            root.appendChild(newElement);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StreamResult result = new StreamResult(fileXML);
            transformer.transform(source, result);


        } catch ( Exception e) {
                    e.printStackTrace();
        }
        //READ
        try {
            Log.i(TAG, "Testing READ");
            InputStream inpstr = new FileInputStream(fileXML);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inpstr);

            //gets the root element
            Element elementRoot=doc.getDocumentElement();
            elementRoot.normalize();

            /*
            //list of evaluations
            NodeList nList = doc.getElementsByTagName("userData");

            //cycling all the sub elements
            for (int i=0; i<nList.getLength(); i++) {
                //node is a list element
                Node node = nList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element2 = (Element) node;
                    xmlstrResult = "suggestion : " + getValue("suggestion", element2);
                }
            }
            */

            //list of evaluations
            NodeList nList = doc.getElementsByTagName("suggestion_test");
            //cycling all the sub elements
            for (int i=0; i<nList.getLength(); i++) {
                //node is current node in list
                Node node = nList.item(i);
                //if is an element
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    //take the value from this element
                    xmlstrResult = "suggestion : " + getStringInside(element);
                }
            }

        } catch (Exception e) {e.printStackTrace();}

        return xmlstrResult;
    }





}
