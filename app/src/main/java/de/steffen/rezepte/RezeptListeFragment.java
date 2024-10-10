package de.steffen.rezepte;

import android.app.Dialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class RezeptListeFragment extends Fragment{

    DatenBank dbRezepte;
    ActionMode mActionMode;

    public static RezeptListeFragment newInstance(String name, long kat_id, long zut_id, int farbe) {
        Bundle bundle = new Bundle();
        bundle.putCharSequence("Kategorie", name);
        bundle.putLong("Kat_id", kat_id);
        bundle.putLong("Zut_id", zut_id);
        bundle.putInt("Farbe", farbe);
        RezeptListeFragment fragment = new RezeptListeFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public void changeArgs(String name, long kat_id, long zut_id, int farbe) {
        this.getArguments().putCharSequence("Kategorie", name);
        this.getArguments().putLong("Kat_id", kat_id);
        this.getArguments().putLong("Zut_id", zut_id);
        this.getArguments().putInt("Farbe", farbe);
        newCursor(kat_id, zut_id);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.rezepte_pager, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            newCursor(args.getLong("Kat_id"), args.getLong("Zut_id"));
        }
    }

    public void newCursor(long katID, long zutID){
        DatenBank.DataBaseCallback callback = (DatenBank.DataBaseCallback) getActivity();
        dbRezepte=callback.getDatenBank();

        Cursor cursor = dbRezepte.erzeugeRezepteCursor(katID, zutID);

        final ListView liste = (ListView) getView().findViewById(R.id.RezepteListe);
        String[] anzeigeSpalten = new String[] {"foto", "name"};
        int[] anzeigeViews = new int[] {R.id.RezeptBild, R.id.RezeptName};
        SimpleCursorAdapter adapter;
        adapter = new SimpleCursorAdapter(getView().getContext(), R.layout.datensatz, cursor, anzeigeSpalten, anzeigeViews, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder(){
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if(columnIndex == DatenBank.tblRezepte.foto.getColumn()){
                    final byte[] foto = cursor.getBlob(columnIndex);
                    if(foto != null){
                        Bitmap bm = BitmapFactory.decodeByteArray(foto, 0, foto.length);
                        ImageView img = (ImageView) view;
                        img.setImageBitmap(bm);
                        img.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
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
                                //byte[] foto = cursor.getBlob(DatenBank.tblRezepte.foto.getColumn());
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
                        return true;
                    }
                }
                return false;
            }
        });
        liste.setAdapter(adapter);
        liste.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView av, View v, int pos, long Rid) {
                ListeSelektiertCallback callback = (ListeSelektiertCallback) getActivity();
                callback.Selektiert(Rid);

            }
        });
    }

    public void reloadData() {
        ListView liste = (ListView) this.getView().findViewById(R.id.RezepteListe);
        SimpleCursorAdapter adapter = (SimpleCursorAdapter) liste.getAdapter();
        adapter.getCursor().requery();
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadData();

    }

    public interface ListeSelektiertCallback{
        public void Selektiert(long Rid);
    }

}

