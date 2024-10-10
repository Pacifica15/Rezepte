package de.steffen.rezepte;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class DatenBank extends SQLiteOpenHelper {


    public static final String[] RezepteSpalten = new String[]{ "_id",
                                                                "name",
                                                                "kategorie",
                                                                "portionen",
                                                                "user",
                                                                "version",
                                                                "teil1",
                                                                "teil2",
                                                                "teil3",
                                                                "teil4",
                                                                "teil5",
                                                                "anleitung",
                                                                "foto"};
    public enum tblRezepte {
        _id(0), name(1), kategorie(2), portionen(3), user(4), version(5), teil1(6), teil2(7), teil3(8),
        teil4(9), teil5(10), anleitung(11), foto(12);
        private int column;
        private tblRezepte(int mcolumn) {
            this.column=mcolumn;
        }
        public int getColumn() {
            return this.column;
        }
    }

    public static final String[] KategorienSpalten = new String[]{"_id",
                                                                  "kategorie"};
    public enum tblKategorien {
        _id(0), kategorie(1);
        private int column;
        private tblKategorien(int mcolumn) {
            this.column=mcolumn;
        }
        public int getColumn() {
            return this.column;
        }
    }
    public static final String ZutatenSpaltenJoin = "zutatenListe._id, rezeptID, rezeptteil, zutat, menge, "+
                                                "einheit, einh_singular, einh_plural, bez_singular, bez_plural";

    public static final String[] ZutatenSpalten = new String[]{ "_id",
                                                                "bez_singular",
                                                                "bez_plural",
                                                                "einheit_id",
                                                                "shop",
                                                                "packungsgroesse",
                                                                "pack_einheit_id",
                                                                "preis"};

    public enum tblZutaten{
        _id(0), bez_singular(1), bez_plural(2), einheit_id(3), shop(4), packungsgroesse(5), pack_einh_id(6), preis(7);
        private int column;
        private tblZutaten(int mcolumn){
            this.column = mcolumn;
        }
        public int getColumn() {
            return column;
        }
    }

    public static final String[] ZutatenListeSpalten = new String[]{"_id",
                                                                "rezeptID",
                                                                "rezeptteil",
                                                                "zutat",
                                                                "menge",
                                                                "einheit"};

    public static final String ZutatenStammJoin = "zutaten._id, zutaten.bez_singular, einheiten.einh_singular, zutaten.shop, zutaten.preis, rezeptID as rez_id";

    public enum tblZutatenListe{
        _id(0), rezeptID(1), rezeptteil(2), zutat(3), menge(4), einheit(5);
        private int column;
        private tblZutatenListe(int mcolumn){
            this.column = mcolumn;
        }
        public int getColumn() {
            return column;
        }
    }

    public enum tblZutatenListeJoin{
        _id(0), rezept(1), rezeptteil(2), zutat(3), menge(4), einheit(5), einh_singular(6),
        einh_plural(7), bez_singular(8), bez_plural(9);
        private int column;
        private tblZutatenListeJoin(int mcolumn){
            this.column = mcolumn;
        }
        public int getColumn() {
            return column;
        }
    }


    public static final String[] EinheitenSpalten = new String[]{   "_id",
                                                                    "einh_singular",
                                                                    "einh_plural",
                                                                    "langtext",
                                                                    "basis_id_e",
                                                                    "faktor_e",
                                                                    "basis_id_k",
                                                                    "faktor_k"};

    public enum tblEinheiten {
        _id(0), einh_singular(1), einh_plural(2), langtext(3), basis_id_e(4), faktor_e(5),
        basis_id_k(6), faktor_k(7);
        private int column;
        private tblEinheiten(int mcolumn) {this.column = mcolumn;}
        public int getColumn() {return column;}
    }

    public enum sqlEinkaufsListe{
        _id(0), menge(1), einheit(2), bezeichnung(3), ok(4), zut(5), einh(6);
        private int column;
        private sqlEinkaufsListe(int mcolumn){
            this.column = mcolumn;
        }
        public int getColumn() {
            return column;
        }
    }

    public SQLiteDatabase db;
    private Toast toast;
    private  Context myContext;

    public DatenBank(Context context, String name, SQLiteDatabase.CursorFactory factory) {
        super(context, name, null, 1);
        myContext=context;
        db = getWritableDatabase();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try{
            String kategorien =  "CREATE TABLE kategorien"
                    + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "kategorie VARCHAR(30) NOT NULL)";
            db.execSQL(kategorien);
            SQLfromAsset(db,"kategorien.sql");

            String zutaten =  "CREATE TABLE zutaten"
                            + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "bez_singular VARCHAR(30),"
                            + "bez_plural VARCHAR(30),"
                            + "einheit_id INTEGER,"
                            + "shop VARCHAR(30),"
                            + "packungsgroesse REAL,"
                            + "pack_einheit_id INTEGER,"
                            + "preis REAL)";
            db.execSQL(zutaten);
            SQLfromAsset(db,"zutaten.sql");

            String einheiten =  "CREATE TABLE einheiten"
                    + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "einh_singular VARCHAR(10),"
                    + "einh_plural VARCHAR(10),"
                    + "langtext VARCHAR(20),"
                    + "basis_id_e INTEGER,"
                    + "faktor_e REAL,"
                    + "basis_id_k INTEGER,"
                    + "faktor_k REAL)";
            db.execSQL(einheiten);
            SQLfromAsset(db,"einheiten.sql");

            String rezepte =  "CREATE TABLE rezepte"
                    + "( _id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name VARCHAR(40) NOT NULL,"
                    + "kategorie INTEGER,"
                    + "portionen INTEGER,"
                    + "user VARCHAR(40),"
                    + "version INTEGER,"
                    + "teil1 VARCHAR(30),"
                    + "teil2 VARCHAR(30),"
                    + "teil3 VARCHAR(30),"
                    + "teil4 VARCHAR(30),"
                    + "teil5 VARCHAR(30),"
                    + "anleitung TEXT,"
                    + "foto BLOB)";
            db.execSQL(rezepte);

            String zutatenListe =  "CREATE TABLE zutatenListe"
                                 + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                 + "rezeptID INTEGER NOT NULL,"
                                 + "rezeptteil INTEGER,"
                                 + "zutat INTEGER NOT NULL,"
                                 + "menge REAL,"
                                 + "einheit INTEGER)";
            db.execSQL(zutatenListe);


            String einkaufsListe =  "CREATE TABLE einkaufsListe"
                                  + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                  + "rezeptID INTEGER NOT NULL,"
                                  + "rezeptteil INTEGER,"
                                  + "zutat INTEGER NOT NULL,"
                                  + "menge REAL,"
                                  + "einheit INTEGER,"
                                  + "ok BOOLEAN)";
            db.execSQL(einkaufsListe);

            String umrechnung =  "CREATE TABLE umrechnung"
                    + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "zutat_id INTEGER,"
                    + "einheit_id INTEGER,"
                    + "einh_neu_id INTEGER,"
                    + "faktor REAL)";
            db.execSQL(umrechnung);
            SQLfromAsset(db,"umrechnung.sql");
        }
        catch(Exception ex){
            toast.makeText(myContext ,ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    @Override
    public synchronized void close(){
        if (db != null){
            db.close();
            db = null;
        }
        super.close();
    }


    public interface DataBaseCallback  {
        public DatenBank getDatenBank();
        public String getDBPfad();
    }

    private void SQLfromAsset(SQLiteDatabase db, String AssetName) {
        BufferedReader myInput = null;
        String SQLStatement;

        try {
            myInput = new BufferedReader(new InputStreamReader(myContext.getAssets().open(AssetName),"UTF-8"));
                do {
                    SQLStatement = myInput.readLine();
                    if (SQLStatement != null) {
                        db.execSQL(SQLStatement);
                    }
                } while (SQLStatement != null);
            }
            catch (IOException ex) {
                toast.makeText(myContext ,ex.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (myInput != null) {myInput.close();}
            } catch (IOException ex) {
                toast.makeText(myContext ,ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public Cursor erzeugeRezepteCursor(long katId, long zutId){
        //String suchStr="kategorie = "+String.valueOf(katId);
        String suchStr = "SELECT rezepte.* FROM rezepte";
        if(zutId>=0){
            suchStr = suchStr +" INNER JOIN zutatenListe on rezepte._id = zutatenListe.rezeptID WHERE zutatenListe.zutat = "+zutId;
        }
        if(katId>=0){
            if(zutId>=0){
                suchStr = suchStr +" AND kategorie = "+katId;
            }else{
                suchStr = suchStr +" WHERE kategorie = "+katId;
            }
        }
        if(zutId>=0){
            suchStr = suchStr +" GROUP BY rezepte._id";
        }
        suchStr = suchStr +" ORDER BY rezepte.name";

        return db.rawQuery(suchStr, null);
    }

    public Cursor erzeugeEinRezeptCursor(long id){
        String suchStr = "_id = "+String.valueOf(id);
        return db.query("rezepte", RezepteSpalten, suchStr, null, null, null, "name");
    }


    public static String SQLZutatenListe (long id){
        String sql = "";
        sql =   "select zutatenliste._id, rezeptID, rezeptteil, \n" +
                //"zutatenliste.zutat as z_id, zutatenliste.einheit as e_id, " +
                "case when menge<>1 or einheiten.einh_singular=\"g\" or einheiten.einh_singular=\"kg\" then zutaten.bez_plural else zutaten.bez_singular end as zutat, \n" +
                "menge, \n" +
                "case when menge<>1 then einheiten.einh_plural else einheiten.einh_singular end as einheit\n" +
                "from zutatenListe \n" +
                "left join zutaten on zutat=zutaten._id\n" +
                "left join einheiten on einheit=einheiten._id\n" +
                "where rezeptID="+ String.valueOf(id);
        return sql;
    }

    public Cursor erzeugeZutatenListeCursor(long id, int teil){
        String suchStr = "SELECT "+ ZutatenSpaltenJoin
                +" FROM zutatenListe LEFT JOIN zutaten ON zutatenListe.zutat = zutaten._id LEFT JOIN einheiten ON zutatenListe.einheit = einheiten._id"
                +" WHERE zutatenListe.rezeptID = " + String.valueOf(id);
        if (teil >= 0 ) {
            suchStr = suchStr +" AND rezeptteil = "+String.valueOf(teil);
        }
        return db.rawQuery(suchStr, null);
    }

    public Cursor erzeugeZutatenCursor(){
        return db.rawQuery("SELECT "+ZutatenStammJoin+" FROM zutaten LEFT JOIN einheiten ON einheiten._id = zutaten.einheit_id " +
                "LEFT JOIN zutatenListe on zutaten._id = zutatenliste.zutat GROUP BY zutaten._id ORDER BY zutaten.bez_singular",null );
    }

    public Cursor erzeugeKategorienCursor(){
        return db.query("kategorien", KategorienSpalten, null, null, null, null, null);
    }

}