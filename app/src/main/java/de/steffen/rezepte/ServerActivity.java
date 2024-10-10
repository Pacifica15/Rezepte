package de.steffen.rezepte;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

//todo Mehrere Rezepte auswählen

public class ServerActivity extends AppCompatActivity {

    class Rezepte {
        public String name;
        public String user;
        public String version;
        public String datum;
        public Boolean isInDB;
        public Boolean isNewVersion;
        public Integer alter;
    }

    public class ExpandableListAdapter extends BaseExpandableListAdapter  {

        private Activity context;
        private Map<String, List<Rezepte>> itemcollections;
        private List<String> item;

        public ExpandableListAdapter(Activity context, List<String> item_names, Map<String, List<Rezepte>> collections) {
            this.context = context;
            this.item = item_names;
            this.itemcollections = collections;
        }

        @Override
        public Object getChild(int groupposition, int childposition) {return itemcollections.get(item.get(groupposition)).get(childposition);}

        @Override
        public long getChildId(int groupposition, int childposition) {
            return childposition;
        }

        @Override
        public View getChildView(int groupposition, int childpostion, boolean isLastchild, View convertview, ViewGroup parent) {
            final Rezepte childitem = (Rezepte) getChild(groupposition, childpostion);
            LayoutInflater inflater = context.getLayoutInflater();
            if(convertview==null){
                //convertview = inflater.inflate(R.layout.server_rezeptliste_child_item, null);
                convertview = inflater.inflate(R.layout.server_rezeptliste_child_item_neu, null);
            }
            TextView childTvName = (TextView)convertview.findViewById(R.id.child_text);
            childTvName.setText(childitem.name);
            TextView childTvVersion = (TextView)convertview.findViewById(R.id.child_version);
            childTvVersion.setText(childitem.version);
            TextView childTvUser = (TextView)convertview.findViewById(R.id.child_user);
            childTvUser.setText(childitem.user);
            TextView childTvDatum = (TextView)convertview.findViewById(R.id.child_datum);
            childTvDatum.setText(childitem.datum);
            ImageView childImage = (ImageView) convertview.findViewById(R.id.child_image);
            if (childitem.isInDB) {
                if (childitem.isNewVersion) {
                    childImage.setImageResource(R.drawable.ic_svg_rezept_change);
                } else {
                    childImage.setImageResource(R.drawable.ic_svg_rezept_exists);
                }
            } else {
                if (childitem.alter<=maxRezAlter) {
                    childImage.setImageResource(R.drawable.ic_svg_rezept_neu);
                } else {
                    childImage.setImageResource(R.drawable.ic_svg_rezept);
                }
            }
            //if (childitem.isNewVersion) childImage.setImageResource(R.drawable.ic_svg_rezept_neu);

            return convertview;
        }

        @Override
        public int getChildrenCount(int groupposition) {return itemcollections.get(item.get(groupposition)).size();}

        @Override
        public Object getGroup(int groupPosition) {return item.get(groupPosition);}

        @Override
        public int getGroupCount() {return item.size();}

        @Override
        public long getGroupId(int groupPosition) {return groupPosition;}

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            String itemname = (String) getGroup(groupPosition);
            if(convertView==null){
                LayoutInflater inflater = context.getLayoutInflater();
                if (getChildrenCount(groupPosition)>0) {
                    convertView = inflater.inflate(R.layout.server_rezeptliste_group_item, null);
                } else {
                    convertView = inflater.inflate(R.layout.server_rezeptliste_group_item_empty, null);
                }
            }
            TextView groupTv = (TextView)convertView.findViewById(R.id.group_text);
            groupTv.setText(itemname);
            return convertView;
        }

        @Override
        public boolean hasStableIds() {return false;}

        public void rezeptGelesen(int groupPosition, int childPosition) {
            ((Rezepte) getChild(groupPosition,childPosition)).isInDB=true;
            ((Rezepte) getChild(groupPosition,childPosition)).isNewVersion=false;
            notifyDataSetChanged();
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {return true;}
    }


    private String getB64Auth () {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String login = prefs.getString("e_benutzer", "");
        String pass = prefs.getString("e_passwort", "");
        String source=login+":"+pass;
        String ret="Basic "+ Base64.encodeToString(source.getBytes(),Base64.URL_SAFE|Base64.NO_WRAP);
        return ret;
    }


    ProgressDialog pd;
    List<String> kategorien = new ArrayList<String>();
    Map<String, List<Rezepte>> rezeptListe = new LinkedHashMap<String, List<Rezepte>>();;
    JSONArray json;
    Document rezeptDoc;
    DatenBank dbRezepte;
    String dbPfad;
    Integer maxRezAlter;

    Boolean nurNeue = false;
    Boolean nurNichtGeholt = false;

    ExpandableListView lVRl;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);

        dbPfad = getExternalFilesDir("")+"/rezepte.db";
        dbRezepte = new DatenBank(this, dbPfad, null);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_server);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        lVRl = (ExpandableListView) findViewById(R.id.rezeptliste);
        lVRl.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View view, final int groupPosition, final int childPosition, long id) {
                final String rezept = ((Rezepte)lVRl.getExpandableListAdapter().getChild(groupPosition, childPosition)).name;
                final String pfad = lVRl.getExpandableListAdapter().getGroup(groupPosition).toString();
                AlertDialog.Builder builder = new AlertDialog.Builder(ServerActivity.this);
                builder.setTitle(getText(R.string.importieren));
                builder.setMessage(getString(R.string.importieren_txt,pfad+"/"+rezept));
                builder.setCancelable(false);
                builder.setPositiveButton(getText(R.string.JA), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new GetRezept().execute(pfad, rezept, String.valueOf(groupPosition), String.valueOf(childPosition));
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(getText(R.string.NEIN) , new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
                return true;
            }
        });
        lVRl.setEmptyView(findViewById(R.id.empty));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ServerActivity.this);
        maxRezAlter = Integer.valueOf(prefs.getString("e_tage","14"));
        vertraueAllenServern();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.acticity_server, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == 0) {
            return true;
        }
        if (id == R.id.action_refresh) {
            new GetRezeptListe().execute();
            return true;
        }
        if (id == R.id.action_only_not_saved) {
            nurNichtGeholt = !nurNichtGeholt;
            if(nurNichtGeholt) {
                item.setIcon(R.drawable.ic_svg_rezept_exists);
            }else {
                item.setIcon(R.drawable.ic_svg_rezept_exists_not);
            }

            new GetRezeptListe().execute();
            return true;
        }

        if (id == R.id.action_only_new) {
            nurNeue = !nurNeue;
            if(nurNeue) {
                item.setIcon(R.drawable.ic_svg_rezept_neu);
            }else {
                item.setIcon(R.drawable.ic_svg_rezept_neu_not);
            }

            new GetRezeptListe().execute();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        dbRezepte.close();
        super.onDestroy();
    }


    // Thread zum Lesen der Rezeptliste (wird derzeit nicht verwendet)
    //----------------------------------------------------------------
    /*
    private class GetRezeptListe extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            super.onPreExecute();
            pd = new ProgressDialog(ServerActivity.this);
            pd.setMessage(getString(R.string.server_suche_rezepte));
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... params) {

            HttpsURLConnection connection = null;
            BufferedReader reader = null;
            String retVal = null;

            try {
                rezeptListe.clear();
                kategorien.clear();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ServerActivity.this);
                URL url = new URL(prefs.getString("e_server",getString(R.string._serverAdresse))+"rezeptliste");
                connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", getB64Auth());
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
                    json = new JSONArray(buffer.toString());
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
            if (pd.isShowing()){pd.dismiss();}
            if (result==null){
                 try {
                     for (int i = 0; i < json.length(); i++) {
                         kategorien.add(json.getJSONObject(i).getString("Kategorie"));
                         List<String> rezepte = new ArrayList<String>();
                         JSONArray jsonRezepte = json.getJSONObject(i).getJSONArray("Rezepte");
                         for (int j = 0; j< jsonRezepte.length(); j++){
                             rezepte.add(jsonRezepte.getString(j));
                         }
                         rezeptListe.put(json.getJSONObject(i).getString("Kategorie"), rezepte);
                     }
                    lVRl.setAdapter(new ExpandableListAdapter(ServerActivity.this, kategorien, rezeptListe) {
                    });

                } catch (JSONException e) {

                }
            } else {
                Toast.makeText(ServerActivity.this,result,Toast.LENGTH_LONG).show();
            }
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }*/

    // Thread zum Lesen der Rezeptliste mit Zusatzangaben
    //---------------------------------------------------
    private class GetRezeptListe extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            super.onPreExecute();
            pd = new ProgressDialog(ServerActivity.this);
            pd.setMessage(getString(R.string.server_suche_rezepte));
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... params) {

            HttpsURLConnection connection = null;
            BufferedReader reader = null;
            String retVal = null;

            try {
                rezeptListe.clear();
                kategorien.clear();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ServerActivity.this);
                URL url = new URL(prefs.getString("e_server",getString(R.string._serverAdresse))+"rezeptliste2");
                connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", getB64Auth());
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
                    json = new JSONArray(buffer.toString());
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
            if (pd.isShowing()){pd.dismiss();}
            if (result==null){
                GregorianCalendar today = new GregorianCalendar();
                try {
                    for (int i = 0; i < json.length(); i++) {
                        kategorien.add(json.getJSONObject(i).getString("Kategorie"));
                        List<Rezepte> rezepte = new ArrayList<Rezepte>();

                        JSONArray jsonRezepte = json.getJSONObject(i).getJSONArray("Rezepte");
                        for (int j = 0; j< jsonRezepte.length(); j++){
                            Rezepte rezept = new Rezepte();
                            rezept.name=jsonRezepte.getJSONObject(j).getString("name");
                            rezept.user=jsonRezepte.getJSONObject(j).getString("user");
                            rezept.version="V"+jsonRezepte.getJSONObject(j).getString("version");
                            rezept.datum=jsonRezepte.getJSONObject(j).getString("date");
                            if (rezept.datum.substring(2,3).equals(".") && rezept.datum.substring(5,6).equals(".") && rezept.datum.length()==10) {
                                //GregorianCalender zählt die Monate von 0 .. 11 !!!
                                GregorianCalendar past = new GregorianCalendar( Integer.parseInt(rezept.datum.substring(6,10)),
                                                                                Integer.parseInt(rezept.datum.substring(3,5))-1,
                                                                                Integer.parseInt(rezept.datum.substring(0,2)));
                                long difference = today.getTimeInMillis() - past.getTimeInMillis();
                                rezept.alter=(int)(difference / (1000 * 60 * 60 * 24));
                            } else {
                                rezept.alter=99999;
                            }
                            Cursor rCursor = dbRezepte.db.query("rezepte", DatenBank.RezepteSpalten, "name = '"+rezept.name.substring(0,rezept.name.length()-4)+"'",null,null,null,null);
                            rezept.isInDB=false;
                            rezept.isNewVersion=false;
                            if (rCursor.getCount()>0) {
                                rCursor.moveToFirst();
                                rezept.isInDB=true;
                                if (!jsonRezepte.getJSONObject(j).getString("version").equals("")) {
                                    rezept.isNewVersion = Integer.parseInt(jsonRezepte.getJSONObject(j).getString("version")) > rCursor.getInt(DatenBank.tblRezepte.version.getColumn());
                                }
                            }
                            rCursor.close();
                            if(!((nurNeue && rezept.alter > maxRezAlter) || (nurNichtGeholt && rezept.isInDB))){
                                rezepte.add(rezept);
                            }
                        }
                        rezeptListe.put(json.getJSONObject(i).getString("Kategorie"), rezepte);
                    }
                    lVRl.setAdapter(new ExpandableListAdapter(ServerActivity.this, kategorien, rezeptListe) {
                    });

                } catch (JSONException e) {

                }
            } else {
                Toast.makeText(ServerActivity.this,result,Toast.LENGTH_LONG).show();
            }
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }



    // Thread zum Importieren eines Rezeptes
    //--------------------------------------
    private class GetRezept extends AsyncTask<String, String, String> {

        Integer groupPos;
        Integer childPos;

        protected void onPreExecute() {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            super.onPreExecute();
            pd = new ProgressDialog(ServerActivity.this);
            pd.setMessage(getString(R.string.server_suche_rezepte));
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... params) {

            HttpsURLConnection connection = null;
            BufferedReader reader = null;
            String retVal = null;

            groupPos = Integer.valueOf(params[2]);
            childPos = Integer.valueOf(params[3]);
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ServerActivity.this);
                String restRequest = prefs.getString("e_server",getString(R.string._serverAdresse)) + "rezept?";
                String requestParams = "kategorie="+URLEncoder.encode(params[0],"UTF-8");
                requestParams += "&rezept="+URLEncoder.encode(params[1],"UTF-8");
                requestParams += "&name="+URLEncoder.encode(prefs.getString("e_benutzer","nicht_registriert"),"UTF-8");
                requestParams += "&pw="+URLEncoder.encode(prefs.getString("e_passwort","fehlt"),"UTF-8");
                restRequest += requestParams;
                URL url = new URL(restRequest);
                connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", getB64Auth());
                connection.connect();
                if (connection.getResponseCode() != 200) {
                    retVal=getString(R.string.server_fehler,connection.getResponseCode(),connection.getResponseMessage());
                }
                else {

                    InputStream stream = connection.getInputStream();
                    XMLExport exporter = new XMLExport(ServerActivity.this, dbRezepte.db, ServerActivity.this.getExternalFilesDir("").getPath());
                    exporter.ImportRezept(stream);
                    retVal = getText(R.string.rezept_import).toString();
                }

            } catch (Exception e) {
                retVal = e.getMessage();
            } finally {
                if (connection != null) {connection.disconnect();}
                try {
                    if (reader != null) {reader.close();}
                } catch (IOException e) {

                }
            }
            return retVal;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){pd.dismiss();}
            Toast.makeText(ServerActivity.this,result,Toast.LENGTH_LONG).show();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            //new GetRezeptListe().execute();
            if (result.equals(getText(R.string.rezept_import).toString())) {
                ((ExpandableListAdapter)lVRl.getExpandableListAdapter()).rezeptGelesen(groupPos,childPos);
            }
        }
    }

}
