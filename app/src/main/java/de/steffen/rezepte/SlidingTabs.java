package de.steffen.rezepte;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static de.steffen.rezepte.R.color.colorAccent;
import static de.steffen.rezepte.R.color.colorPrimaryDark;


public class SlidingTabs extends Fragment {
    public static String TAG = "SlidingTabs";
    private long filterZutId = -1;
    List<Zutaten> ZutatenFromDB = new ArrayList<Zutaten>();
    DatenBank dbRezepte;
    MenuItem icon_filter;

    static class RezeptePagerItem {
        private final String mTitle;
        private final long mkatId;
        private long mZutID;
        private final int mFarbe;
        private final int mRahmen;
        private RezeptListeFragment mFragment;

        RezeptePagerItem(String title, long katId, long zutID, int farbe, int rahmen) {
            mTitle = title;
            mkatId = katId;
            mZutID = zutID;
            mFarbe = farbe;
            mRahmen = rahmen;
        }

        Fragment createFragment() {
            mFragment = RezeptListeFragment.newInstance(mTitle, mkatId, mZutID ,mFarbe);
            return mFragment;
        }

        CharSequence getTitle() { return mTitle; }
        int getIndicatorColor() { return mFarbe; }
        int getDividerColor() { return mRahmen; }
    }

    private SlidingTabLayout mSlidingTabLayout;
    private ViewPager mViewPager;
    private List<RezeptePagerItem> mTabs = new ArrayList<RezeptePagerItem>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        DatenBank.DataBaseCallback callback = (DatenBank.DataBaseCallback) getActivity();
        dbRezepte=callback.getDatenBank();
        Cursor zutaten = dbRezepte.db.rawQuery("SELECT _id, bez_singular, bez_plural FROM zutaten",null);
        while (zutaten.moveToNext()) {
            ZutatenFromDB.add(new Zutaten(zutaten.getLong(0), zutaten.getString(1), zutaten.getString(2)));
        }
        PopulatePager();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.rezeptliste, menu);
        icon_filter = menu.findItem(R.id.action_filter);
        String subTitle = "";
        for (int p = 0; p < ZutatenFromDB.size(); p++) {
            if(ZutatenFromDB.get(p).id == filterZutId) {
                subTitle=getString(R.string.filter_mit,ZutatenFromDB.get(p).bez_singular);
                break;
            }
        }

        if(filterZutId>-1){
            icon_filter.setIcon(R.drawable.ic_svg_filter_red);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(subTitle);
        }else{
            icon_filter.setIcon(R.drawable.ic_svg_filter);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("");
        }
    }

    @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rezepte, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewPager = (ViewPager) getView().findViewById(R.id.viewpager);
        mViewPager.setAdapter(new myFragmentPagerAdapter(getChildFragmentManager()));
        mSlidingTabLayout = (SlidingTabLayout) getView().getRootView().findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);

        mSlidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {

            @Override
            public int getIndicatorColor(int position) {
                return mTabs.get(position).getIndicatorColor();
            }

            @Override
            public int getDividerColor(int position) {
                return mTabs.get(position).getDividerColor();
            }

        });
        PopulatePager();
    }

    @Override
    public void onPause() {
        super.onPause();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("");
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                Intent edit = new Intent(getContext(), de.steffen.rezepte.EinRezeptEdit.class);
                edit.putExtra("Rezept", Long.valueOf(-1));
                startActivity(edit);
                return true;
            case R.id.action_download:
                Intent intent = new Intent(getContext(), ServerActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_filter:
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(getText(R.string.filtern));
                LinearLayout ll = new LinearLayout(getContext());
                ll.setOrientation(LinearLayout.VERTICAL);
                TextView tv = new TextView(getContext());
                tv.setText(getText(R.string.filtern_text));
                ll.addView(tv);
                final AutoCompleteTextView atv = new AutoCompleteTextView(getContext());
                ArrayAdapter<Zutaten> zutAdapter = new ArrayAdapter<Zutaten>(getContext(),R.layout.my_autocomplete_dropdown,ZutatenFromDB);
                atv.setValidator(new ZutatenValidator());
                atv.setThreshold(1);
                atv.setText("");
                atv.setDropDownBackgroundResource(R.color.colorPrimaryDark);
                for (int p = 0; p < zutAdapter.getCount(); p++) {
                    Zutaten mZutat = (Zutaten) zutAdapter.getItem(p);
                    if(mZutat.id == filterZutId) {
                        atv.setText(mZutat.toString());
                        break;
                    }
                }
                atv.setAdapter(zutAdapter);
                ll.addView(atv);
                builder.setView(ll, 50, 0, 50, 0);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        long iDFound = -1;
                        int i;
                        for ( i=0; i<ZutatenFromDB.size();i++) {
                            if (atv.getText().toString().equals(ZutatenFromDB.get(i).bez_singular) ||
                                    atv.getText().toString().equals(ZutatenFromDB.get(i).bez_plural)) {
                                iDFound=ZutatenFromDB.get(i).id;
                                break;
                            }
                        }
                        if(iDFound>-1){
                            icon_filter.setIcon(R.drawable.ic_svg_filter_red);
                            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(getString(R.string.filter_mit,ZutatenFromDB.get(i).bez_singular));
                        }else{
                            icon_filter.setIcon(R.drawable.ic_svg_filter);
                            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("");
                        }

                        filterZutId = iDFound;
                        PopulatePager();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setNeutralButton(getText(R.string.filter_loeschen), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        filterZutId = -1;
                        PopulatePager();
                        icon_filter.setIcon(R.drawable.ic_svg_filter);
                        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("");
                        dialog.dismiss();
                    }
                });
                AlertDialog a = builder.create();
                a.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                a.show();


                return true;
        }
        return false;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

    }

        public void PopulatePager(){
            mTabs.clear();
            DatenBank.DataBaseCallback callback = (DatenBank.DataBaseCallback) getActivity();
            DatenBank dbRezepte=callback.getDatenBank();
            Cursor kategorienCursor = dbRezepte.erzeugeKategorienCursor();
            int anzahl = kategorienCursor.getCount();
            kategorienCursor.moveToFirst();
            Cursor rezepteCursor;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if (prefs.getBoolean("e_alle_rezepte",true)) {
                rezepteCursor = dbRezepte.erzeugeRezepteCursor(-1, filterZutId);
                mTabs.add(new RezeptePagerItem(getString(R.string.rezepte_alle, rezepteCursor.getCount()) , -1, filterZutId, getResources().getColor(colorAccent), Color.GRAY));
            }
            for(int i = 0; i<anzahl; i++) {
                rezepteCursor = dbRezepte.erzeugeRezepteCursor(kategorienCursor.getInt(0), filterZutId);
                mTabs.add(new RezeptePagerItem(kategorienCursor.getString(1)+" ("+rezepteCursor.getCount()+")", kategorienCursor.getInt(0), filterZutId, getResources().getColor(colorPrimaryDark), Color.GRAY));
                kategorienCursor.moveToNext();
            }
            if (mViewPager!=null) {
                mViewPager.getAdapter().notifyDataSetChanged();
                mSlidingTabLayout.setViewPager(mViewPager);
            }
        }

        class myFragmentPagerAdapter extends FragmentStatePagerAdapter {
            myFragmentPagerAdapter(FragmentManager fm) {
                super(fm);
            }
            public Fragment getItem(int i) {
                return mTabs.get(i).createFragment();
            }
            public int getItemPosition(Object object) {
                return POSITION_NONE;
            }

            @Override
            public int getCount() {
                return mTabs.size();
            }

           @Override
            public CharSequence getPageTitle(int position) {
                return mTabs.get(position).getTitle();
            }
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
    }
