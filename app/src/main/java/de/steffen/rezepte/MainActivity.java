package de.steffen.rezepte;

import de.steffen.rezepte.BuildConfig;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

//todo speicherberechtigung automatisch freischalten

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, RezeptListeFragment.ListeSelektiertCallback,
                   DatenBank.DataBaseCallback {


    private class ImportDatabaseTask extends AsyncTask<File, String, String> {
        private ProgressDialog progress = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            progress.setMessage(getText(R.string.db_import));
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setIndeterminate(false);
            progress.setProgress(0);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            progress.setMessage(values[0]);
            progress.setMax(Integer.valueOf(values[1]));
            progress.setProgress(Integer.valueOf(values[2]));
        }

        @Override
        protected void onPostExecute(String s) {
            progress.dismiss();
            Toast.makeText(MainActivity.this , s, Toast.LENGTH_LONG).show();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            recreate();
        }

        @Override
        protected String doInBackground(File... params) {
            final XMLExport exporter = new XMLExport(MainActivity.this,dbRezepte.db,MainActivity.this.getExternalFilesDir("").getPath());
            exporter.setProgressCallback(new XMLExport.ProgressCallback() {
                @Override
                public void getProgress(String theAction, int maxValue, int aktValue) {
                    publishProgress(theAction,String.valueOf(maxValue),String.valueOf(aktValue));
                }
            });
            try {
                exporter.ImportDatenbank(params[0]);
                return getText(R.string.db_import_ok).toString();
            }
            catch (Exception e){
                return e.getMessage();
            }
        }
    }

    private class ExportDatabaseTask extends AsyncTask<Void, String, String> {
        private ProgressDialog progress = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            progress.setMessage(getText(R.string.db_export));
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setIndeterminate(false);
            progress.setProgress(0);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            progress.setMessage(values[0]);
            progress.setMax(Integer.valueOf(values[1]));
            progress.setProgress(Integer.valueOf(values[2]));
        }

        @Override
        protected void onPostExecute(String s) {
            progress.dismiss();
            Toast.makeText(MainActivity.this , s, Toast.LENGTH_LONG).show();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        @Override
        protected String doInBackground(Void... params) {
            final XMLExport exporter = new XMLExport(MainActivity.this,dbRezepte.db,MainActivity.this.getExternalFilesDir("").getPath());
            exporter.setProgressCallback(new XMLExport.ProgressCallback() {
                @Override
                public void getProgress(String theAction, int maxValue, int aktValue) {
                    publishProgress(theAction,String.valueOf(maxValue),String.valueOf(aktValue));
                }
            });
            try {
                exporter.ExportDatabase("datenbank");
                return getText(R.string.db_export_ok).toString();
            }
            catch (Exception e){
                return e.getMessage();
            }
        }
    }

    private class Registrierung extends AsyncTask<String, String, String> {
        ProgressDialog pd;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        JSONObject json;
        String name;
        String eMail;
        String pass;
        String task;
        String benachrichtigung;

        protected void onPreExecute() {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage(getString(R.string.server_suche_rezepte));
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... params) {

            HttpsURLConnection connection = null;
            BufferedReader reader = null;
            String retVal = null;

            try {
                String restRequest = prefs.getString("e_server",getString(R.string._serverAdresse));
                String requestParams = "";
                task = params[0];
                name = params[1];
                eMail = params[2];
                pass = params[3];
                benachrichtigung = params[4];

                if(task == "check"){
                    restRequest += "validuser";
                    //requestParams = "?name="+URLEncoder.encode(name,"UTF-8");
                    //requestParams += "&pw="+URLEncoder.encode(pass,"UTF-8");
                    restRequest += requestParams;
                    URL url = new URL(restRequest);
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestProperty("Authorization", getB64AuthDirect(name,pass));
                    connection.connect();
                }
                if(task == "register"){
                    restRequest += "newuser";
                    URL url = new URL(restRequest);
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Authorization", getB64AuthDirect(name,pass));
                    connection.setDoOutput(true);
                    connection.setRequestMethod("PUT");
                    OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                    String body = "{\"name\":\"" +name+"\", \"mail\":\"" +eMail+ "\", \"pw\":\"" +pass+ "\", \"options\":\""+benachrichtigung+"\"}";
                    out.write(body);
                    out.close();
                    connection.connect();
                }
                if(task == "upload"){
                    restRequest += "rezept";
                    //requestParams += "?name="+URLEncoder.encode(name,"UTF-8");
                    //requestParams += "&pw="+URLEncoder.encode(pass,"UTF-8");
                    restRequest += requestParams;
                    URL url = new URL(restRequest);
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestProperty("Content-Type", "application/xml");
                    connection.setRequestProperty("Authorization", getB64AuthfromPrefs());
                    connection.setDoOutput(true);
                    connection.setRequestMethod("PUT");
                    //OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                    new XMLExport(MainActivity.this, dbRezepte.db, MainActivity.this.getExternalFilesDir("").getPath()).ExportRezept(lastRId, connection.getOutputStream());
                    connection.connect();
                }

                switch (connection.getResponseCode()){
                    case  400:
                        retVal = getText(R.string.err_400).toString();
                        break;
                    case  403:
                        if(task == "upload"){
                            retVal = getText(R.string.err_403_put).toString();
                        }else{
                            retVal = getText(R.string.err_403_user).toString();
                        }
                        break;
                    case  404:
                        retVal = getText(R.string.err_404_put).toString();
                        break;
                    case 409:
                        if(task == "upload"){
                            retVal = getText(R.string.err_409_rezept).toString();
                        }else{
                            retVal = getText(R.string.err_409_user).toString();
                        }
                        break;
                    case  415:
                        retVal = getText(R.string.err_415).toString();
                        break;

                    default:
                        //retVal=connection.getResponseMessage();
                        retVal=getString(R.string.server_fehler,connection.getResponseCode(),retVal);
                }

                if (connection.getResponseCode() == 200) {
                    retVal=null;
                    if(task == "check"){
                        InputStream stream = connection.getInputStream();

                        reader = new BufferedReader(new InputStreamReader(stream));

                        StringBuffer buffer = new StringBuffer();
                        String line = "";
                        while ((line = reader.readLine()) != null) {
                            buffer.append(line + "\n");
                        }
                        reader.close();
                        json = new JSONObject(buffer.toString());
                    }
                }

            } catch (Exception e) {
                retVal = e.getMessage();
            } finally {
                try {
                    if (reader != null) {reader.close();}
                } catch (IOException e) {}

                if (connection != null) {connection.disconnect();}
            }
            return retVal;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){pd.dismiss();}
            if (result==null){
                if(registerDialog != null){
                    registerDialog.dismiss();
                }
                if(task == "check"){
                    try {
                        name = json.getString("name");
                        eMail = json.getString("mail");
                    } catch (JSONException e) {
                        Toast.makeText(MainActivity.this, R.string.reg_log_illegal_returncode, Toast.LENGTH_SHORT).show();
                    }
                    prefs.edit().putString("e_benutzer", name).commit();
                    prefs.edit().putString("e_eMail", eMail).commit();
                    prefs.edit().putString("e_passwort", pass).commit();
                    Toast.makeText(MainActivity.this, R.string.reg_log_erfolgreich, Toast.LENGTH_SHORT).show();
                }
                if(task == "register"){
                    prefs.edit().putString("e_benutzer", name).commit();
                    prefs.edit().putString("e_eMail", eMail).commit();
                    prefs.edit().putString("e_passwort", pass).commit();
                    Toast.makeText(MainActivity.this, R.string.reg_reg_erfolgreich, Toast.LENGTH_SHORT).show();
                }
                if(task == "upload"){
                    Toast.makeText(MainActivity.this, R.string.reg_upload_ok, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this,result,Toast.LENGTH_LONG).show();
            }
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }

    private static void vertraueAllenServern() {
        // Erstellen eines TrustManagers der keine Zertifikate prüft
        TrustManager[] vertraueAllenZertifikaten = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }
        } };

        // und den TrustManager installieren
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, vertraueAllenZertifikaten, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class GetNews extends AsyncTask<String, String, String> {

        String newsText;
        String lastVersion;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        protected void onPreExecute() {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            super.onPreExecute();
        }

        protected String doInBackground(String... params) {

            HttpsURLConnection connection = null;
            BufferedReader reader = null;
            String retVal = null;


            try {
                URL url = new URL(prefs.getString("e_server",getString(R.string._serverAdresse))+
                                    "news?version="+URLEncoder.encode(BuildConfig.VERSION_NAME,"UTF-8"));
                connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", getB64AuthfromPrefs());
                connection.connect();

                if (connection.getResponseCode() != 200) {
                    retVal=getString(R.string.server_fehler,connection.getResponseCode(),connection.getResponseMessage());
                }
                else {
                    InputStream stream = connection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuffer buffer = new StringBuffer();
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line + "\n");
                    }
                    reader.close();
                    JSONObject json = new JSONObject(buffer.toString());
                    newsText = json.getString("txt");
                    lastVersion=json.getString("LastVersion");
                }

            } catch (Exception e) {
                retVal = e.getMessage();
            } finally {
                try {
                    if (reader != null) {reader.close();}
                } catch (IOException e) {

                }
                if (connection != null) {connection.disconnect();}
            }
            return retVal;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result==null){
                if(!prefs.getString("e_lastVersion", "").equals(lastVersion)) prefs.edit().putString("e_lastVersion", lastVersion).commit();
                if(!prefs.getString("e_lastNews", "").equals(newsText+" ")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(R.string.news);
                    builder.setMessage(newsText);
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.show();
                    prefs.edit().putString("e_lastNews", newsText+" ").commit();
                }
            }else {
                Toast.makeText(MainActivity.this,result,Toast.LENGTH_LONG).show();
            }
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }

    private class GetNewApp extends AsyncTask<String, String, String> {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        protected void onPreExecute() {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            super.onPreExecute();
        }

        protected String doInBackground(String... params) {

            HttpsURLConnection connection = null;
            String retVal = null;
            InputStream input = null;
            OutputStream output = null;


            try {
                URL url = new URL(prefs.getString("e_server",getString(R.string._serverAdresse))+ "files/SteffensRezepte.apk");
                connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", getB64AuthfromPrefs());
                connection.connect();

                if (connection.getResponseCode() != 200) {
                    retVal=getString(R.string.server_fehler,connection.getResponseCode(),connection.getResponseMessage());
                }
                else {
                    int fileLength = connection.getContentLength();
                    input = connection.getInputStream();
                    output = new FileOutputStream(getExternalFilesDir("")+"/SteffensRezepte.apk");

                    byte data[] = new byte[4096];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        total += count;
                        // publishing the progress....
                        // if (fileLength > 0) publishProgress((int) (total * 100 / fileLength));
                        output.write(data, 0, count);
                    }
                }

            } catch (Exception e) {
                retVal = e.getMessage();
            } finally {
                try {
                    if (input != null) input.close();
                    if (output != null) output.close();
                } catch (IOException e) {

                }
                if (connection != null) {connection.disconnect();}
            }
            return retVal;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result==null){
                // Neue Version erfolgreich heruntergeladen, jetzt installieren
                Toast.makeText(MainActivity.this, "Download complete ... installing ", Toast.LENGTH_SHORT).show();
                //start the installation of the latest version
                Intent installIntent = new Intent(Intent.ACTION_VIEW);
                installIntent.setDataAndType(Uri.fromFile(new File(getExternalFilesDir("")+"/SteffensRezepte.apk")), "application/vnd.android.package-archive");
                installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(installIntent);
                MainActivity.this.finish();
            }else {
                Toast.makeText(MainActivity.this,result,Toast.LENGTH_LONG).show();
            }
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }


    private String getB64AuthfromPrefs () {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String login = prefs.getString("e_benutzer", "");
        String pass = prefs.getString("e_passwort", "");
        String source=login+":"+pass;
        String ret="Basic "+ Base64.encodeToString(source.getBytes(),Base64.URL_SAFE|Base64.NO_WRAP);
        return ret;
    }

    private String getB64AuthDirect (String login, String pass) {
        String source=login+":"+pass;
        String ret="Basic "+ Base64.encodeToString(source.getBytes(),Base64.URL_SAFE|Base64.NO_WRAP);
        return ret;
    }
    String dbPfad;
    Menu mmenu;
    public DatenBank dbRezepte;
    Long lastRId = Long.valueOf(-1);
    String ServerErgebnis = "";
    AlertDialog registerDialog = null;

    //private DownloadManager downloadManager;
    //private long downloadReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);

        dbPfad = getExternalFilesDir("")+"/rezepte.db";
        dbRezepte = new DatenBank(this, dbPfad, null);

        super.onCreate(savedInstanceState);
        vertraueAllenServern();
        PreferenceManager.setDefaultValues(this,R.xml.einstellungen,false);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            public void onDrawerOpened(View drawerView){
                super.onDrawerOpened(drawerView);
                if (findViewById(R.id.nav_titel)!=null)
                    ((TextView)findViewById(R.id.nav_titel)).setText(MainActivity.this.getString(R.string.steffens_rezepte,BuildConfig.VERSION_NAME));
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                if(!prefs.getString("e_lastVersion", "").equals(BuildConfig.VERSION_NAME)) {
                    ImageView navIcon = ((ImageView)findViewById(R.id.nav_icon));
                    if (navIcon != null) {
                        navIcon.setImageResource(R.drawable.ic_svg_muetze_download);
                        navIcon.setClickable(true);
                        navIcon.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                new GetNewApp().execute();
                                Toast.makeText(MainActivity.this, "Downloading new App ...", Toast.LENGTH_SHORT).show();
                            }
                        });
                        ((TextView) findViewById(R.id.nav_updateText)).setVisibility(View.VISIBLE);
                    }
                }
            }
        };
        drawer.setDrawerListener(toggle);

        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FragmentTransaction transaction;

         if (savedInstanceState == null) {
             transaction = getSupportFragmentManager().beginTransaction();
             SlidingTabs fragment = new SlidingTabs();
             transaction.add(R.id.Frame1, fragment,SlidingTabs.TAG);
             transaction.commit();
         } else {
             // Zunächst den BackStack leeren
             // Und alle erhalten gebliebenen Fragments entfernen
             int back = getSupportFragmentManager().getBackStackEntryCount();
             for (int i = 0 ; i<back; i++){
                 getSupportFragmentManager().popBackStackImmediate();
             }
             Fragment f = getSupportFragmentManager().findFragmentByTag(EinkaufsListe.TAG);
             if (f != null) {
                 getSupportFragmentManager().beginTransaction().remove(f).commitNow();
             }
             f = getSupportFragmentManager().findFragmentByTag(EinRezeptFragment.TAG);
             if (f != null) {
                 getSupportFragmentManager().beginTransaction().remove(f).commitNow();
             }

             // jetzt alle Fragments, passend zum aktuellen Layout neu aufbauen
             Fragment Erf = getSupportFragmentManager().findFragmentByTag(EinRezeptFragment.TAG);
             Boolean ErfVisible = false;
             Fragment Ekl = getSupportFragmentManager().findFragmentByTag(EinkaufsListe.TAG);
             View v = this.findViewById(R.id.Frame2);

             if (savedInstanceState.getBoolean("EkListe")) {
                 if (Ekl == null) {
                     Ekl = new EinkaufsListe();
                 }
             }
             ErfVisible = savedInstanceState.getBoolean("EinRezeptVisible");
             lastRId = savedInstanceState.getLong("LastRId");
             if (savedInstanceState.getBoolean("EinRezept")) {
                 if (Erf == null) {
                     Bundle bundle = new Bundle();
                     bundle.putLong("Rezept_id", lastRId);
                     Erf = new EinRezeptFragment();
                     Erf.setArguments(bundle);
                 }
             }

             if (Erf != null || Ekl != null) {
                 int frameID = R.id.Frame1;
                 if (v != null) {
                     frameID = R.id.Frame2;
                 }
                 if (ErfVisible) {
                     if (Ekl != null) {
                         transaction = getSupportFragmentManager().beginTransaction();
                         if (v != null) {
                             transaction.add(frameID, Ekl, EinkaufsListe.TAG);
                         } else {
                             transaction.replace(frameID, Ekl, EinkaufsListe.TAG);
                         }
                         transaction.addToBackStack(EinkaufsListe.TAG);
                         transaction.commit();
                     }
                     if (Erf != null) {
                         transaction = getSupportFragmentManager().beginTransaction();
                         if (v != null && Ekl == null) {
                             transaction.add(frameID, Erf, EinRezeptFragment.TAG);
                         } else {
                             transaction.replace(frameID, Erf, EinRezeptFragment.TAG);
                         }
                         transaction.addToBackStack(EinRezeptFragment.TAG);
                         transaction.commit();
                     }
                 } else {
                     if (Erf != null) {
                         transaction = getSupportFragmentManager().beginTransaction();
                         if (v != null) {
                             transaction.add(frameID, Erf, EinRezeptFragment.TAG);
                         } else {
                             transaction.replace(frameID, Erf, EinRezeptFragment.TAG);
                         }
                         transaction.addToBackStack(EinRezeptFragment.TAG);
                         transaction.commit();
                     }
                     if (Ekl != null) {
                         transaction = getSupportFragmentManager().beginTransaction();
                         if (v != null && Erf == null) {
                             transaction.add(frameID, Ekl, EinkaufsListe.TAG);
                         } else {
                             transaction.replace(frameID, Ekl, EinkaufsListe.TAG);
                         }
                         transaction.addToBackStack(EinkaufsListe.TAG);
                         transaction.commit();
                     }
                 }
             }
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EinRezeptFragment er = (EinRezeptFragment) getSupportFragmentManager().findFragmentByTag(EinRezeptFragment.TAG);
                if(er!=null) {
                    er.EinkaufsListeAktualisieren();
                    Snackbar.make(view, getText(R.string.ek_liste_hinzugefuegt), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            }
        });
        if(savedInstanceState == null) {
            new GetNews().execute();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SlidingTabs st =  (SlidingTabs) getSupportFragmentManager().findFragmentByTag(SlidingTabs.TAG);
        if (st!=null && st.isVisible()) st.PopulatePager();
        EinkaufsListe ek =  (EinkaufsListe) getSupportFragmentManager().findFragmentByTag(EinkaufsListe.TAG);
        if (ek!=null && ek.isVisible()) ek.ReloadEinkaufsListe();
    }

    @Override
    protected void onDestroy() {
        dbRezepte.close();
        //this.unregisterReceiver(downloadReceiver);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Boolean EkListeVisible = false;
        Boolean EinRezeptVisible = false;
        if(getSupportFragmentManager().getBackStackEntryCount() >0) {
            if (EinkaufsListe.TAG.toString().equals(getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).getName())) {
                EkListeVisible = true;
            } else {
                EinRezeptVisible = true;
            }
        }

        Boolean EkListe = (getSupportFragmentManager().findFragmentByTag(EinkaufsListe.TAG) != null);
        Boolean EinRezept = (getSupportFragmentManager().findFragmentByTag(EinRezeptFragment.TAG) != null);
        //outState.putBoolean("quer", findViewById(R.id.TestFrameQuer) != null);
        outState.putBoolean("quer", findViewById(R.id.Frame2) != null);
        outState.putBoolean("EkListe", EkListe);
        outState.putBoolean("EkListeVisible", EkListeVisible);
        outState.putBoolean("EinRezept", EinRezept);
        outState.putBoolean("EinRezeptVisible", EinRezeptVisible);
        outState.putLong("LastRId",lastRId);

        getIntent().putExtra("EkListe", EkListe);
        getIntent().putExtra("EinRezept", EinRezept);
        getIntent().putExtra("EinRezeptVisible", EinRezeptVisible);
        getIntent().putExtra("LastRId",lastRId);
    }

    @Override
    public void onBackPressed() {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(getSupportFragmentManager().getBackStackEntryCount() == 0){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.appverlassen_titel);
                builder.setMessage(R.string.appverlassen_text);
                builder.setPositiveButton(R.string.JA, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.NEIN, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }else{
               /* FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                if (fab != null) {
                    fab.setVisibility(View.GONE);
                }*/
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mmenu=menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == 0) {
            return true;
        }
        if (id == R.id.action_import) {
            final XMLExport exporter = new XMLExport(this,dbRezepte.db,this.getExternalFilesDir("").getPath());
            FileChooser fc = new FileChooser(this,"XML");
            fc.setFileListener(new FileChooser.FileSelectedListener() {
                @Override
                public void fileSelected(Context con, File file) {
                    try {
                        Toast.makeText(con ,getText(R.string.rezept_import_start), Toast.LENGTH_LONG).show();
                        exporter.ImportRezept(file);
                        Toast.makeText(con ,getText(R.string.rezept_import), Toast.LENGTH_LONG).show();
                        recreate();
                    }
                    catch (Exception e) {
                        Log.e("Import",e.getMessage());
                        Toast.makeText(con ,e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
            fc.showDialog();
            return true;
        }
        if (id == R.id.action_upload){
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            new Registrierung().execute("upload", prefs.getString("e_benutzer",""),"", prefs.getString("e_passwort",""), "");
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_rezepte) {
            Fragment f;
            FragmentTransaction ta = getSupportFragmentManager().beginTransaction();
            f =  getSupportFragmentManager().findFragmentByTag(EinRezeptFragment.TAG);
            if (f!=null) {
                ta.remove(f);
                getSupportFragmentManager().popBackStack();
            }
            f =  getSupportFragmentManager().findFragmentByTag(EinkaufsListe.TAG);
            if (f!=null) {
                ta.remove(f);
                getSupportFragmentManager().popBackStack();
            }
            ta.commit();
        }
        else if (id == R.id.nav_ekliste) {
            EinkaufsListe fragment = new EinkaufsListe();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            //if(findViewById(R.id.TestFrameQuer)!= null){
            if(findViewById(R.id.Frame2)!= null){
                transaction.replace(R.id.Frame2, fragment,EinkaufsListe.TAG);
            }else{
                transaction.replace(R.id.Frame1, fragment,EinkaufsListe.TAG);
            }
            transaction.addToBackStack(EinkaufsListe.TAG);
            transaction.commit();
            /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setVisibility(View.GONE);*/
        }

        else if (id == R.id.nav_einstellung) {
            Intent intent = new Intent(this, Einstellungen.class);
            startActivity(intent);
        }
        else if (id == R.id.nav_db_export) {
            new ExportDatabaseTask().execute();
        }
        else if (id == R.id.nav_db_import) {
            FileChooser fc = new FileChooser(this,"XML");
            fc.setFileListener(new FileChooser.FileSelectedListener() {
                @Override
                public void fileSelected(final Context con, final File file) {
                    new ImportDatabaseTask().execute(file);
                }
            });
            fc.showDialog();
        }
        else if (id == R.id.nav_zutaten) {
            Intent intent = new Intent(this, EditData.class);
            intent.putExtra("id", "zutaten");
            startActivity(intent);
        }
        else if (id == R.id.nav_registrieren) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getText(R.string.m_anmelden));
            ScrollView sv = new ScrollView(this);
            LinearLayout ll = new LinearLayout(this);
            ll.setOrientation(LinearLayout.VERTICAL);

            //Nur Abstand:
            final TextView tv0 = new TextView(this);
            tv0.setText("");
            ll.addView(tv0);

            final TextView tv1 = new TextView(this);
            tv1.setText(getText(R.string.reg_name_oder_eMail));
            ll.addView(tv1);
            final EditText name = new EditText(this);
            name.setText(prefs.getString("e_benutzer",""));
            name.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
            ll.addView(name);

            final TextView tv2 = new TextView(this);
            tv2.setText(getText(R.string.reg_email));
            tv2.setVisibility(View.GONE);
            ll.addView(tv2);
            final EditText  eMail= new EditText(this);
            eMail.setText(prefs.getString("e_eMail",""));
            eMail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            eMail.setVisibility(View.GONE);
            ll.addView(eMail);

            TextView tv3 = new TextView(this);
            tv3.setText(getText(R.string.reg_pass1));
            ll.addView(tv3);
            final EditText  pass1= new EditText(this);
            pass1.setText(prefs.getString("e_passwort",""));
            pass1.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ll.addView(pass1);

            final TextView tv4 = new TextView(this);
            tv4.setText(getText(R.string.reg_pass2));
            tv4.setVisibility(View.GONE);
            ll.addView(tv4);
            final EditText  pass2= new EditText(this);
            pass2.setText(prefs.getString("e_passwort",""));
            pass2.setTransformationMethod(PasswordTransformationMethod.getInstance());
            pass2.setVisibility(View.GONE);
            ll.addView(pass2);

            LinearLayout ll2 = new LinearLayout(this);
            final CheckBox cb = new CheckBox(this);
            cb.setChecked(true);
            cb.setVisibility(View.GONE);
            ll2.addView(cb);
            ll2.setOrientation(LinearLayout.HORIZONTAL);
            final TextView tv5 = new TextView(this);
            tv5.setText(getText(R.string.reg_info));
            tv5.setVisibility(View.GONE);
            ll2.addView(tv5);
            ll.addView(ll2);

            sv.addView(ll);
            builder.setView(sv, 50, 0, 50, 0);
            builder.setPositiveButton(R.string.reg_registrieren, null);
            builder.setNegativeButton(R.string.reg_login, null);
            builder.setNeutralButton(R.string.abbrechen, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            registerDialog = builder.create();
            registerDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(final DialogInterface dialog) {

                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(pass2.getVisibility()==View.GONE){
                                tv1.setText(R.string.reg_name);

                                tv2.setVisibility(View.VISIBLE);
                                eMail.setVisibility(View.VISIBLE);

                                tv4.setVisibility(View.VISIBLE);
                                pass2.setVisibility(View.VISIBLE);

                                tv5.setVisibility(View.VISIBLE);
                                cb.setVisibility(View.VISIBLE);

                                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(View.GONE);
                                ((AlertDialog) dialog).setTitle(R.string.reg_registrieren);
                            }else {
                                if(name.getText().toString().equals("")|| eMail.getText().toString().equals("")||
                                        pass1.getText().toString().equals("") || pass2.getText().toString().equals("")){

                                    Toast.makeText(MainActivity.this,R.string.reg_alle_ausfüllen,Toast.LENGTH_SHORT).show();
                                }else {
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(eMail.getText()).matches()) {
                                        Toast.makeText(MainActivity.this, R.string.reg_email_falsch, Toast.LENGTH_SHORT).show();
                                    } else {
                                        if (pass1.getText().toString().equals("") || !pass1.getText().toString().equals(pass2.getText().toString())) {
                                            Toast.makeText(MainActivity.this, R.string.reg_pass_falsch, Toast.LENGTH_SHORT).show();
                                        } else {
                                            if(cb.isChecked()) {
                                                new Registrierung().execute("register", name.getText().toString(), eMail.getText().toString(), pass1.getText().toString(), "/M+");
                                            }else {
                                                new Registrierung().execute("register", name.getText().toString(), eMail.getText().toString(), pass1.getText().toString(), "/M-");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });

                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(name.getText().toString().equals("")|| pass1.getText().toString().equals("")){
                                Toast.makeText(MainActivity.this,R.string.reg_alle_ausfüllen,Toast.LENGTH_SHORT).show();
                            }else{
                                new Registrierung().execute("check", name.getText().toString(), "", pass1.getText().toString(), "");
                            }
                        }
                    });

                }
            });

        registerDialog.show();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void Selektiert(long Rid) {
        //View v = this.findViewById(R.id.TestFrameQuer);
        View v = this.findViewById(R.id.Frame2);

        lastRId = Rid;

        FragmentManager fm = getSupportFragmentManager();
        EinRezeptFragment fragment = (EinRezeptFragment) fm.findFragmentByTag(EinRezeptFragment.TAG);

        if(fragment != null){
            fragment.aktualisieren(Rid);
        }else{
            Bundle bundle = new Bundle();
            bundle.putLong("Rezept_id", Rid);
            fragment = new EinRezeptFragment();
            fragment.setArguments(bundle);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if(v != null){
                if(fm.findFragmentByTag(EinkaufsListe.TAG)!= null){
                    transaction.replace(R.id.Frame2, fragment,EinRezeptFragment.TAG);
                }else{
                    transaction.add(R.id.Frame2, fragment,EinRezeptFragment.TAG);
                }
            }else{
                transaction.replace(R.id.Frame1, fragment,EinRezeptFragment.TAG);
            }
            transaction.addToBackStack(EinRezeptFragment.TAG);
            transaction.commit();
            /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setVisibility(View.VISIBLE);*/
        }
    }

    @Override
    public DatenBank getDatenBank() {
     return dbRezepte;
    }
    @Override
    public String getDBPfad() {
        return dbPfad;
    }


    /*
    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            //check if the broadcast message is for our Enqueued download
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if(downloadReference == referenceId){
                Uri downloadUri = downloadManager.getUriForDownloadedFile(downloadReference);
                if (downloadUri==null) {
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(downloadReference);
                    Cursor c = downloadManager.query(q);
                    int errCode=0;
                    if (c.moveToFirst()) {
                        errCode = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                    }
                    Toast.makeText(MainActivity.this, "Download FEHLER "+errCode +" at " + downloadUri, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Download complete " + downloadUri, Toast.LENGTH_LONG).show();
                    //start the installation of the latest version
                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                    installIntent.setDataAndType(downloadUri, "application/vnd.android.package-archive");
                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(installIntent);
                }

            }
        }
    };
    */
}
