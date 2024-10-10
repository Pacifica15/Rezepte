package de.steffen.rezepte;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

//todo einheit aus Zutat auslesen

public class EinRezeptEdit extends AppCompatActivity {

    public static final int REQUEST_IMAGE = 10001;
    public static final int REQUEST_NEW_ZUTAT = 10002;
    boolean newImage = false;
    Bitmap newPhoto;

    static class ZutatenViewHolder {
        public ImageButton btn;
        public EditText menge;
        public TextWatcher mTextWatcher;
        public AutoCompleteTextView einheit;
        public TextWatcher eTextWatcher;
        public AutoCompleteTextView zutat;
        public TextWatcher zTextWatcher;
    }

    static class MainViewHolder{
        public ImageView bild;
        public EditText name;
        public Spinner kategorie;
        public EditText portionen;
        public EditText[] schritt = new EditText[5];
        public LinearLayout[] header = new LinearLayout[5];
        public TextView schritt1_bez;
        public NotEmptyTextWatcher[] watcher = new NotEmptyTextWatcher[5];
        public ListView[] zListe = new ListView[5];
        public EditText zubereitung;
    }

    class ZutatenValidator implements AutoCompleteTextView.Validator {
        @Override
        public boolean isValid(CharSequence text) {
            for (int i=0; i<ZutatenFromDB.size();i++) {
                if (text.toString().equals(ZutatenFromDB.get(i).bez_singular) || text.toString().equals(ZutatenFromDB.get(i).bez_plural)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public CharSequence fixText(CharSequence invalidText) {
            return "";
        }
    }

    class EinheitenValidator implements AutoCompleteTextView.Validator {
        @Override
        public boolean isValid(CharSequence text) {
            for (int i=0; i<EinheitenFromDB.size();i++) {
                if (text.toString().equals(EinheitenFromDB.get(i).einh_singular) || text.toString().equals(EinheitenFromDB.get(i).einh_plural)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public CharSequence fixText(CharSequence invalidText) {
            return "";
        }
    }

    class NotEmptyTextWatcher implements TextWatcher {
        String lastText;
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            lastText = s.toString();
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) {
            dataChanged = true;
            if (s.toString().equals("")) {
                s.append(lastText);
                Toast.makeText(EinRezeptEdit.this, getText(R.string.nicht_leer), Toast.LENGTH_SHORT).show();
            }
        }
    }

    class ChangedTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) {
            dataChanged = true;
        }
    }

    class ZutatenSimpleAdapter extends SimpleAdapter {

        public ZutatenSimpleAdapter (Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            long valueToFind;
            int p;
            View v = convertView;
            ArrayAdapter adapter;
            if (v == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                v = inflater.inflate(R.layout.zutaten_datensatz_edit, parent, false);
                ZutatenViewHolder holder = new ZutatenViewHolder();
                holder.btn = (ImageButton) v.findViewById(R.id.btn_delete);
                holder.menge = (EditText) v.findViewById(R.id.menge);
                holder.einheit = (AutoCompleteTextView) v.findViewById(R.id.einheit);
                holder.zutat = (AutoCompleteTextView) v.findViewById(R.id.zutat);

                ArrayAdapter<Einheiten> einhAdapter = new ArrayAdapter<Einheiten>(getApplicationContext(),R.layout.my_autocomplete_dropdown,EinheitenFromDB);
                holder.einheit.setAdapter(einhAdapter);
                holder.einheit.setValidator(new EinheitenValidator());
                holder.einheit.setThreshold(1);

                ArrayAdapter<Zutaten> zutAdapter = new ArrayAdapter<Zutaten>(getApplicationContext(),R.layout.my_autocomplete_dropdown,ZutatenFromDB);
                holder.zutat.setAdapter(zutAdapter);
                holder.zutat.setValidator(new ZutatenValidator());
                holder.zutat.setThreshold(1);

                v.setTag(holder);
            }

            final ZutatenViewHolder holder = (ZutatenViewHolder) v.getTag();
            final ArrayMap map = (ArrayMap) getItem(position);

            if (holder.mTextWatcher != null) holder.menge.removeTextChangedListener(holder.mTextWatcher);
            if (holder.eTextWatcher != null) holder.einheit.removeTextChangedListener(holder.eTextWatcher);
            if (holder.zTextWatcher != null) holder.zutat.removeTextChangedListener(holder.zTextWatcher);

            DecimalFormat decimalFormat = new DecimalFormat("#.###");
            if ((Double) map.get("menge")==0) {holder.menge.setText("");} else {holder.menge.setText(decimalFormat.format((Double)map.get("menge")));}

            valueToFind = (Long) map.get("einheit");
            holder.einheit.setText("");
            adapter = (ArrayAdapter) holder.einheit.getAdapter();
            for (p = 0; p < adapter.getCount(); p++) {
                Einheiten mEinheit = (Einheiten) adapter.getItem(p);
                if(mEinheit.id == valueToFind) {
                    if ((Double) map.get("menge")!=1) {
                        mEinheit.setPlural(true);
                    } else {
                        mEinheit.setPlural(false);
                    }
                    holder.einheit.setText(mEinheit.toString());
                    break;
                }
            }

            valueToFind = (Long) map.get("zutat");
            holder.zutat.setText("");
            adapter = (ArrayAdapter) holder.zutat.getAdapter();
            for (p = 0; p < adapter.getCount(); p++) {
                Zutaten mZutat = (Zutaten) adapter.getItem(p);
                if(mZutat.id == valueToFind) {
                    if ((Double) map.get("menge")!=1) {
                        mZutat.setPlural(true);
                    } else {
                        mZutat.setPlural(false);
                    }
                    holder.zutat.setText(mZutat.toString());
                    break;
                }
            }

            holder.mTextWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    dataChanged = true;
                    if (s.toString().equals("")) {
                        map.put("menge", Double.valueOf(0));
                    } else {
                        map.put("menge", Double.valueOf(s.toString().replace(",",".")));
                    }
                }
            };
            holder.menge.addTextChangedListener(holder.mTextWatcher);

            holder.eTextWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    dataChanged = true;
                    long iDFound = -1;
                    for (int i=0; i<EinheitenFromDB.size();i++) {
                        if (s.toString().equals(EinheitenFromDB.get(i).einh_singular) ||
                                s.toString().equals(EinheitenFromDB.get(i).einh_plural)) {
                            iDFound=EinheitenFromDB.get(i).id;
                            break;
                        }
                    }
                    map.put("einheit", iDFound);
                }
            };
            holder.einheit.addTextChangedListener(holder.eTextWatcher);

            holder.zTextWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    dataChanged = true;
                    long iDFound = -1;
                    for (int i=0; i<ZutatenFromDB.size();i++) {
                        if (s.toString().equals(ZutatenFromDB.get(i).bez_singular) ||
                                s.toString().equals(ZutatenFromDB.get(i).bez_plural)) {
                            iDFound=ZutatenFromDB.get(i).id;
                            break;
                        }
                    }
                    map.put("zutat", iDFound);
                }
            };
            holder.zutat.addTextChangedListener(holder.zTextWatcher);

            holder.btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dataChanged = true;
                        if(parent.getId() == R.id.zutaten){zListen.get(0).remove(position);}
                        if(parent.getId() == R.id.zutaten2){zListen.get(1).remove(position);}
                        if(parent.getId() == R.id.zutaten3){zListen.get(2).remove(position);}
                        if(parent.getId() == R.id.zutaten4){zListen.get(3).remove(position);}
                        if(parent.getId() == R.id.zutaten5){zListen.get(4).remove(position);}
                        ListAdapter la = ((ListView) parent).getAdapter();
                        ((ListView) parent).setAdapter(null);
                        ((ListView) parent).setAdapter(la);
                        ((BaseAdapter) ((ListView) parent).getAdapter()).notifyDataSetChanged();
                        setListViewHeightBasedOnChildren((ListView) parent);
                    }
                });
            return v;
        }
    }

    class Kategorien{
        public long id;
        public String kategorie;

        Kategorien(long id, String kategorie){
            this.id = id;
            this.kategorie = kategorie;
        }
        @Override
        public String toString() {
            return this.kategorie;
        }
    }

    class Zutaten{
        public long id;
        public String bez_plural;
        public String bez_singular;
        public boolean isPlural = false;

        Zutaten(long id, String bez_singular, String bez_plural){
            this.id = id;
            this.bez_plural = bez_plural;
            this.bez_singular = bez_singular;
        }
        public void setPlural (boolean plural) {
           this.isPlural=plural;
        }

        @Override
        public String toString() {
            if (this.isPlural) {
                return this.bez_plural;
            }
            else {
                return this.bez_singular;
            }
        }
    }

    class Einheiten{
        public long id;
        public String einh_plural;
        public String einh_singular;
        public boolean isPlural = false;

        Einheiten(long id, String einh_singular, String einh_plural){
            this.id = id;
            this.einh_plural = einh_plural;
            this.einh_singular = einh_singular;
        }
        public void setPlural (boolean plural) {
            this.isPlural=plural;
        }

        @Override
        public String toString() {
            if (this.isPlural) {
                return this.einh_plural;
            }
            else {
                return this.einh_singular;
            }
        }
    }

    String dbPfad;
    public DatenBank dbRezepte;
    Menu mmenu;
    long RId;
    Cursor rezepteCursor;
    boolean isNew;
    int anz_schritte;
    boolean dataChanged = false;
    int rezVersion;

    List<ArrayMap<String, String>>listZutaten1 = new ArrayList<ArrayMap<String, String>>();
    List<ArrayMap<String, String>>listZutaten2 = new ArrayList<ArrayMap<String, String>>();
    List<ArrayMap<String, String>>listZutaten3 = new ArrayList<ArrayMap<String, String>>();
    List<ArrayMap<String, String>>listZutaten4 = new ArrayList<ArrayMap<String, String>>();
    List<ArrayMap<String, String>>listZutaten5 = new ArrayList<ArrayMap<String, String>>();
    List<List<ArrayMap<String, String>>> zListen = new ArrayList<List<ArrayMap<String, String>>>();
    List<Einheiten> EinheitenFromDB = new ArrayList<Einheiten>();
    List<Kategorien> KategorienFromDB = new ArrayList<Kategorien>();
    List<Zutaten> ZutatenFromDB = new ArrayList<Zutaten>();

    List<EditText> uListe = new ArrayList<EditText>();
    String[] arbeitsSchritte = new String[5];
    ZutatenSimpleAdapter[] zAdapter = new ZutatenSimpleAdapter[5];

    final MainViewHolder mHolder = new MainViewHolder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);

        dbPfad = getExternalFilesDir("") + "/rezepte.db";
        dbRezepte = new DatenBank(this, dbPfad, null);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.ein_rezept_edit);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Liste aller gespeicherten Kategorien einlesen
        Cursor kategorien = dbRezepte.db.rawQuery("SELECT _id, kategorie FROM kategorien",null);
        //EinheitenFromDB.add(new Einheiten(-1,"",""));
        while (kategorien.moveToNext()) {
            KategorienFromDB.add(new Kategorien(kategorien.getLong(0),kategorien.getString(1)));
        }
        kategorien.close();
        //Liste aller gespeicherten Einheiten einlesen
        Cursor einheiten = dbRezepte.db.rawQuery("SELECT _id, einh_singular, einh_plural FROM einheiten",null);
        //EinheitenFromDB.add(new Einheiten(-1,"",""));
        while (einheiten.moveToNext()) {
            EinheitenFromDB.add(new Einheiten(einheiten.getLong(0),einheiten.getString(1), einheiten.getString(2)));
        }
        einheiten.close();
        //Liste aller gespeicherten Zutaten einlesen
        final Cursor zutaten = dbRezepte.db.rawQuery("SELECT _id, bez_singular, bez_plural FROM zutaten",null);
        //ZutatenFromDB.add(new Zutaten(-1,"",""));
        while (zutaten.moveToNext()) {
            ZutatenFromDB.add(new Zutaten (zutaten.getLong(0), zutaten.getString(1), zutaten.getString(2)));
        }
        zutaten.close();


        zListen.add(listZutaten1);
        zListen.add(listZutaten2);
        zListen.add(listZutaten3);
        zListen.add(listZutaten4);
        zListen.add(listZutaten5);

        uListe.add((EditText) findViewById(R.id.ueberschriftzutaten1));
        uListe.add((EditText) findViewById(R.id.ueberschriftzutaten2));
        uListe.add((EditText) findViewById(R.id.ueberschriftzutaten3));
        uListe.add((EditText) findViewById(R.id.ueberschriftzutaten4));
        uListe.add((EditText) findViewById(R.id.ueberschriftzutaten5));

        mHolder.zListe[0] = (ListView) findViewById(R.id.zutaten);
        mHolder.zListe[1] = (ListView) findViewById(R.id.zutaten2);
        mHolder.zListe[2] = (ListView) findViewById(R.id.zutaten3);
        mHolder.zListe[3] = (ListView) findViewById(R.id.zutaten4);
        mHolder.zListe[4] = (ListView) findViewById(R.id.zutaten5);

        mHolder.header[0] = (LinearLayout) findViewById(R.id.schritt1);
        mHolder.header[1] = (LinearLayout) findViewById(R.id.schritt2);
        mHolder.header[2] = (LinearLayout) findViewById(R.id.schritt3);
        mHolder.header[3] = (LinearLayout) findViewById(R.id.schritt4);
        mHolder.header[4] = (LinearLayout) findViewById(R.id.schritt5);

        mHolder.bild = (ImageView) findViewById(R.id.RezeptBild);
        mHolder.name = (EditText)findViewById(R.id.RezeptName);
        mHolder.portionen = (EditText)findViewById(R.id.portionen);
        mHolder.zubereitung = (EditText)findViewById(R.id.zubereitung);
        mHolder.kategorie = (Spinner) findViewById(R.id.kategorie);
        mHolder.schritt1_bez = (TextView) findViewById(R.id.zutatenfuer1);

        RId = getIntent().getExtras().getLong("Rezept");
        isNew = RId == -1;

        if (!isNew) {
            rezepteCursor = dbRezepte.erzeugeEinRezeptCursor(RId);
            rezepteCursor.moveToFirst();
        }

        if(savedInstanceState == null) {
            if (!isNew) {
                Cursor cZutaten;
                if (rezepteCursor.getString(DatenBank.tblRezepte.teil1.getColumn()).equals("")) {
                    cZutaten = dbRezepte.erzeugeZutatenListeCursor(RId, -1);
                    while (cZutaten.moveToNext()) {
                        ArrayMap am = new ArrayMap();
                        am.put("id", cZutaten.getLong(DatenBank.tblZutatenListeJoin._id.getColumn()));
                        am.put("menge", cZutaten.getDouble(DatenBank.tblZutatenListeJoin.menge.getColumn()));
                        am.put("einheit", cZutaten.getLong(DatenBank.tblZutatenListeJoin.einheit.getColumn()));
                        am.put("zutat", cZutaten.getLong(DatenBank.tblZutatenListeJoin.zutat.getColumn()));
                        listZutaten1.add(am);
                    }
                } else {

                    for (int i = 1; i <= 5; i++) {
                        cZutaten = dbRezepte.erzeugeZutatenListeCursor(RId, i);
                        while (cZutaten.moveToNext()) {
                            ArrayMap am = new ArrayMap();
                            am.put("id", cZutaten.getLong(DatenBank.tblZutatenListeJoin._id.getColumn()));
                            am.put("menge", cZutaten.getDouble(DatenBank.tblZutatenListeJoin.menge.getColumn()));
                            am.put("einheit", cZutaten.getLong(DatenBank.tblZutatenListeJoin.einheit.getColumn()));
                            am.put("zutat", cZutaten.getLong(DatenBank.tblZutatenListeJoin.zutat.getColumn()));
                            zListen.get(i - 1).add(am);
                        }
                    }
                }
            }
        }else{
            zListen = (List<List<ArrayMap<String, String>>>) savedInstanceState.getSerializable("zListen");
        }

        ImageButton bt = (ImageButton) findViewById(R.id.add_1);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayMap am = new ArrayMap();
                am.put("id", Long.valueOf(-1));
                am.put("menge", Double.valueOf(0));
                am.put("einheit", Long.valueOf(-1));
                am.put("zutat", Long.valueOf(-1));
                listZutaten1.add(am);
                setListViewHeightBasedOnChildren(mHolder.zListe[0]);
            }
        });
        bt = (ImageButton) findViewById(R.id.add_2);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!(mHolder.schritt[0].getText().toString().equals("")||mHolder.schritt[1].getText().toString().equals(""))) {
                    ArrayMap am = new ArrayMap();
                    am.put("id", Long.valueOf(-1));
                    am.put("menge", Double.valueOf(0));
                    am.put("einheit", Long.valueOf(-1));
                    am.put("zutat", Long.valueOf(-1));
                    listZutaten2.add(am);
                    setListViewHeightBasedOnChildren(mHolder.zListe[1]);
                }else{
                    Toast.makeText(EinRezeptEdit.this, "Bitte zuerst Bezeichnung von Schritt 1 und 2 eingeben!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        bt = (ImageButton) findViewById(R.id.add_3);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!(mHolder.schritt[0].getText().toString().equals("")||
                        mHolder.schritt[1].getText().toString().equals("")||
                        mHolder.schritt[2].getText().toString().equals(""))) {
                    ArrayMap am = new ArrayMap();
                    am.put("id", Long.valueOf(-1));
                    am.put("menge", Double.valueOf(0));
                    am.put("einheit", Long.valueOf(-1));
                    am.put("zutat", Long.valueOf(-1));
                    listZutaten3.add(am);
                    setListViewHeightBasedOnChildren(mHolder.zListe[2]);
                }else{
                    Toast.makeText(EinRezeptEdit.this, "Bitte zuerst Bezeichnung von Schritt 1, 2 und 3 eingeben!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        bt = (ImageButton) findViewById(R.id.add_4);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!(mHolder.schritt[0].getText().toString().equals("")||
                        mHolder.schritt[1].getText().toString().equals("")||
                        mHolder.schritt[2].getText().toString().equals("")||
                        mHolder.schritt[3].getText().toString().equals(""))) {
                    ArrayMap am = new ArrayMap();
                    am.put("id", Long.valueOf(-1));
                    am.put("menge", Double.valueOf(0));
                    am.put("einheit", Long.valueOf(-1));
                    am.put("zutat", Long.valueOf(-1));
                    listZutaten4.add(am);
                    setListViewHeightBasedOnChildren(mHolder.zListe[3]);
                }else{
                    Toast.makeText(EinRezeptEdit.this, "Bitte zuerst Bezeichnung von Schritt 1, 2, 3 und 4 eingeben!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        bt = (ImageButton) findViewById(R.id.add_5);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!(mHolder.schritt[0].getText().toString().equals("")||
                        mHolder.schritt[1].getText().toString().equals("")||
                        mHolder.schritt[2].getText().toString().equals("")||
                        mHolder.schritt[3].getText().toString().equals("")||
                        mHolder.schritt[4].getText().toString().equals(""))) {
                    ArrayMap am = new ArrayMap();
                    am.put("id", Long.valueOf(-1));
                    am.put("menge", Double.valueOf(0));
                    am.put("einheit", Long.valueOf(-1));
                    am.put("zutat", Long.valueOf(-1));
                    listZutaten5.add(am);
                    setListViewHeightBasedOnChildren(mHolder.zListe[4]);
                }else{
                    Toast.makeText(EinRezeptEdit.this, "Bitte zuerst Bezeichnung von Schritt 1, 2, 3, 4 und 5 eingeben!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mHolder.bild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int img_size = Integer.valueOf(prefs.getString("img_size", "400"));
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra("crop", "true");
                intent.putExtra("aspectX", 1);
                intent.putExtra("aspectY", 1);
                intent.putExtra("outputX", img_size);
                intent.putExtra("outputY", img_size);
                intent.putExtra("noFaceDetection", true);
                File path = new File (getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),"");
                File file = new File(path,"new_image" + ".jpg" );
                Uri imageUri = FileProvider.getUriForFile(getApplication(), "de.steffen.rezepte.fileprovider", file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                intent.addFlags(FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.putExtra("outputFormat",Bitmap.CompressFormat.JPEG.toString());
                intent.putExtra("return-data", false);

                List<ResolveInfo> resInfoList = getApplicationContext().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    getApplicationContext().grantUriPermission(packageName, imageUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                startActivityForResult(Intent.createChooser(intent,getString(R.string.bild_auswahl)), REQUEST_IMAGE);

            }
        });

        mHolder.schritt[0] = (EditText)findViewById(R.id.ueberschriftzutaten1);
        mHolder.watcher[0] = new NotEmptyTextWatcher();
        mHolder.schritt[1] = (EditText)findViewById(R.id.ueberschriftzutaten2);
        mHolder.watcher[1] = new NotEmptyTextWatcher();
        mHolder.schritt[2] = (EditText)findViewById(R.id.ueberschriftzutaten3);
        mHolder.watcher[2] = new NotEmptyTextWatcher();
        mHolder.schritt[3] = (EditText)findViewById(R.id.ueberschriftzutaten4);
        mHolder.watcher[3] = new NotEmptyTextWatcher();
        mHolder.schritt[4] = (EditText)findViewById(R.id.ueberschriftzutaten5);
        mHolder.watcher[4] = new NotEmptyTextWatcher();

        setListView();

        mHolder.schritt[0].addTextChangedListener(mHolder.watcher[0]);
        mHolder.schritt[1].addTextChangedListener(mHolder.watcher[1]);
        mHolder.schritt[2].addTextChangedListener(mHolder.watcher[2]);
        mHolder.schritt[3].addTextChangedListener(mHolder.watcher[3]);
        mHolder.schritt[4].addTextChangedListener(mHolder.watcher[4]);

        mHolder.name.addTextChangedListener(new ChangedTextWatcher());
        mHolder.portionen.addTextChangedListener(new ChangedTextWatcher());
        mHolder.zubereitung.addTextChangedListener(new ChangedTextWatcher());

        mHolder.kategorie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dataChanged = true;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    protected void onDestroy() {
        dbRezepte.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(dataChanged){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getText(R.string.speichern));
            builder.setMessage(getText(R.string.speichern_text));
            builder.setPositiveButton(getText(R.string.JA), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    save();
                    dialog.dismiss();
                    EinRezeptEdit.super.onBackPressed();
                }
            });
            builder.setNegativeButton(getText(R.string.NEIN) , new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    EinRezeptEdit.super.onBackPressed();
                }
            });
            builder.setNeutralButton(getText(R.string.ABBRECHEN), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();

        }else{
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("zListen", (Serializable) zListen);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_IMAGE:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    if(extras != null ) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        newPhoto = BitmapFactory.decodeFile(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)+"/new_image" + ".jpg", options);
                        mHolder.bild.setImageBitmap(newPhoto);
                        newImage = true;
                        dataChanged = true;
                    }
                }
                break;
            case REQUEST_NEW_ZUTAT:
                //Liste aller gespeicherten Zutaten neu einlesen
                ZutatenFromDB.clear();

                final Cursor zutaten = dbRezepte.db.rawQuery("SELECT _id, bez_singular, bez_plural FROM zutaten",null);
                while (zutaten.moveToNext()) {
                    ZutatenFromDB.add(new Zutaten (zutaten.getLong(0), zutaten.getString(1), zutaten.getString(2)));
                }
                zutaten.close();
                String[] anzeigeSpalten = new String[] {"menge", "einheit", "zutat"};
                int[] anzeigeViews = new int[] {R.id.menge, R.id.einheit, R.id.zutat};
                for (int a=0; a<5; a++) {
                    zAdapter[a] = new ZutatenSimpleAdapter(this, zListen.get(a), R.layout.zutaten_datensatz_edit,  anzeigeSpalten, anzeigeViews);
                    //zAdapter[a].setViewBinder(new EinRezeptEdit.ZutatenViewBinder());
                    mHolder.zListe[a].setAdapter(zAdapter[a]);
                    setListViewHeightBasedOnChildren(mHolder.zListe[a]);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ein_rezept_edit, menu);
        mmenu=menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_cancel) {
            if(dataChanged){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getText(R.string.abbrechen));
                builder.setMessage(getText(R.string.abbrechen_text));
                builder.setPositiveButton(getText(R.string.JA), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                builder.setNegativeButton(getText(R.string.NEIN), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }else{
                finish();
            }

            return true;
        }

        if (id == R.id.action_save){
            save();
            finish();
            return true;
        }

        if (id == R.id.action_add_zutat){
            Intent intent = new Intent(this, EditData.class);
            intent.putExtra("id", "zutaten-neu");
            startActivityForResult(intent,REQUEST_NEW_ZUTAT);
            return true;
        }

        if(id == R.id.action_add_schritt){
            if(anz_schritte < 5){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getText(R.string.arbeitsschritt_name));
                final EditText input = new EditText(this);
                final EditText input2 = new EditText(this);
                final LinearLayout ll = new LinearLayout(this);
                ll.setOrientation(LinearLayout.VERTICAL);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setHint(getString(R.string.arbeitsschritt_bezeichnung,(anz_schritte+1)));
                ll.addView(input);
                if(anz_schritte == 0){
                    input2.setInputType(InputType.TYPE_CLASS_TEXT);
                    input2.setHint(getString(R.string.arbeitsschritt_bezeichnung,(anz_schritte+2)));
                    ll.addView(input2);
                }
                builder.setView(ll, 50, 0, 50, 0);
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!(input.getText().toString().equals("")||(input2.getText().toString().equals("")&&anz_schritte==0))) {
                            anz_schritte++;
                           if (anz_schritte==1) {
                               uListe.get(0).setText(input.getText());
                               uListe.get(1).setText(input2.getText());
                               mHolder.header[1].setVisibility(View.VISIBLE);
                               anz_schritte++;
                           }else{
                               uListe.get(anz_schritte-1).setText(input.getText());
                               mHolder.header[anz_schritte-1].setVisibility(View.VISIBLE);
                            }
                            dialog.dismiss();
                        }else{
                            Toast.makeText(EinRezeptEdit.this, getText(R.string.arbeitsschritte_ausfuellen), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }else{
                Toast.makeText(this, getText(R.string.arbeitsschritte_max), Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if(id == R.id.action_remove_schritt){
            if(anz_schritte > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getText(R.string.arbeitsschritte_loeschen));
                final RadioGroup ll = new RadioGroup(this);
                ll.setOrientation(LinearLayout.VERTICAL);
                final RadioButton r1 = new RadioButton(this);
                r1.setText(uListe.get(0).getText());
                r1.setId(R.id.rb1);
                ll.addView(r1);
                final RadioButton r2 = new RadioButton(this);
                r2.setText(uListe.get(1).getText());
                r2.setId(R.id.rb2);
                ll.addView(r2);

                if(anz_schritte > 2){
                    final RadioButton r3 = new RadioButton(this);
                    r3.setText(uListe.get(2).getText());
                    r3.setId(R.id.rb3);
                    ll.addView(r3);
                    if(anz_schritte > 3){
                        final RadioButton r4 = new RadioButton(this);
                        r4.setText(uListe.get(3).getText());
                        r4.setId(R.id.rb4);
                        ll.addView(r4);
                        if(anz_schritte > 4){
                            final RadioButton r5 = new RadioButton(this);
                            r5.setText(uListe.get(4).getText());
                            r5.setId(R.id.rb5);
                            ll.addView(r5);
                        }
                    }
                }
                builder.setView(ll);
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int startWert = 0;
                        switch (ll.getCheckedRadioButtonId()){
                            case R.id.rb1:
                                startWert=1;
                                break;
                            case R.id.rb2:
                                startWert=2;
                                break;
                            case R.id.rb3:
                                startWert=3;
                                break;
                            case R.id.rb4:
                                startWert=4;
                                break;
                            case R.id.rb5:
                                startWert=5;
                                break;
                        }
                        for(int i = startWert; i < 5; i++){
                            mHolder.schritt[i-1].removeTextChangedListener(mHolder.watcher[i-1]);
                            uListe.get(i-1).setText(uListe.get(i).getText());
                            mHolder.schritt[i-1].addTextChangedListener(mHolder.watcher[i-1]);
                            zListen.get(i-1).clear();
                            for(int j = 0; j<zListen.get(i).size(); j++){
                                zListen.get(i-1).add(zListen.get(i).get(j));
                            }
                        }
                        mHolder.schritt[4].removeTextChangedListener(mHolder.watcher[4]);
                        uListe.get(4).setText("");
                        mHolder.schritt[4].addTextChangedListener(mHolder.watcher[4]);
                        zListen.get(4).clear();
                        mHolder.header[anz_schritte-1].setVisibility(View.GONE);
                        anz_schritte--;
                        if(anz_schritte ==1){
                            anz_schritte =0;
                            uListe.get(0).setText("");
                        }
                        ((BaseAdapter) mHolder.zListe[0].getAdapter()).notifyDataSetChanged();
                        ((BaseAdapter) mHolder.zListe[1].getAdapter()).notifyDataSetChanged();
                        ((BaseAdapter) mHolder.zListe[2].getAdapter()).notifyDataSetChanged();
                        ((BaseAdapter) mHolder.zListe[3].getAdapter()).notifyDataSetChanged();
                        ((BaseAdapter) mHolder.zListe[4].getAdapter()).notifyDataSetChanged();

                    }
                });
                builder.show();
            }

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void setListView(){
        Spinner Kategorien = (Spinner) findViewById(R.id.kategorie);
        Kategorien.setAdapter(new ArrayAdapter(this, R.layout.my_spinner_dropdown,  KategorienFromDB ));
        int i = 0;
        if(!isNew) {
            long id = rezepteCursor.getLong(DatenBank.tblRezepte.kategorie.getColumn());
            while (KategorienFromDB.get((int) Kategorien.getItemIdAtPosition(i)).id != id && i + 1 < Kategorien.getCount()) {
                i++;
            }
            if (KategorienFromDB.get((int) Kategorien.getItemIdAtPosition(i)).id == id) {
                Kategorien.setSelection(i, false);
            }
        }

        if(isNew){
            mHolder.portionen.setText("4");
            mHolder.name.setText("");
            for (int a=0; a<5; a++) { arbeitsSchritte[a]=""; }
            rezVersion = 0;
        }
        else{
            arbeitsSchritte[0] = rezepteCursor.getString(DatenBank.tblRezepte.teil1.getColumn());
            arbeitsSchritte[1] = rezepteCursor.getString(DatenBank.tblRezepte.teil2.getColumn());
            arbeitsSchritte[2] = rezepteCursor.getString(DatenBank.tblRezepte.teil3.getColumn());
            arbeitsSchritte[3] = rezepteCursor.getString(DatenBank.tblRezepte.teil4.getColumn());
            arbeitsSchritte[4] = rezepteCursor.getString(DatenBank.tblRezepte.teil5.getColumn());

            mHolder.portionen.setText(String.valueOf(rezepteCursor.getInt(DatenBank.tblRezepte.portionen.getColumn())));
            mHolder.name.setText(rezepteCursor.getString(DatenBank.tblRezepte.name.getColumn()));
            rezVersion = rezepteCursor.getInt(DatenBank.tblRezepte.version.getColumn());
            byte[] foto = rezepteCursor.getBlob(DatenBank.tblRezepte.foto.getColumn());
            if(foto != null) {
                Bitmap bm = BitmapFactory.decodeByteArray(foto, 0, foto.length);
                mHolder.bild.setImageBitmap(bm);
            }
            mHolder.zubereitung.setText(rezepteCursor.getString(DatenBank.tblRezepte.anleitung.getColumn()));

            anz_schritte = 0;
            for (int a=0; a<5; a++) {
                if(arbeitsSchritte[a].equals("")){
                    mHolder.schritt[a].setText("");
                    if (a==0) {
                       mHolder.schritt1_bez.setText((R.string.zutaten));
                    } else {
                       mHolder.header[a].setVisibility(View.GONE);
                    }
                }else{
                    if (a==0) { mHolder.schritt1_bez.setText((R.string.zutaten_fuer)); }
                    mHolder.schritt[a].setText(arbeitsSchritte[a]);
                    mHolder.header[a].setVisibility(View.VISIBLE);
                    anz_schritte++;
                }
            }
        }


        String[] anzeigeSpalten = new String[] {"menge", "einheit", "zutat"};
        int[] anzeigeViews = new int[] {R.id.menge, R.id.einheit, R.id.zutat};


        //ListView liste = (ListView) findViewById(R.id.zutaten);

        for (int a=0; a<5; a++) {
            zAdapter[a] = new ZutatenSimpleAdapter(this, zListen.get(a), R.layout.zutaten_datensatz_edit,  anzeigeSpalten, anzeigeViews);
            //zAdapter[a].setViewBinder(new EinRezeptEdit.ZutatenViewBinder());
            mHolder.zListe[a].setAdapter(zAdapter[a]);
            setListViewHeightBasedOnChildren(mHolder.zListe[a]);
        }
    }

    void save(){
        if(dataChanged) {
            dbRezepte.db.delete("zutatenliste", "rezeptID = " + RId, null);
            ContentValues cv = new ContentValues();
            boolean hatSchritte = !(mHolder.schritt[1].getText().toString().equals(""));

            cv.put("name", ((EditText) findViewById(R.id.RezeptName)).getText().toString());
            cv.put("portionen", Integer.valueOf(((EditText) findViewById(R.id.portionen)).getText().toString()));
            cv.put("version", rezVersion+1);
            if (hatSchritte) {
                cv.put("teil1", mHolder.schritt[0].getText().toString());
                cv.put("teil2", mHolder.schritt[1].getText().toString());
                cv.put("teil3", mHolder.schritt[2].getText().toString());
                cv.put("teil4", mHolder.schritt[3].getText().toString());
                cv.put("teil5", mHolder.schritt[4].getText().toString());
            } else {
                cv.put("teil1", "");
                cv.put("teil2", "");
                cv.put("teil3", "");
                cv.put("teil4", "");
                cv.put("teil5", "");
            }


            cv.put("anleitung", mHolder.zubereitung.getText().toString());
            Cursor kCursor = dbRezepte.db.query("kategorien", DatenBank.KategorienSpalten, "kategorie = '" +
                    mHolder.kategorie.getSelectedItem().toString() + "'", null, null, null, null);
            if (kCursor.moveToFirst()) {
                cv.put("kategorie", kCursor.getInt(DatenBank.tblKategorien._id.getColumn()));
            } else {
                cv.put("kategorie", 1);
            }
            if (newImage || isNew) {
                if(newPhoto == null){
                    newPhoto = ((BitmapDrawable)mHolder.bild.getDrawable()).getBitmap();
                }
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                newPhoto.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                byte[] byteArray = stream.toByteArray();
                cv.put("foto", byteArray);
                newPhoto = null;
                newImage = false;
            }
            if (isNew) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                cv.put("user", prefs.getString("e_benutzer",""));
                RId = dbRezepte.db.insert("rezepte", null, cv);
                isNew = false;
            } else {
                dbRezepte.db.update("rezepte", cv, "_id = " + RId, null);
            }

            for (int i = 0; i < 5; i++) {
                List<ArrayMap<String, String>> lv = zListen.get(i);

                for (int z = 0; z < lv.size(); z++) {
                    ArrayMap map = (ArrayMap) lv.get(z);
                    if ((Long) map.get("zutat") >= 0) {
                        cv.clear();
                        cv.put("rezeptID", RId);
                        if (hatSchritte) {
                            cv.put("rezeptteil", i + 1);
                        } else {
                            cv.put("rezeptteil", 0);
                        }

                        cv.put("menge", (Double) map.get("menge"));
                        cv.put("einheit", (Long) map.get("einheit"));
                        cv.put("zutat", (Long) map.get("zutat"));
                        dbRezepte.db.insert("zutatenListe", null, cv);
                    }
                }
            }
            Toast.makeText(this, getText(R.string.speichern_ok),Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, getText(R.string.speichern_nicht_geändert),Toast.LENGTH_SHORT).show();
        }
    }


    /**** Method for Setting the Height of the ListView dynamically.
     **** Hack to fix the issue of not showing all the items of the ListView
     **** when placed inside a ScrollView  ****/
    public void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) return;

       /* DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        int minHeight =displayMetrics.heightPixels;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            //if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            if (minHeight>view.getMeasuredHeight()) minHeight = view.getMeasuredHeight();
        }
        totalHeight = minHeight * listAdapter.getCount();
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);*/

        int totalHeight = listAdapter.getCount() * convertDipToPixels(40);
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);

    }
    public int convertDipToPixels(float dips)
    {
        return (int) (dips * this.getResources().getDisplayMetrics().density + 0.5f);
    }
}