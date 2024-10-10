package de.steffen.rezepte;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.text.DecimalFormat;

//todo Einkaufsliste nach Shops unterteilen
//todo Preise in der Einkaufsliste anzeigen
//todo Preise in der Einkaufsliste eingebbar machen und Summe bilden
//todo Mengenumrechnung je Zutat (z.B. EL in Gramm)
//todo Einkaufsliste auf Einkuafsmengeneinheiten aufbauen (z.B. l für Flüssigkeiten)

public class EinkaufsListe extends Fragment {
    String sql_einkaufsliste1 = "select _id, case when menge_k is null or menge_k<1 then menge_e else menge_k end as menge,\n"+
            "\t\tcase when menge_k is null or menge_k<1\n"+
            "\t\t\tthen case when menge_e <> 1 then einh_e_p else einh_e_s end \n"+
            "\t\t\telse case when menge_k <> 1 then einh_k_p else einh_k_p end\n"+
            "\t\tend as einheit,\t\n"+
            "\t\tcase when menge_k is null \n"+
            "\t\t\tthen case when menge_e <> 1 or einh_e_s=\"g\" or einh_e_s=\"Kg\" then bez_plural else bez_singular end\n"+
            "\t\t\telse case when menge_k <> 1 or einh_k_s=\"g\" or einh_k_s=\"Kg\" then bez_plural else bez_singular end\n"+
            "\t\tend as bezeichnung, ok, zut, einh\t\n"+
            "from\n"+
            "(select einkaufsliste._id, rezeptID, rezeptteil, zutat as zut, einheit as einh, zutaten.bez_singular, zutaten.bez_plural, ok,\n"+
            "\t   sum(menge*einheiten.faktor_e) as menge_e, einh_e.einh_singular as einh_e_s, einh_e.einh_plural as einh_e_p,\n"+
            "\t   sum(menge*einheiten.faktor_k) as menge_k, einh_k.einh_singular as einh_k_s, einh_k.einh_plural as einh_k_p\n"+
            "from einkaufsliste \n"+
            "left join zutaten on zutat=zutaten._id\n"+
            "left join einheiten on einheit=einheiten._id\n"+
            "left join einheiten einh_e on einheiten.basis_id_e=einh_e._id\n"+
            "left join einheiten einh_k on einheiten.basis_id_k=einh_k._id \n"+
            "group by einh_e_s, zut, ok having menge>0 order by zut)";

    String sql_einkaufsliste2 = "select _id, case when menge_k is null or menge_k<1 then menge_e else menge_k end as menge,\n"+
            "\t\tcase when menge_k is null or menge_k<1\n"+
            "\t\t\tthen case when menge_e <> 1 then einh_e_p else einh_e_s end \n"+
            "\t\t\telse case when menge_k <> 1 then einh_k_p else einh_k_p end\n"+
            "\t\tend as einheit,\t\n"+
            "\t\tcase when menge_k is null \n"+
            "\t\t\tthen case when menge_e <> 1 or einh_e_s=\"g\" or einh_e_s=\"Kg\" then bez_plural else bez_singular end\n"+
            "\t\t\telse case when menge_k <> 1 or einh_k_s=\"g\" or einh_k_s=\"Kg\" then bez_plural else bez_singular end\n"+
            "\t\tend as bezeichnung, ok, zut, einh\t\n"+
            "from\n"+
            "(select einkaufsliste._id, rezeptID, rezeptteil, zutat as zut, einheit as einh, zutaten.bez_singular, zutaten.bez_plural, ok,\n"+
            "\t   sum(menge*einheiten.faktor_e) as menge_e, einh_e.einh_singular as einh_e_s, einh_e.einh_plural as einh_e_p,\n"+
            "\t   sum(menge*einheiten.faktor_k) as menge_k, einh_k.einh_singular as einh_k_s, einh_k.einh_plural as einh_k_p\n"+
            "from einkaufsliste \n"+
            "left join zutaten on zutat=zutaten._id\n"+
            "left join einheiten on einheit=einheiten._id\n"+
            "left join einheiten einh_e on einheiten.basis_id_e=einh_e._id\n"+
            "left join einheiten einh_k on einheiten.basis_id_k=einh_k._id \n"+
            "group by einh_e_s, zut, ok having menge>0 order by ok, zut)";

    String sql_einkaufslisteSend = "select _id, case when menge_k is null or menge_k<1 then menge_e else menge_k end as menge,\n"+
            "\t\tcase when menge_k is null or menge_k<1\n"+
            "\t\t\tthen case when menge_e <> 1 then einh_e_p else einh_e_s end \n"+
            "\t\t\telse case when menge_k <> 1 then einh_k_p else einh_k_p end\n"+
            "\t\tend as einheit,\t\n"+
            "\t\tcase when menge_k is null \n"+
            "\t\t\tthen case when menge_e <> 1 or einh_e_s=\"g\" or einh_e_s=\"Kg\" then bez_plural else bez_singular end\n"+
            "\t\t\telse case when menge_k <> 1 or einh_k_s=\"g\" or einh_k_s=\"Kg\" then bez_plural else bez_singular end\n"+
            "\t\tend as bezeichnung, ok, zut, einh\t\n"+
            "from\n"+
            "(select einkaufsliste._id, rezeptID, rezeptteil, zutat as zut, einheit as einh, zutaten.bez_singular, zutaten.bez_plural, ok,\n"+
            "\t   sum(menge*einheiten.faktor_e) as menge_e, einh_e.einh_singular as einh_e_s, einh_e.einh_plural as einh_e_p,\n"+
            "\t   sum(menge*einheiten.faktor_k) as menge_k, einh_k.einh_singular as einh_k_s, einh_k.einh_plural as einh_k_p\n"+
            "from einkaufsliste \n"+
            "left join zutaten on zutat=zutaten._id\n"+
            "left join einheiten on einheit=einheiten._id\n"+
            "left join einheiten einh_e on einheiten.basis_id_e=einh_e._id\n"+
            "left join einheiten einh_k on einheiten.basis_id_k=einh_k._id \n"+
            "group by einh_e_s, zut, ok having menge>0 and ok = -1 order by ok, zut)";

    DatenBank dbRezepte;
    public static String TAG = "EinkaufsListe";
    View vi;
    Boolean editMode = false;

    class MySimpleCursorAdapter extends SimpleCursorAdapter {

        public MySimpleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View vi;
            LinearLayout ll;
            CheckBox cb;
            vi=super.newView(context, cursor, parent);
            ll = (LinearLayout) vi;
            cb = (CheckBox) ll.findViewById(R.id.cb_ok);
            cb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ListView lv = (ListView) v.getParent().getParent();
                    long mID = lv.getItemIdAtPosition(lv.getPositionForView(v));
                    MySimpleCursorAdapter a = (MySimpleCursorAdapter) lv.getAdapter();
                    Cursor c = a.getCursor();
                    CheckBox cb = (CheckBox) v;
                    String suchStr = "zutat = "+c.getInt(DatenBank.sqlEinkaufsListe.zut.getColumn());
                    suchStr += " and einheit = "+c.getInt(DatenBank.sqlEinkaufsListe.einh.getColumn());
                    suchStr += " and ok = "+c.getInt(DatenBank.sqlEinkaufsListe.ok.getColumn());
                    ContentValues cv = new ContentValues();
                    if (cb.isChecked()) { cv.put("ok", 0); } else { cv.put("ok", -1); }
                    dbRezepte.db.update("einkaufsListe", cv, suchStr, null);
                    a.getCursor().requery();
                }
            });
            ImageButton bt = (ImageButton) ll.findViewById(R.id.btn_delete);
            bt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ListView lv = (ListView) v.getParent().getParent();
                    long mID = lv.getItemIdAtPosition(lv.getPositionForView(v));
                    MySimpleCursorAdapter a = (MySimpleCursorAdapter) lv.getAdapter();
                    Cursor c = a.getCursor();
                    String suchStr = "zutat = "+c.getInt(DatenBank.sqlEinkaufsListe.zut.getColumn());
                    suchStr += " and einheit = "+c.getInt(DatenBank.sqlEinkaufsListe.einh.getColumn());
                    suchStr += " and ok = "+c.getInt(DatenBank.sqlEinkaufsListe.ok.getColumn());
                    dbRezepte.db.delete("einkaufsListe", suchStr, null);
                    a.getCursor().requery();
                }
            });
            return vi;
        }
    }

    class EkListeViewBinder implements SimpleCursorAdapter.ViewBinder {

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if(columnIndex == DatenBank.sqlEinkaufsListe.ok.getColumn()){
                CheckBox cb = (CheckBox) view;
                cb.setVisibility(View.VISIBLE);
                cb.setChecked(cursor.getInt(columnIndex)==0);
                LinearLayout ll = (LinearLayout) view.getParent();
                TextView menge = (TextView) ll.findViewById(R.id.menge);
                TextView einheit = (TextView) ll.findViewById(R.id.einheit);
                TextView zutat = (TextView) ll.findViewById(R.id.zutat);
                if (cb.isChecked()) {
                    menge.setTextColor(Color.LTGRAY);
                    menge.setPaintFlags(menge.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    einheit.setTextColor(Color.LTGRAY);
                    einheit.setPaintFlags(einheit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    zutat.setTextColor(Color.LTGRAY);
                    zutat.setPaintFlags(zutat.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);                }
                else {
                    menge.setTextColor(getResources().getColor(android.R.color.tertiary_text_dark));
                    menge.setPaintFlags(menge.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    einheit.setTextColor(getResources().getColor(android.R.color.tertiary_text_dark));
                    einheit.setPaintFlags(einheit.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    zutat.setTextColor(getResources().getColor(android.R.color.tertiary_text_dark));
                    zutat.setPaintFlags(zutat.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                }
                return true;
            }
            if(columnIndex == DatenBank.sqlEinkaufsListe._id.getColumn()){
                if(editMode)view.setVisibility(View.VISIBLE);
                else view.setVisibility(View.GONE);
                return true;
            }
            return false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        DatenBank.DataBaseCallback callback = (DatenBank.DataBaseCallback) getActivity();
        dbRezepte=callback.getDatenBank();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.einkaufsliste, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_delete) {
            AlertDialog ad = new AlertDialog.Builder(vi.getContext())
                    .setTitle(getText(R.string.loeschen_zutaten))
                    .setMessage(getText(R.string.loeschen_zutaten_text))
                    .setPositiveButton(getText(R.string.ALLE), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dbRezepte.db.delete("einkaufsListe", "", null);
                            ListView liste = (ListView) vi.findViewById(R.id.ek_liste);
                            MySimpleCursorAdapter ca = (MySimpleCursorAdapter) liste.getAdapter();
                            ca.getCursor().requery();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(getText(R.string.MARKIERTE), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dbRezepte.db.delete("einkaufsListe", "ok = 0", null);
                            ListView liste = (ListView) vi.findViewById(R.id.ek_liste);
                            MySimpleCursorAdapter ca = (MySimpleCursorAdapter) liste.getAdapter();
                            ca.getCursor().requery();
                            dialog.dismiss();
                        }
                    })
                    .setNeutralButton(R.string.abbrechen, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setCancelable(false)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();           return true;
        }
        if (id == R.id.action_edit) {
            editMode = !editMode;
            ListView liste = (ListView) vi.findViewById(R.id.ek_liste);
            MySimpleCursorAdapter ca = (MySimpleCursorAdapter) liste.getAdapter();
            ca.getCursor().requery();
            return true;
        }
        if (id == R.id.action_share) {
            DecimalFormat df = new DecimalFormat("0.###");
            String EkListe = getText(R.string.ek_liste_share_text).toString();
            Cursor cursor = dbRezepte.db.rawQuery(sql_einkaufslisteSend, null);
            if(cursor.moveToFirst()){
                do{
                    EkListe = EkListe + "\n" + df.format(cursor.getDouble(DatenBank.sqlEinkaufsListe.menge.getColumn())) + " ";
                    EkListe = EkListe + cursor.getString(DatenBank.sqlEinkaufsListe.einheit.getColumn()) + " ";
                    EkListe = EkListe + cursor.getString(DatenBank.sqlEinkaufsListe.bezeichnung.getColumn());
                }
                while (cursor.moveToNext());
            }
            Intent send = new Intent();
            send.setAction(Intent.ACTION_SEND);
            send.putExtra(Intent.EXTRA_TEXT, EkListe);
            send.setType("text/plain");
            startActivity(send);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_einkaufsliste, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vi = view;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Cursor cursor;
        if (prefs.getBoolean("e_ok_unten",false)) {
            cursor = dbRezepte.db.rawQuery(sql_einkaufsliste2, null);
        }
        else {
            cursor = dbRezepte.db.rawQuery(sql_einkaufsliste1, null);
        }
        ListView liste = (ListView) vi.findViewById(R.id.ek_liste);

        String[] anzeigeSpalten = new String[] {"menge", "einheit", "bezeichnung", "ok", "_id", "zut", "einh"};
        int[] anzeigeViews = new int[] {R.id.menge, R.id.einheit, R.id.zutat, R.id.cb_ok, R.id.btn_delete};
        MySimpleCursorAdapter adapter1 = new MySimpleCursorAdapter(vi.getContext(), R.layout.zutaten_datensatz, cursor, anzeigeSpalten, anzeigeViews, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        adapter1.setViewBinder(new EinkaufsListe.EkListeViewBinder());
        liste.setAdapter(adapter1);
        liste.setEmptyView(vi.findViewById(R.id.empty));
     }

    @Override
    public void onResume() {
        super.onResume();
        ReloadEinkaufsListe();
    }

    public void ReloadEinkaufsListe() {
        ListView liste = (ListView) vi.findViewById(R.id.ek_liste);
        MySimpleCursorAdapter ca = (MySimpleCursorAdapter) liste.getAdapter();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Cursor cursor;
        if (prefs.getBoolean("e_ok_unten",false)) {
            cursor = dbRezepte.db.rawQuery(sql_einkaufsliste2, null);
        }
        else {
            cursor = dbRezepte.db.rawQuery(sql_einkaufsliste1, null);
        }
        ca.changeCursor(cursor);
    }
}
