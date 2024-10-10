package de.steffen.rezepte;


import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static android.database.Cursor.FIELD_TYPE_BLOB;
import static android.database.Cursor.FIELD_TYPE_FLOAT;
import static android.database.Cursor.FIELD_TYPE_INTEGER;
import static android.database.Cursor.FIELD_TYPE_STRING;


public class XMLExport {

    private Context mContext ;
    private SQLiteDatabase RezepteDB;
    private String EXPORT_PATH = "";
    private DecimalFormat mDecimalFormat;

    // file selection event handling
    public interface ProgressCallback {
        void getProgress(String theAction, int maxValue, int aktValue);
    }
    public XMLExport setProgressCallback(ProgressCallback cProgress) {
        this.cProgress = cProgress;
        return this;
    }
    private ProgressCallback cProgress;

    public XMLExport(Context context, SQLiteDatabase mRezepteDB, String Path) {
        mContext=context;
        this.RezepteDB=mRezepteDB;
        EXPORT_PATH = Path+"/";
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.GERMAN);
        otherSymbols.setDecimalSeparator('.');
        mDecimalFormat = new DecimalFormat("#.###",otherSymbols);
    }

    public void SetPath(String Path) {
        EXPORT_PATH = Path+"/";
    }

    public void ExportRezept(long RId) throws ParserConfigurationException, FileNotFoundException, TransformerException {
        Cursor cRezepte = this.RezepteDB.rawQuery("select * from rezepte where _id = "+RId,null);
        Document XMLDoc = ExpRezTeil(RId, cRezepte);

        // XML Dokument erstellen und ausgeben
        FileOutputStream fos=new FileOutputStream(new File(EXPORT_PATH + cRezepte.getString(DatenBank.tblRezepte.name.getColumn())+".xml"));
        Result fileResult = new StreamResult(fos);
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(XMLDoc),fileResult);
        // Ende von Rezepte.moveToFirst
    }

    public void ExportRezept(long RId, OutputStream fos) throws ParserConfigurationException, FileNotFoundException, TransformerException{
    Cursor cRezepte = this.RezepteDB.rawQuery("select * from rezepte where _id = "+RId,null);
        Document XMLDoc = ExpRezTeil(RId, cRezepte);

        // XML Dokument erstellen und ausgeben
        //FileOutputStream fos=new FileOutputStream(new File(EXPORT_PATH + cRezepte.getString(DatenBank.tblRezepte.name.getColumn())+".xml"));
        Result fileResult = new StreamResult(fos);
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(XMLDoc),fileResult);
        // Ende von Rezepte.moveToFirst
    }

    Document ExpRezTeil(long RId, Cursor cRezepte) throws ParserConfigurationException {
        File path = new File(EXPORT_PATH);
        if (path.exists()==false) { path.mkdirs(); }

        Document XMLDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        // Tags zur Erkennung von "Steffen's Rezepten" und Versionsangabe
        Element header = XMLDoc.createElement("SteffensRezepte");
        XMLDoc.appendChild(header);
        header.setAttribute("XML_Version","1.0");
        header.setAttribute("content","Rezept");

        //Rezeptdaten exportieren

        Element rezept = XMLDoc.createElement("rezept");
        header.appendChild(rezept);


        if (cRezepte.moveToFirst()) {
            // Rezept gefunden - Kopfdaten ausgeben
            String[] columns = cRezepte.getColumnNames();
            for (int i=0; i<columns.length; i++) {
                Element localElement = XMLDoc.createElement((columns[i]));
                if (i==DatenBank.tblRezepte.kategorie.getColumn()) {
                    // Kategorie muss in einen String umgewandelt werden
                    Cursor cKat = this.RezepteDB.rawQuery("SELECT * FROM kategorien where _id = "+cRezepte.getInt(i),null);
                    if (cKat.moveToFirst()) {
                        localElement.appendChild((XMLDoc.createTextNode(cKat.getString(DatenBank.tblKategorien.kategorie.getColumn()))));
                    }
                    else {
                        localElement.appendChild((XMLDoc.createTextNode("")));
                    }
                }
                else {
                    // Wert direkt aus der Datenbank lesen
                    localElement.appendChild((XMLDoc.createTextNode(getValueFromCursor(cRezepte,i))));
                }
                rezept.appendChild(localElement);
            } //ende der for Schleife

            // Jetz die einzelnen Zutaten ausgeben
            Element zutaten =XMLDoc.createElement("zutaten");
            rezept.appendChild(zutaten);
            Cursor cZutaten = this.RezepteDB.rawQuery(DatenBank.SQLZutatenListe(RId),null);
            String[] zColumns = cZutaten.getColumnNames();
            while (cZutaten.moveToNext()){
                Element localItem = XMLDoc.createElement("item");
                zutaten.appendChild(localItem);
                for (int i=0; i<zColumns.length; i++) {
                    Element localElement = XMLDoc.createElement((zColumns[i]));
                    localElement.appendChild(XMLDoc.createTextNode(getValueFromCursor(cZutaten,i)));
                    /*if (zColumns[i].equals("z_id")) {
                        Cursor zDetailCursor = this.RezepteDB.rawQuery("SELECT * FROM zutaten where _id = "+cZutaten.getLong(i), null);
                        if (zDetailCursor.moveToFirst()) {
                            String[] zDetailColumns = zDetailCursor.getColumnNames();
                            for (int zd = 0; zd < zDetailColumns.length; zd++) {
                                Element zDetailElement = XMLDoc.createElement(zDetailColumns[zd]);
                                zDetailElement.appendChild((XMLDoc.createTextNode(getValueFromCursor(zDetailCursor, zd))));
                                localElement.appendChild(zDetailElement);
                            }
                        }
                    }*/
                    localItem.appendChild(localElement);
                }
            }
        }
        return XMLDoc;
    }

    public void ExportDatabase(String name) throws ParserConfigurationException, IOException, TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError {
        // Prüfen ob das Verzeichnis existiert
        File path = new File(EXPORT_PATH);
        if (path.exists() == false) {
            path.mkdirs();
        }

        Document XMLDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        // Tags zur Erkennung von "Steffen's Rezepten" und Versionsangabe
        Element header = XMLDoc.createElement("SteffensRezepte");
        XMLDoc.appendChild(header);
        header.setAttribute("XML_Version","1.0");
        header.setAttribute("content","Datenbank");

        //Rezeptdaten exportieren
        Element root = XMLDoc.createElement("datenbank");
        header.appendChild(root);

        //Alle Tabellen der Datenbank lesen
        Cursor cTabellen = this.RezepteDB.rawQuery("select * from sqlite_master where type='table'",null);
        while (cTabellen.moveToNext()) {
            Element table = XMLDoc.createElement("table");
            root.appendChild(table);
            String tableName = getValueFromCursor(cTabellen,2);
            Element tblName = XMLDoc.createElement("name");
            tblName.appendChild(XMLDoc.createTextNode(tableName));
            table.appendChild(tblName);
            Element sqlStatement = XMLDoc.createElement("sql");
            sqlStatement.appendChild(XMLDoc.createTextNode(getValueFromCursor(cTabellen,4)));
            table.appendChild(sqlStatement);
            Cursor cTable = this.RezepteDB.rawQuery("select * from "+tableName, null);
            String[] columns = cTable.getColumnNames();
            int anzItems = cTable.getColumnCount();
            int aktItem = 0;
            while (cTable.moveToNext()) {
                aktItem++;
                if (cProgress!=null) {
                    cProgress.getProgress(mContext.getString(R.string.xml_export_tabelle,tableName),anzItems,aktItem);
                }
                // Eintrag gefunden - Daten ausgeben
                Element localItem = XMLDoc.createElement("item");
                for (int i = 0; i < columns.length; i++) {
                    Element localElement = XMLDoc.createElement((columns[i]));
                    Element localType = XMLDoc.createElement("type");
                    localType.appendChild(XMLDoc.createTextNode(String.valueOf(cTable.getType(i))));
                    Element localValue = XMLDoc.createElement("value");
                    localValue.appendChild(XMLDoc.createTextNode(getValueFromCursor(cTable,i)));
                    localElement.appendChild(localType);
                    localElement.appendChild(localValue);
                    //localElement.appendChild((XMLDoc.createTextNode(getValueFromCursor(cTable, i))));
                    localItem.appendChild(localElement);
                } //ende der for Schleife
                table.appendChild(localItem);
            }
        }
        // XML Dokument erstellen und ausgeben
        FileOutputStream fos=new FileOutputStream(new File(EXPORT_PATH + name +".xml"));
        Result fileResult = new StreamResult(fos);
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(XMLDoc),fileResult);
    }



    public String getValueFromCursor (Cursor mcursor, int pos) {
        switch (mcursor.getType(pos)) {
            case FIELD_TYPE_INTEGER:
                return String.valueOf(mcursor.getInt(pos));

            case FIELD_TYPE_FLOAT:
                double menge = (mcursor.getFloat(pos));
                return mDecimalFormat.format(menge);

            case FIELD_TYPE_STRING:
                return mcursor.getString(pos);

            case FIELD_TYPE_BLOB:
                byte[] foto = mcursor.getBlob(pos);
                return Base64.encodeToString(foto,0);
        }
        return "";
    }

    public boolean IsSteffensRezept(File file) throws Exception {
        String XML_Version="";
        String content="";
        Document localDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        localDocument.getDocumentElement().normalize();
        Element topNode = (Element) localDocument.getElementsByTagName("SteffensRezepte").item(0);
        content = topNode.getAttribute("content");
        XML_Version = topNode.getAttribute("XML_Version");
        return (content.equals("Rezept") && XML_Version.equals("1.0"));
    }
    public boolean IsSteffensDatenbank(File file) throws Exception {
        String XML_Version="";
        String content="";
        Document localDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        localDocument.getDocumentElement().normalize();
        Element topNode = (Element) localDocument.getElementsByTagName("SteffensRezepte").item(0);
        content = topNode.getAttribute("content");
        XML_Version = topNode.getAttribute("XML_Version");
        return (content.equals("Datenbank") && XML_Version.equals("1.0"));
    }

    public void ImportRezept(InputStream stream) throws Exception {
        Document localDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
        ImportRezeptfromDoc(localDocument);
    }

    public void ImportRezept(File file) throws Exception {
        Document localDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        ImportRezeptfromDoc(localDocument);
    }

    void ImportRezeptfromDoc(Document localDocument) throws Exception {
        String XML_Version="";
        String content="";
        String vorhandeneRezepte="";
        Long katID;
        Long zutID;
        Long einhID;

        localDocument.getDocumentElement().normalize();
        Element topNode = (Element) localDocument.getElementsByTagName("SteffensRezepte").item(0);
        content = topNode.getAttribute("content");
        XML_Version = topNode.getAttribute("XML_Version");
        if (content.equals("Rezept") && XML_Version.equals("1.0")) {
            NodeList rezepte = topNode.getElementsByTagName("rezept");
            for (int r=0; r< rezepte.getLength(); r++) {
                Element rNode = (Element) rezepte.item(r);
                Cursor rCursor = RezepteDB.query("rezepte", DatenBank.RezepteSpalten, "name = '"+rNode.getElementsByTagName("name").item(0).getTextContent()+"'",null,null,null,null);

                //prüfen ob versucht wir eine neue Rezeptversion einzulesen (neue Version > vorhandene Version)
                Boolean rezeptImportieren = false;
                if (rCursor.getCount()>0) {
                    rCursor.moveToFirst();
                    Integer newVersion = 0;
                    try {
                        String version = rNode.getElementsByTagName("version").item(0).getTextContent();
                        newVersion = Integer.valueOf(version);
                    }
                    catch (Exception ex) {
                        // Die Version konnte nicht ermittelt werden => bleibt 0
                    }
                    if (newVersion > rCursor.getInt(DatenBank.tblRezepte.version.getColumn())) {
                        // jetzt das vorhandene Rezept mit Zutaten aus der Datenbank löschen
                        RezepteDB.delete("zutatenliste", "rezeptID = " + rCursor.getInt(DatenBank.tblRezepte._id.getColumn()), null);
                        RezepteDB.delete("rezepte", "_id = " + rCursor.getInt(DatenBank.tblRezepte._id.getColumn()), null);
                        rezeptImportieren = true;
                    }
                } else {
                    rezeptImportieren = true;
                }

                if (rezeptImportieren) {
                    ContentValues values = new ContentValues();
                    values.put("name",rNode.getElementsByTagName("name").item(0).getTextContent());
                    values.put("portionen",Integer.valueOf(rNode.getElementsByTagName("portionen").item(0).getTextContent()));
                    values.put("user",rNode.getElementsByTagName("user").item(0).getTextContent());
                    values.put("version",Integer.valueOf(rNode.getElementsByTagName("version").item(0).getTextContent()));
                    Cursor kCursor = RezepteDB.query("kategorien", DatenBank.KategorienSpalten, "kategorie = '"+rNode.getElementsByTagName("kategorie").item(0).getTextContent()+"'",null,null,null,null);
                    if (kCursor.moveToFirst()) {
                        katID = kCursor.getLong(DatenBank.tblKategorien._id.getColumn());
                    } else {
                        ContentValues newKat = new ContentValues();
                        newKat.put("kategorie", rNode.getElementsByTagName("kategorie").item(0).getTextContent());
                        katID = RezepteDB.insert("kategorien", null, newKat);
                    }
                    values.put("kategorie",katID);
                    if (rNode.getElementsByTagName("teil1").item(0).getFirstChild()!=null) {
                        values.put("teil1",rNode.getElementsByTagName("teil1").item(0).getTextContent());
                    } else {
                        values.put("teil1","");
                    }
                    if (rNode.getElementsByTagName("teil2").item(0).getFirstChild()!=null) {
                        values.put("teil2",rNode.getElementsByTagName("teil2").item(0).getTextContent());
                    } else {
                        values.put("teil2","");
                    }
                    if (rNode.getElementsByTagName("teil3").item(0).getFirstChild()!=null) {
                        values.put("teil3",rNode.getElementsByTagName("teil3").item(0).getTextContent());
                    } else {
                        values.put("teil3","");
                    }
                    if (rNode.getElementsByTagName("teil4").item(0).getFirstChild()!=null) {
                        values.put("teil4",rNode.getElementsByTagName("teil4").item(0).getTextContent());
                    } else {
                        values.put("teil4","");
                    }
                    if (rNode.getElementsByTagName("teil5").item(0).getFirstChild()!=null) {
                        values.put("teil5",rNode.getElementsByTagName("teil5").item(0).getTextContent());
                    } else {
                        values.put("teil5","");
                    }
                    if (rNode.getElementsByTagName("anleitung").item(0).getFirstChild()!=null) {
                        values.put("anleitung",rNode.getElementsByTagName("anleitung").item(0).getTextContent());
                    } else {
                        values.put("anleitung","");
                    }
                    if (rNode.getElementsByTagName("foto").item(0)!=null) {
                        String Base64CodedImage = rNode.getElementsByTagName("foto").item(0).getTextContent();
                        values.put("foto",Base64.decode(Base64CodedImage,0));
                    } else {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        Bitmap newPhoto = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap._unbekannt);
                        newPhoto.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                        byte[] byteArray = stream.toByteArray();
                        values.put("foto", byteArray);
                    }
                    Long newRId = RezepteDB.insert("rezepte", null, values);

                    Element zNode = (Element) rNode.getElementsByTagName("zutaten").item(0);
                    NodeList items = zNode.getElementsByTagName("item");
                    for (int i=0;i<items.getLength();i++) {
                        Element zItem = (Element) items.item(i);
                        Cursor eCursor = RezepteDB.query("einheiten", DatenBank.EinheitenSpalten,
                                "(einh_singular = '" + zItem.getElementsByTagName("einheit").item(0).getTextContent() + "'" +
                                        " or einh_plural = '" + zItem.getElementsByTagName("einheit").item(0).getTextContent()+"')", null, null, null, null);
                        if (eCursor.moveToFirst()) {
                            einhID = eCursor.getLong(DatenBank.tblEinheiten._id.getColumn());
                        } else {
                            ContentValues newEinheit = new ContentValues();
                            newEinheit.put("einh_singular", zItem.getElementsByTagName("einheit").item(0).getTextContent());
                            newEinheit.put("einh_plural", zItem.getElementsByTagName("einheit").item(0).getTextContent());
                            einhID = RezepteDB.insert("zutaten", null, newEinheit);
                        }
                        eCursor.close();
                        Cursor zCursor = RezepteDB.query("zutaten", DatenBank.ZutatenSpalten,
                                "(bez_singular = '" + zItem.getElementsByTagName("zutat").item(0).getTextContent() + "'"+
                                        " or bez_plural = '" + zItem.getElementsByTagName("zutat").item(0).getTextContent() + "')", null, null, null, null);
                        if (zCursor.moveToFirst()) {
                            zutID = zCursor.getLong(DatenBank.tblZutaten._id.getColumn());
                        } else {
                            ContentValues newZutat = new ContentValues();
                            newZutat.put("bez_singular", zItem.getElementsByTagName("zutat").item(0).getTextContent());
                            newZutat.put("bez_plural", zItem.getElementsByTagName("zutat").item(0).getTextContent());
                            newZutat.put("einheit_id", einhID);
                            zutID = RezepteDB.insert("zutaten", null, newZutat);
                        }
                        zCursor.close();
                        ContentValues newItem = new ContentValues();
                        newItem.put("rezeptID", newRId);
                        if(zItem.getElementsByTagName("rezeptteil").getLength() == 0){
                            newItem.put("rezeptteil",0);
                        }else{
                            newItem.put("rezeptteil", Integer.valueOf(zItem.getElementsByTagName("rezeptteil").item(0).getTextContent()));
                        }
                        newItem.put("zutat", zutID);
                        if (zItem.getElementsByTagName("menge").item(0).getTextContent().equals("")) {
                            newItem.put("menge", Double.valueOf(0));
                        } else {
                            newItem.put("menge", Double.valueOf(zItem.getElementsByTagName("menge").item(0).getTextContent()));
                        }
                        newItem.put("einheit", einhID);
                        RezepteDB.insert("zutatenListe", null, newItem);
                    }
                    kCursor.close();
                } else {
                    if (vorhandeneRezepte!=""){vorhandeneRezepte=vorhandeneRezepte+", ";}
                    vorhandeneRezepte=vorhandeneRezepte+rNode.getElementsByTagName("name").item(0).getTextContent();
                    //throw new Exception(mContext.getString(R.string.xml_err_rezept_existiert,rNode.getElementsByTagName("name").item(0).getTextContent()));
                }
                rCursor.close();
            }
        } else {
            throw new Exception(mContext.getText(R.string.xml_err_no_steffen).toString());
        }
        if (vorhandeneRezepte!="") {
            throw new Exception(mContext.getString(R.string.xml_err_rezept_existiert,vorhandeneRezepte));
        }
    }


    public void ImportDatenbank(File file) throws Exception {
        String XML_Version="";
        String content="";

        Document localDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        localDocument.getDocumentElement().normalize();
        Element topNode = (Element) localDocument.getElementsByTagName("SteffensRezepte").item(0);
        content = topNode.getAttribute("content");
        XML_Version = topNode.getAttribute("XML_Version");
        if (content.equals("Datenbank") && XML_Version.equals("1.0")) {
            NodeList DatenbankNodes = topNode.getElementsByTagName("table");
            for (int i = 0; i<DatenbankNodes.getLength() ; i++) {
                Node TableNode = DatenbankNodes.item(i);
                if (TableNode.getNodeType() == 1) {
                    Element localElement = (Element) TableNode;
                    RezepteDB.execSQL("DELETE FROM "+((Element)TableNode).getElementsByTagName("name").item(0).getTextContent());
                    NodeList nl = localElement.getElementsByTagName("item");
                    for (int a = 0; a < nl.getLength(); a++) {
                        if (cProgress!=null) {
                            cProgress.getProgress(mContext.getString(R.string.xml_import_tabelle,((Element)TableNode).getElementsByTagName("name").item(0).getTextContent()),nl.getLength(),a);
                        }
                        String spalte;
                        String wert;
                        int type;
                        Node el = nl.item(a);
                        NodeList spalten = el.getChildNodes();
                        ContentValues newItem = new ContentValues();
                        for (int s = 0; s < spalten.getLength(); s++) {
                            Node node = spalten.item(s);
                            spalte=node.getNodeName();
                            wert=((Node) ((Element) node).getElementsByTagName("value").item(0)).getTextContent();
                            type=Integer.valueOf(((Node) ((Element) node).getElementsByTagName("type").item(0)).getTextContent());

                            switch (type) {
                                case FIELD_TYPE_INTEGER:
                                    newItem.put(spalte, Integer.valueOf(wert));
                                    break;
                                case FIELD_TYPE_FLOAT:
                                    newItem.put(spalte, Double.valueOf(wert));
                                    break;
                                case FIELD_TYPE_BLOB:
                                    newItem.put(spalte,Base64.decode(wert,0));
                                    break;
                                case FIELD_TYPE_STRING:
                                    newItem.put(spalte,wert);
                            }
                        }
                        RezepteDB.insert(((Element)TableNode).getElementsByTagName("name").item(0).getFirstChild().getNodeValue(), null, newItem);
                    }
                }
            }
        } else {
            throw new Exception(mContext.getText(R.string.xml_err_no_database).toString());
        }
    }

}
