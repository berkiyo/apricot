package com.berkd.apricot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.*;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVER_BT = 1;

    /**
     * DEFINE VARIABLES HERE
     */
    String[] units = {"Celsius", "Fahrenheit"};
    int[] roomSim = {480, 460, 490, 510, 520, 560, 600, 590, 760, 900, 1050, 1120, 1075, 1010, 980, 975, 1080, 1250, 1450, 1500, 1650, 1750, 1800, 1950, 2000, 2050, 2100, 2150, 2180, 2200, 2250, 2300, 2400, 2500, 2550, 2600, 2750, 2650, 2800, 2850, 2900, 2950, 3100, 3300, 3400, 3100, 2800, 2250, 2000, 1700, 1500, 1300, 1200, 1150, 1250, 1350, 1240, 1240, 1050, 580, 490, 485, 520, 600, 770, 775, 770};
    int index = 0;

    int set = 0;
    private final Handler mHandler = new Handler();
    private Runnable mTimer;
    private double graphLastXValue = 5d;
    private LineGraphSeries<DataPoint> mSeries;
    private TextView graphType; // used to store the amplitude OR frequency type.
    private int myDataPoints = 50; // used to set the precision of the amplitude accuracy.

    double co2Count = Math.random()*1000;
    double humidCount = 20;
    double simVal = 0;

    int simState = 0;
    int unitState = 0; // 0 = celsius, 1 = fahrenheit

    BluetoothAdapter mBlueAdapter;

    DatabaseHelper myDB;
    private boolean buttonFreezeClicked = false;


    // set text values
    private TextView co2Reading;
    private TextView tempReading;
    private TextView humidReading;
    private TextView ventilationStatus;
    private TextView connectionStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        co2Reading = findViewById(R.id.textCo2Val);
        tempReading = findViewById(R.id.textTempVal);
        humidReading = findViewById(R.id.textHumidtyVal);
        ventilationStatus = findViewById(R.id.textVentilationStatus);
        connectionStatus = findViewById(R.id.textConnectionStatus);

        //aboutPopup();
        buttonConnection();         // handle the "connection status" events
        buttonViewLogs();           // handle the "view logs" events
        buttonProgramMode();        // handle the "program mode" events

        createNotificationChannel(); // create the notification channel



        updateCo2Val();
        updateHumidity();
        //updateTemperature();
        //updateVentilation();



        GraphView graph = (GraphView) findViewById(R.id.graph);
        initGraph(graph);
        runGraph();
        runNotify();

    }


    public void bluetoothSetup() {
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBlueAdapter == null) {
            connectionStatus.setText("Bluetooth Not Available");
        } else {
            connectionStatus.setText("Bluetooth Available");
        }
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
        super.onResume();
    }

    @Override
    public void onPause() {
        //pauseGraph();
        super.onPause();
    }

    public void runNotify() {
        mTimer = new Runnable() {
            int notificationOn = 0;
            int secondsSinceLast = 0;
            @Override
            public void run() {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, "apricot")
                        .setSmallIcon(R.drawable.ic_warning)
                        .setContentTitle("Apricot Notification")
                        .setContentText("Consider ventilating immediately! CO2 levels over 2000.")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);

                if (simVal >= 2000) {
                    if (secondsSinceLast == 0) {
                        notificationManager.notify(100, builder.build());
                        secondsSinceLast++;
                    } else {
                        secondsSinceLast++;
                    }
                }
                if (secondsSinceLast == 20) {
                    secondsSinceLast = 0; //  if 20 seconds has elapsed, then reset
                }
                mHandler.postDelayed(this, 1000);
            }
        };

        mHandler.postDelayed(mTimer, 50);
    }


    public void runGraph() {
        mTimer = new Runnable() {
            @Override
            public void run() {

                Date date = Calendar.getInstance().getTime();
                DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
                String strDate = dateFormat.format(date);

                switch(simState){
                    case 0:
                        // do basic randomness
                        simVal = Math.random()*1000;
                        ventilationStatus.setText("Debug Mode Enabled");
                        ventilationStatus.setTextColor(Color.BLUE);
                        break;
                    case 1:
                        // simulate a bedroom with door closed then opened after x amount of time
                        if (index < roomSim.length) {
                            simVal = roomSim[index];
                            index++;
                            if (simVal > 2800) {
                                ventilationStatus.setText("Danger! Ventilate Immediately");
                                ventilationStatus.setTextColor(Color.parseColor("#CF0300"));

                            }
                            else if (simVal > 2500) {
                                ventilationStatus.setText("Ventilate Immediately");
                                ventilationStatus.setTextColor(Color.parseColor("#CF0300"));
                            }
                            else if (simVal > 1000) {
                                ventilationStatus.setText("Consider Ventilating");
                                ventilationStatus.setTextColor(Color.parseColor("#E86016"));
                                //addData("Dangerous CO2 Level at: " + strDate);

                            }
                            else {
                                ventilationStatus.setText("Ventilation Not Required");
                                ventilationStatus.setTextColor(Color.parseColor("#16AA22"));

                            }
                        } else {
                            index = 0;
                        }
                        break;
                    case 2:
                        // simulate dangerous CO2 levels (i.e. smoky)
                        simVal = Math.random()*2500;
                        ventilationStatus.setText("Smoky Test (Debug)");
                        ventilationStatus.setTextColor(Color.parseColor("#710095"));
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



    public void freezeClicked(View view) {

        if (buttonFreezeClicked) {
            runGraph();
            // set it back to normal state
            buttonFreezeClicked = false;

        } else {
            pauseGraph();
            buttonFreezeClicked = true;
        }

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
                Toast.makeText(MainActivity.this, "IC_SAVE", Toast.LENGTH_SHORT).show();
                break;

            case R.id.item3:
                settingsPopup();
                break;

            case R.id.item4:
                Toast.makeText(this, "Dark mode coming soon!", Toast.LENGTH_SHORT).show();
                break;

            case R.id.item5:
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
        final Spinner mSpinner2 = mView.findViewById(R.id.spinnerColour);

        final ArrayAdapter<String> sAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.units));

        final ArrayAdapter<String> sAdapter2 = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.colours));

        sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(sAdapter);

        sAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner2.setAdapter(sAdapter2);


        // Load the previously saved font selection!
        mSpinner.setSelection(loadSpinnerState("settings"));
        mSpinner2.setSelection(loadSpinnerState("colour"));

        //

        mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                /**
                 * UNITS
                 */
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

                /**
                 * COLOURS
                 */
                if(mSpinner2.getSelectedItem().toString().equalsIgnoreCase("blue")) {
                    Toast.makeText(MainActivity.this, "Blue set", Toast.LENGTH_SHORT).show();
                    mSeries.setColor(Color.BLUE);
                    saveSpinnerState(0, "colour");
                    dialog.dismiss();
                }
                if(mSpinner2.getSelectedItem().toString().equalsIgnoreCase("red")) {
                    Toast.makeText(MainActivity.this, "Red Set", Toast.LENGTH_SHORT).show();
                    mSeries.setColor(Color.RED);
                    saveSpinnerState(1,"colour");
                    dialog.dismiss();
                }
                if(mSpinner2.getSelectedItem().toString().equalsIgnoreCase("green")) {
                    Toast.makeText(MainActivity.this, "Green Set", Toast.LENGTH_SHORT).show();
                    mSeries.setColor(Color.GREEN);
                    saveSpinnerState(2, "colour");
                    dialog.dismiss();
                }
                if(mSpinner2.getSelectedItem().toString().equalsIgnoreCase("yellow")) {
                    Toast.makeText(MainActivity.this, "Yellow Set", Toast.LENGTH_SHORT).show();
                    mSeries.setColor(Color.YELLOW);
                    saveSpinnerState(3, "colour");
                    dialog.dismiss();
                }
                if(mSpinner2.getSelectedItem().toString().equalsIgnoreCase("black")) {
                    Toast.makeText(MainActivity.this, "Black Set", Toast.LENGTH_SHORT).show();
                    mSeries.setColor(Color.BLACK);
                    saveSpinnerState(4, "colour");
                    dialog.dismiss();
                }
                if(mSpinner2.getSelectedItem().toString().equalsIgnoreCase("magenta")) {
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
        Button buttonStatus = findViewById(R.id.buttonConnection);
        buttonStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                View mView = getLayoutInflater().inflate(R.layout.status_popup, null);
                mBuilder.setTitle("Status");


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

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "apricotChannel";
            String description = "Alert channel for Apirocot alerts";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("apricot", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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