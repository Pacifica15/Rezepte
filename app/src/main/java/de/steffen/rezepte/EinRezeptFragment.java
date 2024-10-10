package de.steffen.rezepte;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class EinRezeptFragment extends Fragment {

    class ZutatenViewBinder implements ViewBinder {

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if(columnIndex == DatenBank.tblZutatenListeJoin.menge.getColumn()){
                TextView text = (TextView) view;
                double menge = (cursor.getFloat(columnIndex)/portionen)*umrechnung;
                DecimalFormat decimalFormat = new DecimalFormat("#.###");
                if (menge==0) {text.setText("");} else {text.setText(decimalFormat.format(menge));}
                return true;
            }
            if(columnIndex == DatenBank.tblZutatenListeJoin.einh_singular.getColumn()){
                TextView text = (TextView) view;
                double menge = (cursor.getFloat(DatenBank.tblZutatenListeJoin.menge.getColumn())/portionen)*umrechnung;
                if(menge==1){text.setText(cursor.getString(DatenBank.tblZutatenListeJoin.einh_singular.getColumn()));}
                else {text.setText(cursor.getString(DatenBank.tblZutatenListeJoin.einh_plural.getColumn()));}
                return true;
            }
            if(columnIndex == DatenBank.tblZutatenListeJoin.bez_singular.getColumn()){
                TextView text = (TextView) view;
                double menge = (cursor.getFloat(DatenBank.tblZutatenListeJoin.menge.getColumn())/portionen)*umrechnung;
                if(menge==1){text.setText(cursor.getString(DatenBank.tblZutatenListeJoin.bez_singular.getColumn()));}
                else {text.setText(cursor.getString(DatenBank.tblZutatenListeJoin.bez_plural.getColumn()));}
                return true;
            }
            return false;
        }
    }

    static class ViewHolder {
        public ImageView bild;
        public TextView name;
        public TextView portionen;
        public ListView[] zListe = new ListView[5];
        public TextView[] ueberschrift = new TextView[5];
        public TextView zubereitung;
        public LinearLayout mainLayout;
        public LinearLayout rezeptKopf;
        public TextView emptyLine;
        public ScrollView rezeptScroll;
    }

    public static String TAG = "EinRezept";

    DatenBank dbRezepte;
    int portionen;
    int umrechnung = -1;
    long RId = -1;
    Cursor rezepteCursor;
    ViewHolder viewHolder = new ViewHolder();
    List<File> tmpShareFile = new ArrayList<File>();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DatenBank.DataBaseCallback callback = (DatenBank.DataBaseCallback) getActivity();
        dbRezepte=callback.getDatenBank();
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        for (int i=0;i<tmpShareFile.size(); i++) {
            tmpShareFile.get(i).delete();
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        aktualisieren(RId);
        getActivity().findViewById(R.id.fab).setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        getActivity().findViewById(R.id.fab).setVisibility(View.GONE);
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ein_rezept, menu);
    }

     public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                Intent edit = new Intent(this.getContext(), de.steffen.rezepte.EinRezeptEdit.class);
                edit.putExtra("Rezept", RId);
                startActivity(edit);
                return true;

            case R.id.action_personen:
                AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
                builder.setTitle(R.string.anzahl_personen);

                final EditText input = new EditText(this.getContext());
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setText(String.valueOf(portionen));
                input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(2)});
                builder.setView(input, 50, 0, 50, 0);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        umrechnung = Integer.valueOf(input.getText().toString());
                        dialog.dismiss();
                        setListView();
                    }
                });

               AlertDialog alertDialog = builder.create();
               alertDialog.show();
               return true;

            case R.id.action_export:
                try {
                    XMLExport exporter = new XMLExport(this.getContext(),dbRezepte.db,this.getContext().getExternalFilesDir("").getPath());
                    exporter.ExportRezept(RId);
                    Toast.makeText(getContext() ,getString(R.string.rezept_exportiert,this.getContext().getExternalFilesDir("").getPath()), Toast.LENGTH_LONG).show();               }
                catch (Exception e) {
                    Toast.makeText(getContext() ,e.getMessage(), Toast.LENGTH_LONG).show();
                }
                return true;

            case R.id.action_share:
                Bitmap bmp = null;
                int totalWidth = viewHolder.mainLayout.getWidth();
                int totalHeight = 0;
                totalHeight = totalHeight + viewHolder.rezeptKopf.getHeight();
                totalHeight = totalHeight + viewHolder.portionen.getHeight();
                totalHeight = totalHeight + viewHolder.emptyLine.getHeight();
                totalHeight = totalHeight + viewHolder.rezeptScroll.getChildAt(0).getHeight();
                bmp = Bitmap.createBitmap(totalWidth,totalHeight , Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);
                Drawable bgDrawable = viewHolder.mainLayout.getBackground();
                if (bgDrawable != null) bgDrawable.draw(canvas);
                    else canvas.drawColor(Color.WHITE);
                viewHolder.rezeptKopf.draw(canvas);
                canvas.translate(0,viewHolder.rezeptKopf.getHeight());
                viewHolder.portionen.draw(canvas);
                canvas.translate(0,viewHolder.portionen.getHeight());
                viewHolder.emptyLine.draw(canvas);
                canvas.translate(0,viewHolder.emptyLine.getHeight());

                //viewHolder.rezeptScroll.measure(totalWidth, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                viewHolder.rezeptScroll.layout(0, 0, 30, viewHolder.rezeptScroll.getChildAt(0).getHeight());
                //viewHolder.rezeptScroll.requestLayout();
                viewHolder.rezeptScroll.setDrawingCacheEnabled(true);
                viewHolder.rezeptScroll.buildDrawingCache();
                viewHolder.rezeptScroll.getDrawingCache();
                viewHolder.rezeptScroll.draw(canvas);

                //LinearLayout ll; ll.get

                canvas.translate(0,totalHeight-viewHolder.rezeptScroll.getHeight());
                /*for (int j = 0; j < 5; j++) {
                    ((LinearLayout) viewHolder.rezeptScroll.getChildAt(0)).getChildAt(j*2).draw(canvas);
                    canvas.translate(0,((LinearLayout) viewHolder.rezeptScroll.getChildAt(0)).getChildAt(j*2).getHeight());
                    ListAdapter adapter =((ListView) ((LinearLayout) viewHolder.rezeptScroll.getChildAt(0)).getChildAt((j*2)+1)).getAdapter();
                    for (int i = 0; i < adapter.getCount(); i++) {
                        View childView = adapter.getView(i, null, null);
                        childView.measure(totalWidth, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                        childView.layout(0, 0, childView.getMeasuredWidth(), childView.getMeasuredHeight());
                        childView.requestLayout();
                        childView.setDrawingCacheEnabled(true);
                        childView.buildDrawingCache();
                        childView.getDrawingCache();
                        childView.draw(canvas);
                        canvas.translate(0, childView.getHeight());
                    }
                }
                ((LinearLayout) viewHolder.rezeptScroll.getChildAt(0)).getChildAt(10).draw(canvas);
                canvas.translate(0,((LinearLayout) viewHolder.rezeptScroll.getChildAt(0)).getChildAt(10).getHeight());
                ((LinearLayout) viewHolder.rezeptScroll.getChildAt(0)).getChildAt(11).draw(canvas);
                canvas.translate(0,((LinearLayout) viewHolder.rezeptScroll.getChildAt(0)).getChildAt(11).getHeight());
                */
                Uri bmpUri = null;
                try {
                    //File file = new File(this.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + System.currentTimeMillis() + ".jpg");
                    tmpShareFile.add (File.createTempFile("rezept_share",".jpg",this.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)));
                    FileOutputStream out = new FileOutputStream(tmpShareFile.get(tmpShareFile.size()-1));
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.close();
                    try {
                        bmpUri = FileProvider.getUriForFile(this.getContext(), "de.steffen.rezepte.fileprovider", tmpShareFile.get(tmpShareFile.size()-1));
                        Intent send = new Intent();
                        send.setAction(Intent.ACTION_SEND);
                        send.putExtra(Intent.EXTRA_TEXT, this.getString(R.string.rezept_share_name,viewHolder.name.getText()));
                        send.setType("text/plain");
                        send.putExtra(Intent.EXTRA_STREAM, bmpUri);
                        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        send.setType("image/*");
                        startActivity(Intent.createChooser(send,getText(R.string.senden_mit)));
                    } catch (Exception e) {
                        Toast.makeText(this.getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(this.getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
                return true;

            case R.id.action_delete:
                AlertDialog.Builder build = new AlertDialog.Builder(getContext());
                build.setTitle(getText(R.string.löschen));
                build.setMessage(getText(R.string.löschen_text));
                build.setPositiveButton(getText(R.string.JA), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbRezepte.db.delete("zutatenliste", "rezeptID = " + RId, null);
                        dbRezepte.db.delete("rezepte", "_id = " + RId, null);
                        getActivity().onBackPressed();
                    }
                });
                build.setNegativeButton(getText(R.string.NEIN), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                build.show();

                return true;

            case R.id.action_upload:
                //Behandlung in MainActivity
                return false;



        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ein_rezept, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            long Id = args.getLong("Rezept_id");
            RId = (int) Id;
        }
        if(savedInstanceState != null) {
            umrechnung = savedInstanceState.getInt("umrechnung");
            RId = savedInstanceState.getLong("Rid");
        }

        viewHolder.bild = (ImageView) view.findViewById(R.id.RezeptBild);
        viewHolder.name = (TextView) view.findViewById(R.id.RezeptName);
        viewHolder.portionen = (TextView) view.findViewById(R.id.portionen);
        viewHolder.zListe[0] = (ListView) view.findViewById(R.id.zutaten);
        viewHolder.zListe[1] = (ListView) view.findViewById(R.id.zutaten2);
        viewHolder.zListe[2] = (ListView) view.findViewById(R.id.zutaten3);
        viewHolder.zListe[3] = (ListView) view.findViewById(R.id.zutaten4);
        viewHolder.zListe[4] = (ListView) view.findViewById(R.id.zutaten5);
        viewHolder.ueberschrift[0] = (TextView) view.findViewById(R.id.ueberschriftzutaten1);
        viewHolder.ueberschrift[1] = (TextView) view.findViewById(R.id.ueberschriftzutaten2);
        viewHolder.ueberschrift[2] = (TextView) view.findViewById(R.id.ueberschriftzutaten3);
        viewHolder.ueberschrift[3] = (TextView) view.findViewById(R.id.ueberschriftzutaten4);
        viewHolder.ueberschrift[4] = (TextView) view.findViewById(R.id.ueberschriftzutaten5);
        viewHolder.zubereitung = (TextView) view.findViewById(R.id.zubereitung);
        viewHolder.mainLayout = (LinearLayout) view.findViewById(R.id.ll_ein_rezept);
        viewHolder.rezeptKopf = (LinearLayout) view.findViewById(R.id.rezept_kopf);
        viewHolder.emptyLine = (TextView) view.findViewById(R.id.empty_line);
        viewHolder.rezeptScroll = (ScrollView) view.findViewById(R.id.rezept_scroll);

        final ImageView bild = viewHolder.bild;
        bild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog builder = new Dialog(getContext());
                ImageView iv = new ImageView(getContext());
                iv.setOnClickListener(new View.OnClickListener() {
                    boolean isMP = true;
                    long lastClickTime = 0;
                    @Override
                    public void onClick(View v) {
                        long clickTime = System.currentTimeMillis();
                        if (clickTime - lastClickTime < 300){
                            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) v.getLayoutParams();
                            if(isMP){
                                layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                layoutParams.gravity = Gravity.CENTER;
                                ((ImageView)v).setScaleType(ImageView.ScaleType.CENTER);
                            }else{
                                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                                ((ImageView)v).setScaleType(ImageView.ScaleType.FIT_CENTER);
                            }
                            isMP=!isMP;
                            v.setLayoutParams(layoutParams);
                        }
                        lastClickTime = clickTime;
                    }
                });
                byte[] foto = rezepteCursor.getBlob(DatenBank.tblRezepte.foto.getColumn());
                if(foto != null) {
                    Bitmap bm = BitmapFactory.decodeByteArray(foto, 0, foto.length);
                    iv.setImageBitmap(bm);
                }
                builder.addContentView(iv,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                builder.show();
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                Window window = builder.getWindow();
                window.setBackgroundDrawable(new ColorDrawable(0x00000000));
                lp.copyFrom(window.getAttributes());
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.MATCH_PARENT;
                window.setAttributes(lp);

            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("umrechnung",umrechnung);
        outState.putLong("Rid", RId);
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

        int totalHeight = listAdapter.getCount() * convertDipToPixels(28);
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);

    }
    public int convertDipToPixels(float dips)
    {
        return (int) (dips * getActivity().getResources().getDisplayMetrics().density + 0.5f);
    }

    void aktualisieren(long mRId) {
        RId=mRId;
        if (RId>0) {

            RId = mRId;
            rezepteCursor = dbRezepte.erzeugeEinRezeptCursor(RId);
            rezepteCursor.moveToFirst();
            portionen = rezepteCursor.getInt(DatenBank.tblRezepte.portionen.getColumn());
            if (portionen == 0) {
                portionen = 1;
            }
            if (umrechnung <= 0) {
                umrechnung = portionen;
            }
            setListView();
        }
    }

    void setListView(){
        Cursor[] zutatenCursor = new Cursor[5];

        viewHolder.name.setText(rezepteCursor.getString(DatenBank.tblRezepte.name.getColumn()));
        if (umrechnung == 1) {
            viewHolder.portionen.setText(R.string.anzahl_1);
        }
        else {
            viewHolder.portionen.setText(this.getString(R.string.anzahl_mehrere,umrechnung));
        }

        byte[] foto = rezepteCursor.getBlob(DatenBank.tblRezepte.foto.getColumn());
        if(foto != null) {
            Bitmap bm = BitmapFactory.decodeByteArray(foto, 0, foto.length);
            viewHolder.bild.setImageBitmap(bm);
        }

        if (rezepteCursor.getString(DatenBank.tblRezepte.teil1.getColumn()).equals("")){
            zutatenCursor[0] = dbRezepte.erzeugeZutatenListeCursor(RId, -1);
        }
        else{
            for (int i=0; i<5; i++){
                zutatenCursor[i] = dbRezepte.erzeugeZutatenListeCursor(RId, i+1);
            }
        }

        String[] schritt = new String[5];
        schritt[0] = rezepteCursor.getString(DatenBank.tblRezepte.teil1.getColumn());
        schritt[1] = rezepteCursor.getString(DatenBank.tblRezepte.teil2.getColumn());
        schritt[2] = rezepteCursor.getString(DatenBank.tblRezepte.teil3.getColumn());
        schritt[3] = rezepteCursor.getString(DatenBank.tblRezepte.teil4.getColumn());
        schritt[4] = rezepteCursor.getString(DatenBank.tblRezepte.teil5.getColumn());

        for (int i=0; i<5; i++){
            if (!schritt[i].equals("") || i==0) {
                viewHolder.ueberschrift[i].setText(this.getString(R.string.zutaten)+" "+schritt[i]);
                viewHolder.ueberschrift[i].setVisibility(View.VISIBLE);
                viewHolder.zListe[i].setVisibility(View.VISIBLE);
            } else {
                viewHolder.ueberschrift[i].setVisibility(View.GONE);
                viewHolder.zListe[i].setVisibility(View.GONE);
            }
        }

        viewHolder.zubereitung.setText(rezepteCursor.getString(DatenBank.tblRezepte.anleitung.getColumn()));

        String[] anzeigeSpalten = new String[] {"menge", "einh_singular", "bez_singular"};
        int[] anzeigeViews = new int[] {R.id.menge, R.id.einheit, R.id.zutat};

        SimpleCursorAdapter[] adapter = new SimpleCursorAdapter[5];

        for (int i=0; i<5; i++){
            adapter[i] = new SimpleCursorAdapter(this.getContext(), R.layout.zutaten_datensatz, zutatenCursor[i], anzeigeSpalten, anzeigeViews, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            adapter[i].setViewBinder(new ZutatenViewBinder());
            viewHolder.zListe[i].setAdapter(adapter[i]);
            setListViewHeightBasedOnChildren(viewHolder.zListe[i]);
        }
     }

    public void EinkaufsListeAktualisieren(){
        dbRezepte.db.rawQuery("insert into einkaufsliste select null, rezeptID, rezeptteil, zutat, menge*"+String.valueOf((1.0*umrechnung)/portionen)+", einheit, -1 from zutatenliste where rezeptID="+String.valueOf(RId), null).moveToFirst();
    }

}