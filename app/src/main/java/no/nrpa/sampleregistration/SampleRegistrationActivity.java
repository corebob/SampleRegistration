package no.nrpa.sampleregistration;

import android.Manifest;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class SampleRegistrationActivity extends AppCompatActivity implements LocationListener {

    private ViewSwitcher switcher;
    private ListView lstProj;

    private ArrayList<String> items;
    private ListAdapter adapter;

    TextView tvProjName;

    private LocationManager locManager;
    private String locProvider;
    private boolean providerEnabled;
    private TextView tvCurrProvider;
    private TextView tvCurrGPSDate;
    private TextView tvCurrLat;
    private TextView tvCurrLon;
    private TextView tvNextID;
    private EditText etNewSampleType;
    private EditText etNewComment;

    File appDir;
    int nextId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_registration);

        getWindow().getDecorView().setBackgroundColor(Color.parseColor("#fdf6e3"));

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setLogo(R.mipmap.ic_launcher);
            actionBar.setDisplayUseLogoEnabled(true);
        }

        if(!isExternalStorageWritable())
        {
            Toast.makeText(this, "Access to external storage denied", Toast.LENGTH_SHORT).show();
            exitApp();
        }

        appDir = getStorageDir("sampleregistration");

        switcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);
        switcher.setInAnimation(AnimationUtils.loadAnimation(SampleRegistrationActivity.this, android.R.anim.slide_in_left));
        switcher.setOutAnimation(AnimationUtils.loadAnimation(SampleRegistrationActivity.this, android.R.anim.slide_out_right));

        EditText etNewProj = (EditText) findViewById(R.id.etNewProj);
        etNewProj.setOnKeyListener(etNewProj_onKey);

        items = new ArrayList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);

        lstProj = (ListView) findViewById(R.id.listView);
        lstProj.setOnItemClickListener(lstProj_onItemClick);

        populateProjects();

        Button btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(btnBack_onClick);

        Button btnNextID = (Button) findViewById(R.id.btnNextId);
        btnNextID.setOnClickListener(btnNextID_onClick);

        tvProjName = (TextView) findViewById(R.id.tvProjName);
        tvCurrProvider = (TextView) findViewById(R.id.tvCurrentProvider);
        tvCurrGPSDate = (TextView) findViewById(R.id.tvLastDate);
        tvCurrLat = (TextView) findViewById(R.id.tvCurrentLatitude);
        tvCurrLon = (TextView) findViewById(R.id.tvCurrentLongitude);
        tvNextID = (TextView)findViewById(R.id.etNewId);
        etNewSampleType = (EditText)findViewById(R.id.etNewSampleType);
        etNewComment = (EditText)findViewById(R.id.etNewComment);

        // Initialize location manager
        locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        providerEnabled = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!providerEnabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        Criteria criteria = new Criteria();
        locProvider = locManager.getBestProvider(criteria, false);
        if(locProvider == null) {
            Toast.makeText(this, "No location provider available", Toast.LENGTH_SHORT).show();
            exitApp();
            return;
        }

        Location location = locManager.getLastKnownLocation(locProvider);

        if (location != null) {
            tvCurrProvider.setText("Provider: " + locProvider);
            onLocationChanged(location);
        } else {
            tvCurrProvider.setText("Provider not available");
            tvCurrGPSDate.setText("Date not available");
            tvCurrLat.setText("Location not available");
            tvCurrLon.setText("Location not available");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sample_registration, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private View.OnClickListener btnBack_onClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switcher.showPrevious();
        }
    };

    private View.OnClickListener btnNextID_onClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            File file = new File (appDir, tvProjName.getText().toString() + ".txt");
            //Log.i("sampleregistration", "Writing to " + file.getAbsolutePath());
            try {
                FileOutputStream out = new FileOutputStream(file, true);

                TimeZone tz = TimeZone.getTimeZone("UTC");
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                df.setTimeZone(tz);
                String strDateISO = df.format(new Date());

                String currLat = tvCurrLat.getText().toString();
                String currLon = tvCurrLon.getText().toString();
                String nextID = tvNextID.getText().toString();
                String sampleType = etNewSampleType.getText().toString();
                String sampleComment = etNewComment.getText().toString();

                String line = nextID + "|" + strDateISO + "|" + currLat + "|" + currLon + "|" + sampleType + "|" + sampleComment + "\n";
                out.write(line.getBytes());
                out.flush();
                out.close();

                Toast.makeText(SampleRegistrationActivity.this, "Sample ID " + nextID + " stored as " + sampleType, Toast.LENGTH_LONG).show();

                nextId++;
                tvNextID.setText(String.valueOf(nextId));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private AdapterView.OnItemClickListener lstProj_onItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            tvProjName.setText(String.valueOf(parent.getItemAtPosition(position)));

            LineNumberReader lnr = null;
            try {
                lnr = new LineNumberReader(new FileReader(new File(appDir, tvProjName.getText().toString() + ".txt")));
                lnr.skip(Long.MAX_VALUE);
                nextId = lnr.getLineNumber();
                lnr.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            tvNextID.setText(String.valueOf(nextId));
            switcher.showNext();
        }
    };

    private EditText.OnKeyListener etNewProj_onKey = new EditText.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            EditText et = (EditText) v;
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {

                String strNewProj = et.getText().toString();

                File file = new File (appDir, strNewProj + ".txt");
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    String  strUID = UUID.randomUUID().toString() + "\n";
                    out.write(strUID.getBytes());
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                populateProjects();
                et.setText("");

                return true;
            }
            return false;
        }
    };

    /* Request updates at startup */
    @Override
    protected void onResume() {
        super.onResume();
        if(!providerEnabled)
            return;
        locManager.requestLocationUpdates(locProvider, 400, 1, this);
    }

    /* Remove the locationlistener updates when Activity is paused */
    @Override
    protected void onPause() {
        super.onPause();
        if(!providerEnabled)
            return;
        locManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Date now = new Date();
        double lat = (double) (location.getLatitude());
        double lng = (double) (location.getLongitude());

        tvCurrGPSDate.setText(DateFormat.getDateTimeInstance().format(now));
        tvCurrLat.setText(String.valueOf(lat));
        tvCurrLon.setText(String.valueOf(lng));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        tvCurrProvider.setText("Provider: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        tvCurrProvider.setText("Provider not available");
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public File getStorageDir(String appname) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), appname);
        if (!file.mkdirs()) {
            Log.i("sampleregistration", "Directory not created");
        }
        return file;
    }

    public void populateProjects() {
        items.clear();
        File[] listOfFiles = appDir.listFiles();
        for (File file : listOfFiles) {
            if (file.isFile()) {
                int pos = file.getName().lastIndexOf(".");
                String baseName = pos > 0 ? file.getName().substring(0, pos) : file.getName();
                items.add(baseName);
            }
        }
        lstProj.setAdapter(adapter); // FIXME: probably overkill
    }

    public void exitApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}