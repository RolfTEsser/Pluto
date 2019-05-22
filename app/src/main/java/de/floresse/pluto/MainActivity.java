package de.floresse.pluto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import android.widget.Toolbar;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;

import de.floresse.drivefile.DriveServiceHelper;

public class MainActivity extends Activity {

    private static final String LogTAG = "Pluto";
    public static final String filename = "pluto.txt";

    public static final int REQUEST_CODE_SIGN_IN = 11;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 12;

    private DriveFile driveFile = null;
    private MetadataChangeSet changeSet = null;
    private String timestamp = "";
    private String timestamp_vergl = "";
    private Boolean newFile = false;
    private ScrollView sv = null;
    private LinearLayout llges = null;
    private MyDriveServiceHelper mDriveServiceHelper;

    private ArrayList<LinearLayout> llline = new ArrayList<>();

    private ArrayList<ets> vets = new ArrayList();
    private ArrayList<ifs> vifs = new ArrayList();

    private Float preisgessum = 0f;
    private EditText etPreisgessum = null;

    private int driveFilecount = 0;
    private String driveFileId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Log.i(LogTAG, "Hier ist Pluto");

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);
        toolbar.setTitle("Pluto");
        setActionBar(toolbar);

        sv = findViewById(R.id.scroll);
        llges = new LinearLayout(this);
        llges.setOrientation(LinearLayout.VERTICAL);
        sv.addView(llges);

        etPreisgessum = findViewById(R.id.preisgessum);

        requestSignIn();

    }

    @Override
    protected void onPause() {
        super.onPause();
        deleteEmptyLines();
            Log.i(LogTAG, "files are not equal : writeDriveFile");
            if (newFile) {
                //hier create und save
                createFile();
            } else {
                //readTimestamp und save
                readTimestamp(driveFileId);
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
    private void requestSignIn() {
        Log.d(LogTAG, "Requesting sign-in");

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (googleAccount==null) {
            startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        } else {
            hasAccount(googleAccount);
        }
    }

    private void hasAccount(GoogleSignInAccount googleAccount) {
        // Use the authenticated account to sign in to the Drive service.
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        this, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(googleAccount.getAccount());
        com.google.api.services.drive.Drive googleDriveService =
                new com.google.api.services.drive.Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        new GsonFactory(),
                        credential)
                        .setApplicationName("Pluto")
                        .build();

        // The DriveServiceHelper encapsulates all REST API and SAF functionality.
        // Its instantiation is required before handling any onClick actions.
        mDriveServiceHelper = new MyDriveServiceHelper(googleDriveService);

        queryFile();

    }
    private void queryFile() {
        if (mDriveServiceHelper != null) {
            Log.d(LogTAG, "Querying for files.");
            driveFileId="";
            driveFilecount=0;
            mDriveServiceHelper.queryDriveFiles()
                    .addOnSuccessListener(fileList -> {
                        for (File file : fileList.getFiles()) {
                            Log.i(LogTAG, "queryFile filename : " + file.getName() + " " + file.getId());
                            if (file.getName().equalsIgnoreCase(filename)) {
                                driveFileId = file.getId();
                                driveFilecount++;
                            }
                        }
                        Log.i(LogTAG, filename + " count : " + driveFilecount);
                        switch(driveFilecount) {
                            case 0:
                                newFile=true;
                                onLoadDone();
                                break;
                            case 1:
                                newFile=false;
                                readFile(driveFileId);
                                break;
                            default:
                                newFile=false;
                                Log.i(LogTAG, "Drive: zu viele Treffer");
                                Toast.makeText(this, "zu viele Treffer " + filename, Toast.LENGTH_LONG).show();
                                //TODO hier FilePicker aufrufen und irg.wie FileId rauskriegen
                        }
                    })
                    .addOnFailureListener(exception -> Log.e(LogTAG, "Unable to query files.", exception));
        }
    }

    private void openFilePicker() {
        if (mDriveServiceHelper != null) {
            Log.d(LogTAG, "Opening file picker.");

            Intent pickerIntent = mDriveServiceHelper.createFilePickerIntent();

            // The result of the SAF Intent is handled in onActivityResult.
            startActivityForResult(pickerIntent, REQUEST_CODE_OPEN_DOCUMENT);
        }
    }

    private void openFileFromFilePicker(Uri uri) {
        if (mDriveServiceHelper != null) {
            Log.d(LogTAG, "Opening " + uri.getPath());

            mDriveServiceHelper.openFileUsingStorageAccessFramework(getContentResolver(), uri)
                    .addOnSuccessListener(readResult -> {
                        String timestamp = (String) readResult.get(0);
                        ArrayList<ifs> vifs = (ArrayList<ifs>) readResult.get(1);
                        try {

                        } catch (Exception e) {

                        }
                    })
                    .addOnFailureListener(exception ->
                            Log.e(LogTAG, "Unable to open file from picker.", exception));
        }
    }

    private void createFile() {
        if (mDriveServiceHelper != null) {
            Log.d(LogTAG, "Creating a file.");

            mDriveServiceHelper.createDriveFile(filename)
                    .addOnSuccessListener(fileId -> saveFile(fileId))
                    .addOnFailureListener(exception ->
                            Log.e(LogTAG, "Couldn't create file.", exception));
        }
    }

    private void readFile(String fileId) {
        if (mDriveServiceHelper != null) {
            Log.d(LogTAG, "Reading file " + fileId);

            mDriveServiceHelper.readDriveFile(fileId)
                    .addOnSuccessListener(readResult -> {
                        timestamp = (String) readResult.get(0);
                        vifs = (ArrayList<ifs>) readResult.get(1);

                        onLoadDone();

                    })
                    .addOnFailureListener(exception ->
                            Log.e(LogTAG, "Couldn't read file.", exception));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    handleSignInResult(resultData);
                }
                break;

            case REQUEST_CODE_OPEN_DOCUMENT:
                if (resultCode == Activity.RESULT_OK) {
                    if (resultData != null) {
                        Uri uri = resultData.getData();
                        if (uri != null) {
                            openFileFromFilePicker(uri);
                        }
                    } else {
                        //TODO neu anlegen

                    }
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }


    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(LogTAG, "Signed in as " + googleAccount.getEmail());

                    hasAccount(googleAccount);

                })
                .addOnFailureListener(exception -> Log.e(LogTAG, "Unable to sign in.", exception));
    }

    private void readTimestamp(String fileId) {
        if (mDriveServiceHelper != null) {
            Log.d(LogTAG, "Reading timestamp from file " + fileId);

            mDriveServiceHelper.readDriveFile(fileId)
                    .addOnSuccessListener(readResult -> {
                        timestamp_vergl = (String) readResult.get(0);
                        if (timestamp.equals(timestamp_vergl)) {
                            saveFile(fileId);
                        } else {
                            Toast.makeText(this, "von anderem Dagobert überholt", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(exception ->
                            Log.e(LogTAG, "Couldn't read file.", exception));
        }
    }

    private void saveFile(String fileId) {
        if (mDriveServiceHelper != null && fileId != null) {
            Log.d(LogTAG, "Saving " + fileId);

            timestamp = new Timestamp(new Date().getTime()).toString();
            Vector<Object> inp = new Vector<>();
            inp.add(0, timestamp);
            inp.add(1, vifs);

            mDriveServiceHelper.saveDriveFile(fileId, filename, inp)
                    .addOnFailureListener(exception ->
                            Log.e(LogTAG, "Unable to save file via REST.", exception));
        }
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

        sv.smoothScrollTo(0,0);

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

    public class MyDriveServiceHelper extends DriveServiceHelper {

        public MyDriveServiceHelper(com.google.api.services.drive.Drive drive) {
            super(drive);
        }

        @Override
        public Vector<Object> readFile(InputStream is) {
            String timestamp = null;
            ArrayList<ifs> vifs = new ArrayList<ifs>();
            Vector<Object> readResult = new Vector<>();

            try (ObjectInputStream ois = new ObjectInputStream(is);)
            {

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

            } catch (Exception e) {
                Log.i(LogTAG, "Exception bei Drive-File read : " + e.toString());
                vifs=null;
            }

            readResult.add(0, timestamp);
            readResult.add(1, vifs);

            return readResult;
        }

        @Override
        public void saveFile(ByteArrayOutputStream baos, Vector<Object> inp) {

            String timestamp = (String) inp.get(0);
            ArrayList<ifs> vifs = (ArrayList<ifs>) inp.get(1);

            try (ObjectOutputStream oos = new ObjectOutputStream(baos);)
            {
                oos.writeObject(timestamp);
                oos.writeObject(vifs.size());

                for (int i=0;i<vifs.size();i++) {
                    oos.writeObject(vifs.get(i).anzahl);
                    oos.writeObject(vifs.get(i).produkt);
                    oos.writeObject(vifs.get(i).preis);
                }
                oos.flush();
                oos.close();
            } catch (IOException e) {
                Log.i(LogTAG, "IOException Drive-File : " + e.toString());
            }

        }

    }

}
