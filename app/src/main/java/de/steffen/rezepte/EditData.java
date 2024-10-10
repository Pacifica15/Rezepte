package de.steffen.rezepte;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class EditData extends AppCompatActivity {

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

    //ListView lv = new ListView(this);
    SimpleCursorAdapter adapter;
    ListView liste;
    DatenBank dbRezepte;
    String dbPfad;
    Menu optionsMenu;
    String actionID;

    List<Einheiten> EinheitenFromDB = new ArrayList<Einheiten>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_data);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbPfad = getExternalFilesDir("")+"/rezepte.db";
        dbRezepte = new DatenBank(this, dbPfad, null);

        liste = (ListView) findViewById(R.id.liste);
        liste.setEmptyView(findViewById(R.id.empty));

        Cursor einheiten = dbRezepte.db.rawQuery("SELECT _id, einh_singular, einh_plural FROM einheiten",null);
        //EinheitenFromDB.add(new Einheiten(-1,"",""));
        while (einheiten.moveToNext()) {
            EinheitenFromDB.add(new Einheiten(einheiten.getLong(0),einheiten.getString(1), einheiten.getString(2)));
        }
        einheiten.close();

        actionID = getIntent().getExtras().getString("id");
        switch (actionID) {
            case "zutaten":
                zutaten();
                break;
            case "zutaten-neu":
                zutaten();
                break;
            default:
                finish();
                break;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        optionsMenu=menu;
        getMenuInflater().inflate(R.menu.activity_edit_data, menu);
        if (actionID.equals("zutaten-neu")) {
            onOptionsItemSelected(optionsMenu.findItem(R.id.action_add));
        }

        return true;
    }

/*    @Override
    protected void onResume() {
        super.onResume();
        onOptionsItemSelected(optionsMenu.findItem(R.id.action_add));
        if (actionID.equals("zutaten-neu")) {
            optionsMenu.findItem(R.id.action_add).getActionView().performClick();
        }
    }
*/

    @Override
    protected void onDestroy() {
        dbRezepte.close();
        super.onDestroy();
    }
    long getEinhId(Editable s){
        long iDFound = -1;
        for (int i=0; i<EinheitenFromDB.size();i++) {
            if (s.toString().equals(EinheitenFromDB.get(i).einh_singular) ||
                    s.toString().equals(EinheitenFromDB.get(i).einh_plural)) {
                iDFound=EinheitenFromDB.get(i).id;
                break;
            }
        }
        return iDFound;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_add){
            AlertDialog.Builder builder = new AlertDialog.Builder(EditData.this);
            builder.setTitle(getString(R.string.zutaten));
            builder.setNegativeButton(R.string.abbrechen,null);
            builder.setPositiveButton(R.string.speichern, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // nur speichern wenn eine Bezeichnung eingegeben wurde und die Bezeichnung noch nicht existiert.
                    String bez_sing = ((EditText) ((AlertDialog) dialog).findViewById(R.id.bez_singular)).getText().toString();
                    String bez_plu = ((EditText) ((AlertDialog) dialog).findViewById(R.id.bez_plural)).getText().toString();
                    Cursor zCursor = dbRezepte.db.rawQuery("SELECT * FROM zutaten WHERE bez_singular = '"+bez_sing+"' or bez_plural = '"+bez_sing
                                                           +"' or bez_singular = '"+bez_plu+"' or bez_plural = '"+bez_plu+"'",null);
                    if (!bez_sing.equals("") && !zCursor.moveToFirst()) {
                        ContentValues cv = new ContentValues();
                        cv.put("bez_singular", bez_sing);
                        // wenn kein Plural eingegeben wurde Singular übernehmen
                        if (!bez_plu.equals("")) cv.put("bez_plural", bez_plu); else cv.put("bez_plural", bez_sing);
                        cv.put("einheit_id", getEinhId(((AutoCompleteTextView) ((AlertDialog) dialog).findViewById(R.id.einheit)).getText()));
                        cv.put("shop", ((EditText) ((AlertDialog) dialog).findViewById(R.id.shop)).getText().toString());
                        if (!((EditText) ((AlertDialog) dialog).findViewById(R.id.pckgr)).getText().toString().equals("")) {
                            cv.put("packungsgroesse", Double.valueOf(((EditText) ((AlertDialog) dialog).findViewById(R.id.pckgr)).getText().toString()));
                        }
                        cv.put("pack_einheit_id", getEinhId(((AutoCompleteTextView) ((AlertDialog) dialog).findViewById(R.id.einh_p)).getText()));
                        if (!((EditText) ((AlertDialog) dialog).findViewById(R.id.preis)).getText().toString().equals("")) {
                            cv.put("preis", Double.valueOf(((EditText) ((AlertDialog) dialog).findViewById(R.id.preis)).getText().toString()));
                        }
                        dbRezepte.db.insert("zutaten", null, cv);
                        ((SimpleCursorAdapter) liste.getAdapter()).getCursor().requery();
                    } else {
                        Toast.makeText(EditData.this,R.string.ed_zutat_existiert,Toast.LENGTH_LONG).show();
                    }
                    zCursor.close();
                    if (actionID.equals("zutaten-neu")) finish();
                }
            });
            builder.setView(R.layout.edit_data_zutaten);
            builder.setCancelable(false);
            AlertDialog dlg = builder.show();

            ArrayAdapter<Einheiten> einhAdapter = new ArrayAdapter<Einheiten>(getApplicationContext(),R.layout.my_autocomplete_dropdown,EinheitenFromDB);
            ((AutoCompleteTextView)dlg.findViewById(R.id.einheit)).setAdapter(einhAdapter);
            ((AutoCompleteTextView)dlg.findViewById(R.id.einheit)).setValidator(new EinheitenValidator());
            ((AutoCompleteTextView)dlg.findViewById(R.id.einheit)).setThreshold(1);

            ((AutoCompleteTextView)dlg.findViewById(R.id.einh_p)).setAdapter(einhAdapter);
            ((AutoCompleteTextView)dlg.findViewById(R.id.einh_p)).setValidator(new EinheitenValidator());
            ((AutoCompleteTextView)dlg.findViewById(R.id.einh_p)).setThreshold(1);
        }

        return true;
    }

    void zutaten(){

        Cursor cursor = dbRezepte.erzeugeZutatenCursor();
        String[] anzeigeSpalten = new String[] {"bez_singular", "einh_singular", "shop", "preis", "rez_id"};
        int[] anzeigeViews = new int[] {R.id.bezeichnung, R.id.einheit, R.id.shop, R.id.preis, R.id.btn_delete};
        adapter = new SimpleCursorAdapter(this, R.layout.zutaten_stammdaten, cursor, anzeigeSpalten, anzeigeViews, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if(columnIndex == 5) {
                    //view.setActivated(!cursor.isNull(4));
                    if (cursor.isNull(5)) {
                        view.setVisibility(View.VISIBLE);
                    } else {
                        view.setVisibility(View.INVISIBLE);
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });
        liste.setAdapter(adapter);
        liste.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView av, View v, int pos, final long Zid) {
                AlertDialog.Builder builder = new AlertDialog.Builder(EditData.this);
                builder.setTitle(getString(R.string.zutaten));
                builder.setNegativeButton(R.string.abbrechen,null);
                builder.setPositiveButton(R.string.speichern, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // nur speichern wenn eine Bezeichnung eingegeben wurde und die Bezeichnung noch nicht existiert.
                        String bez_sing = ((EditText) ((AlertDialog) dialog).findViewById(R.id.bez_singular)).getText().toString();
                        String bez_plu = ((EditText) ((AlertDialog) dialog).findViewById(R.id.bez_plural)).getText().toString();
                        Cursor zCursor = dbRezepte.db.rawQuery("SELECT * FROM zutaten WHERE bez_singular = '"+bez_sing+"' or bez_plural = '"+bez_sing
                                +"' or bez_singular = '"+bez_plu+"' or bez_plural = '"+bez_plu+"'",null);
                        if (!bez_sing.equals("") && !zCursor.moveToFirst()) {
                            ContentValues cv = new ContentValues();
                            cv.put("bez_singular", bez_sing);
                            // wenn kein Plural eingegeben wurde Singular übernehmen
                            if (!bez_plu.equals("")) cv.put("bez_plural", bez_plu); else cv.put("bez_plural", bez_sing);
                            cv.put("einheit_id", getEinhId(((AutoCompleteTextView) ((AlertDialog) dialog).findViewById(R.id.einheit)).getText()));
                            cv.put("shop", ((EditText) ((AlertDialog) dialog).findViewById(R.id.shop)).getText().toString());
                            if (!((EditText) ((AlertDialog) dialog).findViewById(R.id.pckgr)).getText().toString().equals("")) {
                                cv.put("packungsgroesse", Double.valueOf(((EditText) ((AlertDialog) dialog).findViewById(R.id.pckgr)).getText().toString()));
                            }
                            cv.put("pack_einheit_id", getEinhId(((AutoCompleteTextView) ((AlertDialog) dialog).findViewById(R.id.einh_p)).getText()));
                            if (!((EditText) ((AlertDialog) dialog).findViewById(R.id.preis)).getText().toString().equals("")) {
                                cv.put("preis", Double.valueOf(((EditText) ((AlertDialog) dialog).findViewById(R.id.preis)).getText().toString()));
                            }
                            dbRezepte.db.update("zutaten", cv, Zid + " = _id", null);
                            ((SimpleCursorAdapter) liste.getAdapter()).getCursor().requery();
                        } else {
                            Toast.makeText(EditData.this,R.string.ed_zutat_existiert,Toast.LENGTH_LONG).show();
                        }
                        zCursor.close();
                    }
                });
                builder.setView(R.layout.edit_data_zutaten);
                builder.setCancelable(false);
                AlertDialog dlg = builder.show();

                ArrayAdapter<Einheiten> einhAdapter = new ArrayAdapter<Einheiten>(getApplicationContext(),R.layout.my_autocomplete_dropdown,EinheitenFromDB);
                ((AutoCompleteTextView)dlg.findViewById(R.id.einheit)).setAdapter(einhAdapter);
                ((AutoCompleteTextView)dlg.findViewById(R.id.einheit)).setValidator(new EinheitenValidator());
                ((AutoCompleteTextView)dlg.findViewById(R.id.einheit)).setThreshold(1);

                ((AutoCompleteTextView)dlg.findViewById(R.id.einh_p)).setAdapter(einhAdapter);
                ((AutoCompleteTextView)dlg.findViewById(R.id.einh_p)).setValidator(new EinheitenValidator());
                ((AutoCompleteTextView)dlg.findViewById(R.id.einh_p)).setThreshold(1);


                Cursor zutCursor = dbRezepte.db.rawQuery("SELECT * FROM zutaten WHERE _id = "+Zid,null);
                zutCursor.moveToFirst();
                ((EditText)dlg.findViewById(R.id.bez_singular)).setText(zutCursor.getString(DatenBank.tblZutaten.bez_singular.getColumn()));
                ((EditText)dlg.findViewById(R.id.bez_plural)).setText(zutCursor.getString(DatenBank.tblZutaten.bez_plural.getColumn()));
                ((EditText)dlg.findViewById(R.id.shop)).setText(zutCursor.getString(DatenBank.tblZutaten.shop.getColumn()));
                ((EditText)dlg.findViewById(R.id.pckgr)).setText(zutCursor.getString(DatenBank.tblZutaten.packungsgroesse.getColumn()));
                ((EditText)dlg.findViewById(R.id.preis)).setText(String.valueOf(zutCursor.getDouble(DatenBank.tblZutaten.preis.getColumn())));

                ArrayAdapter Zadapter = (ArrayAdapter) ((AutoCompleteTextView)dlg.findViewById(R.id.einheit)).getAdapter();
                int valueToFind = zutCursor.getInt(DatenBank.tblZutaten.einheit_id.getColumn());
                for (int p = 0; p < Zadapter.getCount(); p++) {
                    Einheiten mEinheit = (Einheiten) Zadapter.getItem(p);
                    if(mEinheit.id == valueToFind) {
                        mEinheit.setPlural(false);
                        ((AutoCompleteTextView)dlg.findViewById(R.id.einheit)).setText(mEinheit.toString());
                        break;
                    }
                }

                Zadapter = (ArrayAdapter) ((AutoCompleteTextView)dlg.findViewById(R.id.einh_p)).getAdapter();
                valueToFind = zutCursor.getInt(DatenBank.tblZutaten.pack_einh_id.getColumn());
                for (int p = 0; p < Zadapter.getCount(); p++) {
                    Einheiten mEinheit = (Einheiten) Zadapter.getItem(p);
                    if(mEinheit.id == valueToFind) {
                        mEinheit.setPlural(false);
                        ((AutoCompleteTextView)dlg.findViewById(R.id.einh_p)).setText(mEinheit.toString());
                        break;
                    }
                }


            }

        });
    }

}
