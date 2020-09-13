package com.berkd.apricot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.*;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.DateFormat;
import java.util.Calendar;

import javax.security.auth.callback.Callback;

public class MainActivity extends AppCompatActivity {

    /**
     * DEFINE VARIABLES HERE
     */
    String[] units = {"Celsius", "Fahrenheit"};
    int set = 0;
    private final Handler mHandler = new Handler();
    private Runnable mTimer;
    private double graphLastXValue = 5d;
    private LineGraphSeries<DataPoint> mSeries;
    private TextView graphType; // used to store the amplitude OR frequency type.
    private int myDataPoints = 50; // used to set the precision of the amplitude accuracy.

    double co2Count = Math.random()*1000;
    double humidCount = 20;
    int simState = 0;
    int unitState = 0; // 0 = celsius, 1 = fahrenheit


    DatabaseHelper myDB;


    // set text values
    private TextView co2Reading;
    private TextView tempReading;
    private TextView humidReading;
    private TextView ventilationStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        co2Reading = findViewById(R.id.textCo2Val);
        tempReading = findViewById(R.id.textTempVal);
        humidReading = findViewById(R.id.textHumidtyVal);
        ventilationStatus = findViewById(R.id.textVentilationStatus);

        aboutPopup();
        buttonConnection();         // handle the "connection status" events
        buttonViewLogs();           // handle the "view logs" events
        buttonToggleLogging();      // handle the "toggle mode" events
        buttonProgramMode();        // handle the "program mode" events

        updateCo2Val();
        updateHumidity();
        //updateTemperature();
        //updateVentilation();


        GraphView graph = (GraphView) findViewById(R.id.graph);
        initGraph(graph);

    }

    public void initGraph(GraphView graph) {
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(10);

        graph.getGridLabelRenderer().setLabelVerticalWidth(100);

        // first mSeries is a line
        mSeries = new LineGraphSeries<>();
        mSeries.setDrawDataPoints(true);
        mSeries.setDrawBackground(true);
        graph.addSeries(mSeries);
    }

    @Override
    public void onResume() {

        runGraph();
        super.onResume();
    }

    @Override
    public void onPause() {
        pauseGraph();
        super.onPause();
    }


    public void runGraph() {
        mTimer = new Runnable() {
            @Override
            public void run() {
                double simVal = 0;
                switch(simState){
                    case 0:
                        // do basic randomness
                        simVal = Math.random()*1000;
                        break;
                    case 1:
                        // simulate a bedroom with door closed then opened after x amount of time
                        simVal = 5;
                        break;
                    case 2:
                        // simulate dangerous CO2 levels (i.e. smoky)
                        simVal = Math.random()*2500;
                        break;

                }
                final String theVal = String.valueOf(simVal); //  make it usable for textView
                graphLastXValue += 0.25d;
                mSeries.appendData(new DataPoint(graphLastXValue, simVal), true, myDataPoints);
                mHandler.postDelayed(this, 1000);
                co2Count = simVal;



            }
        };

        mHandler.postDelayed(mTimer, 50);
    }

    public void pauseGraph() {
        mHandler.removeCallbacks(mTimer); // pause the graph
    }


    /**
     * UPDATE CO2 VALUES
     *
     *
     */
    public void updateCo2Val() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()){
                    try {
                        Thread.sleep(1000);

                        runOnUiThread(new Runnable() {
                            String theCo2Vals;
                            @Override
                            public void run() {
                                theCo2Vals = String.format("%.2f", co2Count);

                                co2Reading.setText(String.valueOf(theCo2Vals) + " ppm");

                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        thread.start();
    }

    public void updateHumidity() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()){
                    try {
                        Thread.sleep(5000);

                        runOnUiThread(new Runnable() {
                            String theHumidVals;
                            @Override
                            public void run() {
                                theHumidVals = String.format("%.2f", humidCount);
                                humidCount = humidCount + 0.5;
                                humidReading.setText(String.valueOf(theHumidVals) + " %");
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        thread.start();
    }

    /**
     * ESSENTIAL FOR MENU CREATION!
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        return true;
    }

    /** /////////////////////// M E N U ////////////////////////
     * WHEN OPTIONS MENU IS SELECT, CURRENTLY DOES NOT MUCH BUT WIP
     * TODO -> Get these functions things implemented (CURRENTLY WIP)
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

            // help popup
            case R.id.item1:
                helpPopup();
                break;

            case R.id.item2:
                settingsPopup();
                break;
            case R.id.item3:
                Toast.makeText(this, "Dark mode coming soon!", Toast.LENGTH_SHORT).show();

                break;
            case R.id.item4:
                aboutPopup();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * ABOUT POPUP
     *  About the program and whatnot.
     */
    public void aboutPopup() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.about_popup, null);
        mBuilder.setTitle("About");

        mBuilder.setNeutralButton("PROJECT PAGE", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Uri projectPage = Uri.parse("https://www.github.com/berkiyo/apricot");
                Intent intent = new Intent(Intent.ACTION_VIEW, projectPage);
                startActivity(intent);
            }
        });

        mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();
        dialog.show();
    }


    /**
     * SETTINGS POPUP
     */
    public void settingsPopup() {

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.settings_popup, null);
        mBuilder.setTitle("Settings");

        final Spinner mSpinner = mView.findViewById(R.id.unit_spinner);

        final ArrayAdapter<String> sAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.units));

        sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(sAdapter);

        // Load the previously saved font selection!
        mSpinner.setSelection(loadSpinnerState("settings"));
        //

        mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if(mSpinner.getSelectedItem().toString().equalsIgnoreCase("celsius")) {
                    Toast.makeText(MainActivity.this, "Celsius set", Toast.LENGTH_SHORT).show();
                    unitState = 0;
                    saveSpinnerState(0, "settings");
                    dialog.dismiss();
                }
                if(mSpinner.getSelectedItem().toString().equalsIgnoreCase("fahrenheit")) {
                    Toast.makeText(MainActivity.this, "Fahrenheit Set", Toast.LENGTH_SHORT).show();
                    unitState = 1;
                    saveSpinnerState(1, "settings");
                    dialog.dismiss();
                }
            }
        });

        mBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();
        dialog.show();
    }


    /**
     * SAVE_SPINNER_STATE
     *  Store the position of the spinner
     */
    public void saveSpinnerState(int userChoice, String dataName) {
        SharedPreferences sharedPref = getSharedPreferences(dataName,0);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putInt("userChoiceSpinner", userChoice);
        prefEditor.commit();
    }

    /**
     * LOAD_SPINNER_STATE
     *  Return the value of the previously selected font size, return integer array value
     */
    public int loadSpinnerState(String dataName) {
        SharedPreferences sharedPref = getSharedPreferences(dataName, MODE_PRIVATE);
        int spinnerValue = sharedPref.getInt("userChoiceSpinner",-1);
        return spinnerValue;
    }

    private void buttonConnection() {
        Button buttonInsert = findViewById(R.id.buttonConnection);
        buttonInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(MainActivity.this, "Clicked! Functionality coming soon.", Toast.LENGTH_SHORT).show();

            }
        });
    }


    private void buttonViewLogs() {
        Button buttonInsert = findViewById(R.id.buttonLogs);
        buttonInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DatabaseActivity.class);
                startActivity(intent);
            }
        });
    }

    private void buttonToggleLogging() {
        Button buttonInsert = findViewById(R.id.buttonStartLogging);
        buttonInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                View mView = getLayoutInflater().inflate(R.layout.logging_popup, null);
                final Spinner mSpinner = mView.findViewById(R.id.spinnerColour);
                mBuilder.setTitle("Graph Settings");

                final ArrayAdapter<String> sAdapter = new ArrayAdapter<String>(MainActivity.this,
                        android.R.layout.simple_spinner_item,
                        getResources().getStringArray(R.array.colours));

                sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mSpinner.setAdapter(sAdapter);

                // Load the previously saved font selection!
                mSpinner.setSelection(loadSpinnerState("colour"));
                //

                mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if(mSpinner.getSelectedItem().toString().equalsIgnoreCase("blue")) {
                            Toast.makeText(MainActivity.this, "Blue set", Toast.LENGTH_SHORT).show();
                            mSeries.setColor(Color.BLUE);
                            saveSpinnerState(0, "colour");
                            dialog.dismiss();
                        }
                        if(mSpinner.getSelectedItem().toString().equalsIgnoreCase("red")) {
                            Toast.makeText(MainActivity.this, "Red Set", Toast.LENGTH_SHORT).show();
                            mSeries.setColor(Color.RED);
                            saveSpinnerState(1,"colour");
                            dialog.dismiss();
                        }
                        if(mSpinner.getSelectedItem().toString().equalsIgnoreCase("green")) {
                            Toast.makeText(MainActivity.this, "Green Set", Toast.LENGTH_SHORT).show();
                            mSeries.setColor(Color.GREEN);
                            saveSpinnerState(2, "colour");
                            dialog.dismiss();
                        }
                        if(mSpinner.getSelectedItem().toString().equalsIgnoreCase("yellow")) {
                            Toast.makeText(MainActivity.this, "Yellow Set", Toast.LENGTH_SHORT).show();
                            mSeries.setColor(Color.YELLOW);
                            saveSpinnerState(3, "colour");
                            dialog.dismiss();
                        }
                        if(mSpinner.getSelectedItem().toString().equalsIgnoreCase("black")) {
                            Toast.makeText(MainActivity.this, "Black Set", Toast.LENGTH_SHORT).show();
                            mSeries.setColor(Color.BLACK);
                            saveSpinnerState(4, "colour");
                            dialog.dismiss();
                        }
                        if(mSpinner.getSelectedItem().toString().equalsIgnoreCase("magenta")) {
                            Toast.makeText(MainActivity.this, "Black Set", Toast.LENGTH_SHORT).show();
                            mSeries.setColor(Color.MAGENTA);
                            saveSpinnerState(5, "colour");
                            dialog.dismiss();
                        }
                    }
                });

                mBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                mBuilder.setView(mView);
                AlertDialog dialog = mBuilder.create();
                dialog.show();

            }
        });
    }

    public void buttonProgramMode() {
        Button btnProgramMode = findViewById(R.id.buttonProgramMode);
        btnProgramMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                View mView = getLayoutInflater().inflate(R.layout.mode_popup, null);
                final Spinner mSpinner = mView.findViewById(R.id.spinnerSimModes);
                mBuilder.setTitle("Program Mode");

                final ArrayAdapter<String> sAdapter = new ArrayAdapter<String>(MainActivity.this,
                        android.R.layout.simple_spinner_item,
                        getResources().getStringArray(R.array.modes));

                sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mSpinner.setAdapter(sAdapter);

                mSpinner.setSelection(loadSpinnerState("mode"));

                mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(mSpinner.getSelectedItem().toString().equalsIgnoreCase("random")) {
                            Toast.makeText(MainActivity.this, "Random simulation set", Toast.LENGTH_SHORT).show();
                            simState = 0;
                            saveSpinnerState(0, "mode");
                            dialog.dismiss();
                        }
                        if(mSpinner.getSelectedItem().toString().equalsIgnoreCase("bedroom")) {
                            Toast.makeText(MainActivity.this, "Bedroom simulation set", Toast.LENGTH_SHORT).show();
                            simState = 1;
                            saveSpinnerState(1, "mode");
                            dialog.dismiss();
                        }
                        if(mSpinner.getSelectedItem().toString().equalsIgnoreCase("Smoky")) {
                            Toast.makeText(MainActivity.this, "Smoky simulation set", Toast.LENGTH_SHORT).show();
                            simState = 2;
                            saveSpinnerState(2, "mode");
                            dialog.dismiss();
                        }
                    }
                });
                mBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                mBuilder.setView(mView);
                AlertDialog dialog = mBuilder.create();
                dialog.show();

            }
        });
    }

    public void helpPopup() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.help_popup, null);
        mBuilder.setTitle("Help");

        mBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();
        dialog.show();

    }

    /**
     * DATABASE STUFF HERE
     */

    public void addData(String newEntry) {

        boolean insertData = myDB.addData(newEntry);

        if(insertData == true){
            Toast.makeText(this, "Data Successfully Inserted!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Something went wrong :(.", Toast.LENGTH_LONG).show();
        }
    }

/*
    public void newEntry() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        final View mView = getLayoutInflater().inflate(R.layout.save_popup, null); // must be declared final!!!


        pauseGraph();

        final EditText mEditText = findViewById(R.id.saveTextField);
        mBuilder.setView(mView);
        mBuilder.setTitle("Save Graph");


        mBuilder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Calendar calendar = Calendar.getInstance(); // Get current time and make it the subtext
                String currentDate = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.getTime()); // format date

                EditText editText = mView.findViewById(R.id.saveTextField); // this is the way. Don't forget the mView part!

                // ADD THE TEXT!!!
                addData("\nTitle: " + editText.getText().toString() +"\n\nLast Frequency = " + textFrequency.getText().toString() +
                        "\nLast Amplitude = " + textAmplitude.getText().toString() +
                        "\n\n" + currentDate + "\n");


                runGraph();
                dialog.dismiss();

            }
        });

        mBuilder.setPositiveButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                runGraph();
                dialog.dismiss();
            }

        });

        AlertDialog dialog = mBuilder.create();
        dialog.show();

    }
*/
}