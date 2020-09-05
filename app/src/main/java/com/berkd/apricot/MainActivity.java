package com.berkd.apricot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

public class MainActivity extends AppCompatActivity {

    /**
     * DEFINE VARIABLES HERE
     */
    String[] units = {"Celsius", "Fahrenheit"};
    private final Handler mHandler = new Handler();
    private Runnable mTimer;
    private double graphLastXValue = 5d;
    private LineGraphSeries<DataPoint> mSeries;
    private TextView graphType; // used to store the amplitude OR frequency type.
    private int myDataPoints = 50; // used to set the precision of the amplitude accuracy.


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        aboutPopup();
        buttonConnection();         // handle the "connection status" events
        buttonViewLogs();           // handle the "view logs" events
        buttonToggleLogging();      // handle the "toggle mode" events
        buttonProgramMode();        // handle the "program mode" events


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
                graphLastXValue += 0.25d;
                mSeries.appendData(new DataPoint(graphLastXValue, Math.random()), true, myDataPoints);
                mHandler.postDelayed(this, 500);
            }
        };
        mHandler.postDelayed(mTimer, 50);
    }

    public void pauseGraph() {
        mHandler.removeCallbacks(mTimer); // pause the graph
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
        final int unit = 0; // 0 == Celsius
                            // 1 == Fahrenheit


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
        mSpinner.setSelection(loadSpinnerState());
        //

        mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if(mSpinner.getSelectedItem().toString().equalsIgnoreCase("celsius")) {
                    Toast.makeText(MainActivity.this, "Celsius set", Toast.LENGTH_SHORT).show();
                    saveSpinnerState(0);
                    dialog.dismiss();
                }
                if(mSpinner.getSelectedItem().toString().equalsIgnoreCase("fahrenheit")) {
                    Toast.makeText(MainActivity.this, "Fahrenheit Set", Toast.LENGTH_SHORT).show();
                    saveSpinnerState(1);
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
    public void saveSpinnerState(int userChoice) {
        SharedPreferences sharedPref = getSharedPreferences("FileName",0);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putInt("userChoiceSpinner", userChoice);
        prefEditor.commit();
    }

    /**
     * LOAD_SPINNER_STATE
     *  Return the value of the previously selected font size, return integer array value
     */
    public int loadSpinnerState() {
        SharedPreferences sharedPref = getSharedPreferences("FileName",MODE_PRIVATE);
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

                Toast.makeText(MainActivity.this, "Clicked! Functionality coming soon.", Toast.LENGTH_SHORT).show();

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
                mBuilder.setTitle("Logging Options");

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

    public void buttonProgramMode() {
        Button buttonInsert = findViewById(R.id.buttonProgramMode);
        buttonInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                View mView = getLayoutInflater().inflate(R.layout.mode_popup, null);
                mBuilder.setTitle("Program Mode");

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

}