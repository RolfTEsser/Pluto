package de.floresse.pluto;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveApi.DriveIdResult;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;

public class MainActivity extends Activity
        implements ConnectionCallbacks,
        OnConnectionFailedListener {

    private static final String LogTAG = "Pluto";
    private static final int REQUEST_CODE_RESOLUTION = 3;
    public static final String filename = "pluto.txt";

    private GoogleApiClient mGoogleApiClient;

    private DriveFile driveFile = null;
    private MetadataChangeSet changeSet = null;
    private String timestamp = "";
    private Boolean newFile = false;

    private LinearLayout llges = null;

    private ArrayList<LinearLayout> llline = new ArrayList<>();

    private ArrayList<ets> vets = new ArrayList();
    private ArrayList<ifs> vifs = new ArrayList();

    private Float preisgessum = 0f;
    private EditText etPreisgessum = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Log.i(LogTAG, "Hier ist Pluto");
        ScrollView sv = findViewById(R.id.scroll);
        llges = new LinearLayout(this);
        llges.setOrientation(LinearLayout.VERTICAL);
        sv.addView(llges);

        etPreisgessum = findViewById(R.id.preisgessum);

        // Create a GoogleApiClient instance
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                //		.addScope(Drive.SCOPE_APPFOLDER)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setActionBar(toolbar);
    }

    @Override
    protected void onPause() {
        super.onPause();
        deleteEmptyLines();
        writeDriveFile();
    }

    @Override
    public void onConnected(Bundle ch) {
        Log.i(LogTAG, "onConnected: GoogleDrive connected");
        readDriveFile();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(LogTAG, "GoogleDrive connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(LogTAG, "GoogleDrive connection failed");
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_done) {
            clicked(null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean alleProdukteBelegt() {
        boolean alleProdukteBelegt = true;
        for (Integer i=0;i<vifs.size();i++) {
            if (vifs.get(i).produkt.equals("")) {
                alleProdukteBelegt = false;
            }
        }
        return alleProdukteBelegt;
    }

    public void deleteEmptyLines() {
        int count = 0;
        for (int i = vifs.size()-1; i >=0; i--) {
            if (vifs.get(i).produkt.equals("")) {
                count++;
                if (count > 1) {
                    //    Log.i(LogTAG, " deleteEmptyLines : " + i + " / " + vifs.size());
                    vifs.remove(i);
                    //    Log.i(LogTAG, " deleteEmptyLines : " + i + " / " + vifs.size());
                }
            }
        }
    }

    public void clicked(View v) {

        Collections.sort(vifs);

        for (Integer i=0;i<vifs.size();i++) {
            vets.get(i).anzahl.setEnabled(true);
            vets.get(i).produkt.setEnabled(true);
            vets.get(i).preis.setEnabled(true);
            vets.get(i).preisges.setEnabled(true);
            belegenETs(i);
        }

    }
    public void onLoadDone() {

        Collections.sort(vifs);

        for (Integer i = 0; i < vifs.size(); i++) {
            Log.i(LogTAG, "onLoadDone : " + i + " / " + vifs.size());
            neueZeile(i);
            belegenETs(i);

            Log.i(LogTAG, "onLoadDone : " + i + "/" + vets.get(i).produkt.getText() + "/" + vifs.get(i).produkt);

        }

        if (alleProdukteBelegt()) {
            Integer i = vifs.size();
            // neue Zeile
            neueZeile(i);

            vifs.add(new ifs());
        }

    }

    public void belegenETs(Integer i){
        DecimalFormat df = new DecimalFormat("###0.00");
        df.setRoundingMode(RoundingMode.HALF_UP);

        vets.get(i).anzahl.setText((vifs.get(i).anzahl != 0) ? vifs.get(i).anzahl.toString() : "");
        vets.get(i).produkt.setText(vifs.get(i).produkt);
        vets.get(i).preis.setText((vifs.get(i).preis != 0) ? vifs.get(i).preis.toString() : "");
        vets.get(i).preisges.setText((vifs.get(i).preisges != 0) ? df.format(vifs.get(i).preisges) : "");
    }

    public void neueZeile(Integer i) {
        llline.add(i, new LinearLayout(this));
        llline.get(i).setOrientation(LinearLayout.HORIZONTAL);
        llline.get(i).setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        vets.add(new ets(this));
        LinearLayout.LayoutParams llp =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(10, 0, 0, 0);
        vets.get(i).anzahl.setLayoutParams(llp);
        vets.get(i).anzahl.setTag(i);
        vets.get(i).anzahl.setEms(2);
        vets.get(i).anzahl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        vets.get(i).anzahl.setInputType(InputType.TYPE_CLASS_NUMBER);
        vets.get(i).anzahl.addTextChangedListener(new AnzahlChangeListener(vifs, i));
        vets.get(i).anzahl.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onAnzahlClicked(v);
            }
        });
        llline.get(i).addView(vets.get(i).anzahl);

        vets.get(i).produkt.setLayoutParams(llp);
        vets.get(i).produkt.setEms(6);
        vets.get(i).produkt.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        vets.get(i).produkt.addTextChangedListener(new ProduktChangeListener(vifs, i));
        llline.get(i).addView(vets.get(i).produkt);

        vets.get(i).preis.setLayoutParams(llp);
        vets.get(i).preis.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        vets.get(i).preis.setEms(4);
        vets.get(i).preis.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        vets.get(i).preis.addTextChangedListener(new PreisChangeListener(vifs, i));
        llline.get(i).addView(vets.get(i).preis);

        //LinearLayout.LayoutParams llpr =
        //        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        //llpr.setMargins(0, 0, 10, 0);
        //llpr.gravity = Gravity.RIGHT;
        vets.get(i).preisges.setLayoutParams(llp);
        vets.get(i).preisges.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        vets.get(i).preisges.setEms(4);
        vets.get(i).preisges.setTag(i);
        vets.get(i).preisges.setClickable(true);
        vets.get(i).preisges.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onPreisgesClicked(v);
            }
        });
        vets.get(i).preisges.setFocusable(false);
        vets.get(i).preisges.setCursorVisible(false);
        vets.get(i).preisges.setFocusableInTouchMode(false);
        vets.get(i).preisges.setGravity(Gravity.RIGHT);
        llline.get(i).addView(vets.get(i).preisges);

        llges.addView(llline.get(i));

    }

    public void onAnzahlClicked(View v) {
        Integer i = (Integer) v.getTag();
        if (vets.get(i).anzahl.getText().toString().compareTo("")==0) {
            vets.get(i).anzahl.setText("1");
        } else {
            if (vets.get(i).anzahl.getText().toString().compareTo("1")==0) {
                vets.get(i).anzahl.setText("");
            }
        }

    }

    public void onPreisgesClicked(View v) {
        Integer i = (Integer) v.getTag();
        if (vets.get(i).anzahl.isEnabled()) {
            vets.get(i).anzahl.setEnabled(false);
            vets.get(i).produkt.setEnabled(false);
            vets.get(i).preis.setEnabled(false);
        } else {
            vets.get(i).anzahl.setEnabled(true);
            vets.get(i).produkt.setEnabled(true);
            vets.get(i).preis.setEnabled(true);
        }

    }

    public void checkLine(Integer i) {
        /*
        if (vets.get(i).anzahl.getText().length() > 0) {
            vanzahl.set(i, Integer.valueOf(vets.get(i).anzahl.getText().toString()));
        }
        if (vets.get(i).produkt.getText().length() > 0) {
            vprodukt.set(i, vets.get(i).produkt.getText().toString());
        }
        if (vets.get(i).preis.getText().length() > 0) {
            vpreis.set(i, Float.valueOf(vets.get(i).preis.getText().toString()));
        }
        */

    }

    public void check() {
        for (Integer i=0;i<vifs.size();i++) {
            checkLine(i);
        }
        if (alleProdukteBelegt()) {
            Integer i = vifs.size();
            // neue Zeile
            neueZeile(i);

            vifs.add(new ifs());
        }
    }

    public void writeDriveFile() {
        // create new contents resource
        if (newFile){
            Log.i(LogTAG, "creating newDriveContents");
            Drive.DriveApi.newDriveContents(mGoogleApiClient)
                    .setResultCallback(driveContentsCallback);
        } else {
            if (driveFile!=null) {
                new RetrieveTSDriveFileContentsAsyncTask().execute(driveFile);
            }
        }
    }

    public void readDriveFile() {
        // create new contents resource
        Query query = new Query.Builder()
                .addFilter(Filters.contains(SearchableField.TITLE, filename))
                .build();
        Drive.DriveApi.query(mGoogleApiClient, query)
                .setResultCallback(metadataCallback);
    }

    // [START drive_contents_callback]
    final private ResultCallback<DriveContentsResult> driveContentsCallback =
            new ResultCallback<DriveContentsResult>() {
                @Override
                public void onResult(DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.i(LogTAG, "Error while trying to create new file contents");
                        return;
                    }
                    changeSet = new MetadataChangeSet.Builder()
                            .setTitle(filename)
                            .setMimeType("text/plain")
                            .build();
                    Drive.DriveApi.getRootFolder(mGoogleApiClient)
                            .createFile(mGoogleApiClient, changeSet, result.getDriveContents())
                            .setResultCallback(fileCallback);
                }
            };
    // [END drive_contents_callback]

    final private ResultCallback<DriveFileResult> fileCallback = new
            ResultCallback<DriveFileResult>() {
                @Override
                public void onResult(DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.i(LogTAG, "Error while trying to create the file");
                        return;
                    }
                    Log.i(LogTAG, "Created a file in App Folder: "
                            + result.getDriveFile().getDriveId());
                    DriveFile driveFile = result.getDriveFile();
                    new EditContentsAsyncTask().execute(driveFile);
                }
            };

    final private ResultCallback<DriveApi.MetadataBufferResult> metadataCallback =
            new ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(DriveApi.MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.e(LogTAG, "Problem while retrieving results");
                        return;
                    }
                    int vers = 0;
                    MetadataBuffer mdb = null;
                    try {
                        mdb = result.getMetadataBuffer();
                        for (Metadata md : mdb) {
                            if (md == null || !md.isDataValid() || md.isTrashed()) continue;
                            vers++;
                            // collect files
                            DriveId driveId = md.getDriveId();
                            Log.i(LogTAG, "found " + driveId);
                            Drive.DriveApi.fetchDriveId(mGoogleApiClient, driveId.getResourceId())
                                    .setResultCallback(idCallback);
                        }
                    } finally {
                        if (mdb != null) mdb.release();
                    }
                    switch(vers) {
                        case 0:
                            newFile=true;
                            onLoadDone();
                            break;
                        case 1:
                            newFile=false;
                            break;
                        default:
                            newFile=false;
                            Log.e(LogTAG, "Drive: zu viele Treffer");
                    }
                    Log.i(LogTAG, "Anz Treffer : " + vers + " " + newFile);
                }
            };

    public class EditContentsAsyncTask extends AsyncTask<DriveFile, Void, Boolean> {

        @Override
        protected Boolean doInBackground(DriveFile... args) {
            DriveFile file = args[0];
            newFile=false;
            try {
                DriveContentsResult driveContentsResult = file.open(
                        mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).await();
                if (!driveContentsResult.getStatus().isSuccess()) {
                    return false;
                }
                String timestamp = new Timestamp(new Date().getTime()).toString();
                MainActivity.this.timestamp = timestamp;

                DriveContents driveContents = driveContentsResult.getDriveContents();
                OutputStream os = driveContents.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(os);

                oos.writeObject(timestamp);
                oos.writeObject(vifs.size());

                for (int i=0;i<vifs.size();i++) {
                    oos.writeObject(vifs.get(i).anzahl);
                    oos.writeObject(vifs.get(i).produkt);
                    oos.writeObject(vifs.get(i).preis);
                }
                oos.flush();
                oos.close();

                com.google.android.gms.common.api.Status status =
                        driveContents.commit(mGoogleApiClient, changeSet).await();
                return status.getStatus().isSuccess();
            } catch (IOException e) {
                Log.e(LogTAG, "IOException while writing to the output stream", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Log.e(LogTAG, "Error while editing contents");
                return;
            }
            Log.i(LogTAG, "Successfully edited contents");
        }
    } // end class AsyncTask

    final private ResultCallback<DriveIdResult> idCallback = new ResultCallback<DriveIdResult>() {
        @Override
        public void onResult(DriveIdResult result) {
            driveFile = result.getDriveId().asDriveFile();
            new RetrieveDriveFileContentsAsyncTask().execute(driveFile);
        }
    };

    final private class RetrieveDriveFileContentsAsyncTask
            extends AsyncTask<DriveFile, Boolean, String> {

        @Override
        protected String doInBackground(DriveFile... args) {
            String contents = null;
            DriveFile file = args[0];
            DriveContentsResult driveContentsResult =
                    file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await();
            if (!driveContentsResult.getStatus().isSuccess()) {
                return null;
            }
            DriveContents driveContents = driveContentsResult.getDriveContents();
            try {
                ObjectInputStream ois = new ObjectInputStream(driveContents.getInputStream());

                timestamp = (String) ois.readObject();
                int size = (Integer) ois.readObject();

                for (int i=0;i<size;i++) {
                    vifs.add(new ifs());
                    vifs.get(i).anzahl = (Integer) ois.readObject();
                    vifs.get(i).produkt = (String) ois.readObject();
                    vifs.get(i).preis = (Float) ois.readObject();
                    vifs.get(i).preisges = 0f;
                }

                ois.close();
            } catch (IOException e) {
                Log.e(LogTAG, "IOException while reading the input stream", e);
                ///*
            } catch (ClassNotFoundException e) {
                Log.e(LogTAG, "ClassNotFoundException while reading the stream", e);
                //*/
            }
            driveContents.discard(mGoogleApiClient);
            return contents;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            onLoadDone();

        }
    }

    final private class RetrieveTSDriveFileContentsAsyncTask
            extends AsyncTask<DriveFile, Boolean, String> {

        @Override
        protected String doInBackground(DriveFile... args) {
            String timestamp = "";
            DriveFile file = args[0];
            DriveContentsResult driveContentsResult =
                    file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await();
            if (!driveContentsResult.getStatus().isSuccess()) {
                Log.e(LogTAG, "Error while reading the DriveFile " + driveContentsResult +
                        "/" + driveContentsResult.getStatus().getStatusCode() +
                        "/" + driveContentsResult.getStatus().getStatusMessage());
                return "";
            }
            DriveContents driveContents = driveContentsResult.getDriveContents();
            try {
                ObjectInputStream ois = new ObjectInputStream(driveContents.getInputStream());

                timestamp = (String) ois.readObject();

                ois.close();
            } catch (IOException e) {
                Log.e(LogTAG, "IOException while reading the input stream", e);
                ///*
            } catch (ClassNotFoundException e) {
                Log.e(LogTAG, "ClassNotFoundException while reading the stream", e);
                //*/
            }
            driveContents.discard(mGoogleApiClient);
            return timestamp;
        }

        @Override
        protected void onPostExecute(String timestamp) {
            super.onPostExecute(timestamp);
            //Log.i(LogTAG, "Timestamp (alt) :" + MainActivity.this.timestamp);
            //Log.i(LogTAG, "Timestamp (neu) :" + timestamp);
            Log.i(LogTAG, "onPostExecute : " + timestamp + " / " + MainActivity.this.timestamp);
            if (timestamp.equals(MainActivity.this.timestamp)) {
                new EditContentsAsyncTask().execute(driveFile);
            } else {
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.i(LogTAG, "DriveFile - überholt");
                Toast.makeText(MainActivity.this, "Timestamp - Error Google DriveFile", Toast.LENGTH_LONG).show();
            }
        }
    }

    public class AnzahlChangeListener implements TextWatcher {
        private ArrayList<ifs> vifs;
        Integer i;

        public AnzahlChangeListener(ArrayList<ifs> vifs, Integer i) {
            super();
            this.vifs = vifs;
            this.i = i;
        }

        @Override
        public void afterTextChanged(Editable s) {
            vifs.get(i).anzahl = s.length() > 0 ? Integer.valueOf(s.toString()) : 0;
            preisges(i);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
        }

    }

    public class ProduktChangeListener implements TextWatcher {
        private ArrayList<ifs> vifs;
        Integer i;

        public ProduktChangeListener(ArrayList<ifs> vifs, Integer i) {
            super();
            this.vifs = vifs;
            this.i = i;
        }

        @Override
        public void afterTextChanged(Editable s) {
            vifs.get(i).produkt = s.toString();
            check();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
        }

    }

    public class PreisChangeListener implements TextWatcher {
        private ArrayList<ifs> vifs;
        Integer i;

        public PreisChangeListener(ArrayList<ifs> vifs, Integer i) {
            super();
            this.vifs = vifs;
            this.i = i;
        }

        @Override
        public void afterTextChanged(Editable s) {
            try {
                vifs.get(i).preis = (s.length() > 0) ? Float.parseFloat(s.toString()) : 0f;
                preisges(i);
            }
            catch (NumberFormatException e) {

            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
        }

    }

    private void preisges(Integer i) {
        DecimalFormat df = new DecimalFormat("###0.00");
        df.setRoundingMode(RoundingMode.HALF_UP);

        if (vifs.get(i).anzahl != 0 && vifs.get(i).preis != 0f) {
            vifs.get(i).preisges = vifs.get(i).anzahl * vifs.get(i).preis;
            vets.get(i).preisges.setText(df.format(vifs.get(i).preisges));
        } else {
            vifs.get(i).preisges = 0f;
            vets.get(i).preisges.setText("");
        }
        preisgessum = 0f;
        for (int j=0;j<vifs.size();j++) {
            //Log.i(LogTAG, " preisges : " + vifs.element(j).preisges);
            preisgessum += vifs.get(j).preisges;
        }

        //Log.i(LogTAG, " preisges : " + preisgessum);

        etPreisgessum.setText((preisgessum != 0) ? df.format(preisgessum) : "");
    }

    public class ets {
        public EditText anzahl;
        public EditText produkt;
        public EditText preis;
        public EditText preisges;

        public ets(Context ct) {
            anzahl = new EditText(ct);
            produkt = new EditText(ct);
            preis = new EditText(ct);
            preisges = new EditText(ct);
        }
    }

    public class ifs implements Comparable<ifs> {
        public Integer anzahl;
        public String produkt;
        public Float preis;
        public Float preisges;

        public ifs() {
            anzahl = new Integer(0);
            produkt = new String("");
            preis = new Float(0f);
            preisges = new Float(0f);
        }

        public int compareTo(ifs compareIfs) {

            String thisstring = "";
            String compareString = "";

            thisstring    = (this.anzahl > 0) ? "A" : "B";
            compareString = (compareIfs.anzahl > 0) ? "A" : "B";

            thisstring = thisstring + this.produkt;
            compareString = compareString + compareIfs.produkt;

            return thisstring.compareToIgnoreCase(compareString);

        }
    }
}
