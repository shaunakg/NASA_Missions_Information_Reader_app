/*
Hello, developer. If you're seeing this, there's a few things you need to know:
> Pretty sure that this won't run below Marshmallow (API 26)
> Also, if you want to run the test routine, press the 'developer mode disabled' checkbox 7 times.
> Lastly, thank you for trying this app out.
*/

package com.pulsarappdev.nasamissionsinformationgrabber;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Scanner;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {
    private EditText missionText;
    private Button getMissionButton;
    private CheckBox openLinkCheckbox;
    private ProgressDialog pDialog;
    private Button testButton;
    private TextView credits;
    private final String USER_AGENT = "Mozilla/5.0";
    private int developerTries = 1; // This will make 7 clicks. Yes, i could do it another way.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getMissionButton = findViewById(R.id.getMission);
        missionText = findViewById(R.id.missionName);
        openLinkCheckbox = findViewById(R.id.openLinkCheckbox);

        testButton = (Button) findViewById(R.id.testButton);
        testButton.setVisibility(View.GONE);

        credits = (TextView) findViewById(R.id.txtCredits);
        credits.setMovementMethod(LinkMovementMethod.getInstance());

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (!checkPermission()) {
            new DownloadFileFromURL().execute("https://raw.githubusercontent.com/shaunakg/NASA_Missions_Information_Reader_app/master/missions_database.csv");;
        } else {
            if (checkPermission()) {
                requestPermissionAndContinue();
                new DownloadFileFromURL().execute("https://raw.githubusercontent.com/shaunakg/NASA_Missions_Information_Reader_app/master/missions_database.csv");
            }
        }

        Toast featureNotImplemented = Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.feature_not_implemented_toast), Toast.LENGTH_SHORT);

        File f = new File(Environment.getExternalStorageDirectory(), getString(R.string.database_storage_folder));
        if (!f.exists()) {
            f.mkdirs();
        }

    }

    public void enableDeveloperMode(View view) {
        boolean checked = ((CheckBox) view).isChecked();

        if (checked) {
            if (developerTries==Integer.parseInt(getApplicationContext().getString(R.string.developer_mode_tries))) {
                ((CheckBox) view).setText(getString(R.string.enabled_developer_mode_checkbox_text));
                testButton.setVisibility(View.VISIBLE); // SET TO View.GONE to not display test button
                Toast.makeText(getApplicationContext(), "You have enabled developer access. Please note that the button that is now shown will perform a test routine which will change for development purposes. Check changelog or source code for more information.", Toast.LENGTH_LONG).show();
            } else {
                ((CheckBox) view).setText(getString(R.string.tried_developer_mode_checkbox_text)+" ["+developerTries+"]");
                ((CheckBox) view).toggle();
                testButton.setVisibility(View.GONE);
                developerTries=developerTries+1;
            }
        } else {
            ((CheckBox) view).setText(getString(R.string.disabled_developer_mode_checkbox_text));
            testButton.setVisibility(View.GONE); // SET TO View.GONE to not display test button
            developerTries = 0;
        }
    }

    private static final int PERMISSION_REQUEST_CODE = 200;
    private boolean checkPermission() {

        return ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ;
    }

    private void requestPermissionAndContinue() {
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE)
                    && ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setCancelable(true);
                alertBuilder.setTitle(getString(R.string.permission_required_title));
                alertBuilder.setMessage(R.string.permission_required_message);
                alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.N)
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE
                                , READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                    }
                });
                AlertDialog alert = alertBuilder.create();
                alert.show();
                Log.e("", "permission denied, show dialog");
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE,
                        READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        } else {
            Toast.makeText(getApplicationContext(), "Storage permission granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (permissions.length > 0 && grantResults.length > 0) {

                boolean flag = true;
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        flag = false;
                    }
                }
                if (flag) {
                    Toast.makeText(getApplicationContext(), "Storage permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    finish();
                }

            } else {
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void showInfoDialog(View view, String title, String message) {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // add a button
        builder.setPositiveButton("OK", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            System.out.println("Starting download");

            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Downloading mission database...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                String root = Environment.getExternalStorageDirectory().toString();

                System.out.println("Downloading");
                URL url = new URL(f_url[0]);

                URLConnection conection = url.openConnection();
                conection.connect();
                // getting file length
                int lenghtOfFile = conection.getContentLength();

                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                // Output stream to write file

                OutputStream output = new FileOutputStream(root+"/" + getString(R.string.database_storage_folder) +"/mission_database.csv");
                byte data[] = new byte[1024];

                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;

                    // writing data to file
                    output.write(data, 0, count);

                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }



        /**
         * After completing background task
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            System.out.println("Downloaded");
            pDialog.dismiss();
            Toast.makeText(getApplicationContext(), "Synced database", Toast.LENGTH_LONG);
        }

    }

    public void getMission(View view) {

        // new DownloadFilesTask().execute("https://api.nasa.gov/planetary/apod?api_key=0XrYsb3ygxkQKNPnj18BOHuMq9tt4rQb0XDINXO5");

        StringBuilder write_total = new StringBuilder(); // This... this is unholy

        // Toast featureNotImplemented = Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.feature_not_implemented_toast), Toast.LENGTH_SHORT);
        // featureNotImplemented.show();

        Context context = getApplicationContext();

        if (missionText.getText().toString().length() == 0) {

            String warn_text = context.getString(R.string.no_mission_warning_text);
            String warn_title = context.getString(R.string.no_mission_warning_title);
            showInfoDialog(view, warn_title, warn_text);
            missionText.getText().clear();

        } else if (missionText.getText().toString().equals("dev_112")){
            System.out.println("hi");

        } else {

            /*String missionDatabaseLocation = this.getApplicationInfo().dataDir + File.separatorChar + "missions_database.csv";
            File mission_database = new File(missionDatabaseLocation);*/

            try{
                CSVReader reader = new CSVReader(new FileReader(Environment.getExternalStorageDirectory().toString() + "/nasa_missions_information/mission_database.csv"));//Specify asset file name
                String [] nextLine;

                if (missionText.getText().toString().toLowerCase().equals("license".toLowerCase())) {
                    missionText.setText(context.getString(R.string.license_message));
                } else if (missionText.getText().toString().endsWith("##")) {
                    missionText.setText("");
                    Toast.makeText(this, "Cleared field", Toast.LENGTH_SHORT).show();
                } else {
                    boolean wasValidMission = false;
                    while ((nextLine = reader.readNext()) != null) {
                        // nextLine[] is an array of values from the line

                        if (missionText.getText().toString().toLowerCase().equals(nextLine[1].toLowerCase())) {
                            write_total.append(nextLine[0]+"##");
                            missionText.setText(write_total.toString());
                            missionText.setSelection(0,missionText.getText().toString().length());
                            wasValidMission = true;

                            if (openLinkCheckbox.isChecked()) {
                                Uri webpage = Uri.parse(nextLine[0]);
                                if (!nextLine[0].startsWith("http://") && !nextLine[0].startsWith("https://")) {
                                    webpage = Uri.parse("http://" + nextLine[0]);
                                }
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, webpage);
                                startActivity(browserIntent);
                            }


                            break;
                        }
                        /*
                        System.out.println(nextLine[1] + " : " + nextLine[0] + "etc...");
                        Log.d("VariableTag", nextLine[0]);
                        */
                    }

                    if (!wasValidMission) {
                        Toast.makeText(this, "Invalid mission, type \'list\' to see all inputs", Toast.LENGTH_LONG).show();
                    }

                }

            }catch(Exception e){
                e.printStackTrace();
                credits.setText(context.getString(R.string.database_exception_message));
                credits.setMovementMethod(LinkMovementMethod.getInstance());
            }

            /*
            // Old test code, won't actually contribute to the usefulness of the program

            String test_text = context.getString(R.string.information_dialog_test_text);
            String test_title = "Mission Information: " + missionText.getText().toString();
            System.out.println("THIS IS THE TITLE:" + test_title);
            missionText.getText().clear();
            showInfoDialog(view, test_title, test_text);
            */
        }
    }

    public void testRoutine(View view) {
        String url = "http://api.nasa.gov/planetary/apod";
        String charset = java.nio.charset.StandardCharsets.UTF_8.name();
        String api_key = "0XrYsb3ygxkQKNPnj18BOHuMq9tt4rQb0XDINXO5";

        try {

            String query = String.format("api_key=%s", URLEncoder.encode(api_key, charset));
            URLConnection connection = new URL(url + "?" + query).openConnection();
            connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
            connection.setRequestProperty("Accept","*/*");

            InputStream response = connection.getInputStream();
            // InputStream error = connection.getErrorStream();

            try (Scanner scanner = new Scanner(response)) {
                String responseBody = scanner.useDelimiter("\\A").next();
                System.out.println(responseBody);
            }

        } catch (Exception e) {
            System.out.println("- Could not get HTTP, error -");
            credits.setText(getApplicationContext().getString(R.string.developer_exception_message));
            credits.setMovementMethod(LinkMovementMethod.getInstance());
            e.printStackTrace();
        }


    }
}
