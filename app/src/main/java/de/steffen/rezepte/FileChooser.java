package de.steffen.rezepte;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileChooser {

    class FileChooserAdapter extends ArrayAdapter<Files> {

        Context context;

        public FileChooserAdapter(Context context, int resourceId, List<Files> items) {
            super(context, resourceId, items);
            this.context = context;
        }

        private class ViewHolder {
            ImageView img;
            TextView name;
            TextView size;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            Files item = getItem(position);
            View v;

            if (convertView == null) {
                LayoutInflater mInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
                v = mInflater.inflate(R.layout.file_chooser_datensatz, parent, false);
                holder = new ViewHolder();
                holder.name = (TextView) v.findViewById(R.id.name);
                holder.size = (TextView) v.findViewById(R.id.name);
                holder.img = (ImageView) v.findViewById(R.id.img);
                v.setTag(holder);
            } else {
                v=convertView;
                holder = (ViewHolder) v.getTag();
            }

            holder.name.setText(item.name);
            //holder.size.setText(item.size);
            if (item.thumb==null) {
                holder.img.setImageResource(item.resID);
            } else {
                holder.img.setImageBitmap(item.thumb);
            }
            return v;
        }
    }

    class Files {
        String name;
        String size;
        int resID;
        Bitmap thumb;
    }

    private static final String PARENT_DIR = "..";

    private final Context context;
    private ListView list;
    private Dialog dialog;
    private TextView pathName;
    private File currentPath;

    // filter on file extension
    private String extension = null;

    public void setExtension(String extension) {
        this.extension = (extension == null) ? null : extension.toLowerCase();
        refresh(currentPath);
    }

    // file selection event handling
    public interface FileSelectedListener {
        void fileSelected(Context con, File file);
    }
    public FileChooser setFileListener(FileSelectedListener fileListener) {
        this.fileListener = fileListener;
        return this;
    }
    private FileSelectedListener fileListener;

    public FileChooser(final Context context, String extension) {
        this.extension = (extension == null) ? null : extension.toLowerCase();
        this.context = context;
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.file_chooser_dialog);
        list = (ListView) dialog.findViewById(R.id.file_list);
        pathName = (TextView) dialog.findViewById(R.id.path_name);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int which, long id) {
                Files fileChosen = (Files) list.getItemAtPosition(which);
                File chosenFile = getChosenFile(fileChosen.name);
                if (chosenFile.isDirectory()) {
                    refresh(chosenFile);
                } else {
                    dialog.dismiss();
                    if (fileListener != null) {
                        fileListener.fileSelected(context, chosenFile);
                    }
                }
            }
        });
        //dialog.setContentView(list);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        refresh(context.getExternalFilesDir(""));
    }

    public void showDialog() {
        //dialog.setCancelable(false);
        dialog.show();
    }


    /**
     * Sort, filter and display the files for the given path.
     */
    private void refresh(File path) {
        this.currentPath = path;
        pathName.setText(path.toString());
        if (path.exists()) {
            File[] dirs = path.listFiles(new FileFilter() {
                @Override public boolean accept(File file) {
                    return (file.isDirectory() && file.canRead());
                }
            });
            File[] files = path.listFiles(new FileFilter() {
                @Override public boolean accept(File file) {
                    if (!file.isDirectory()) {
                        if (!file.canRead()) {
                            return false;
                        } else if (extension == null) {
                            return true;
                        } else {
                            return file.getName().toLowerCase().endsWith(extension);
                        }
                    } else {
                        return false;
                    }
                }
            });

            // convert to an array
            int i = 0;
            List<Files> fileList = new ArrayList<Files>();
            /* Files fileSpec = new Files();
            int dirLength = 0;
            if (dirs!=null) dirLength = dirs.length;
            int filesLength = 0;
            if (files!=null) filesLength = files.length;*/

            if (path.getParentFile() != null) {
                Files fileSpec = new Files();
                fileSpec.name=PARENT_DIR;
                fileSpec.size="";
                fileSpec.resID=R.drawable.ic_svg_up;
                fileSpec.thumb=null;
                fileList.add(fileSpec);
            }
            if (dirs!=null) {
                Arrays.sort(dirs);
                for (File dir : dirs) {
                    Files fileSpec = new Files();
                    fileSpec.name = dir.getName();
                    fileSpec.size="";
                    fileSpec.resID=R.drawable.ic_svg_ordner;
                    fileSpec.thumb=null;
                    fileList.add(fileSpec);
                }
            }
            XMLExport exporter = new XMLExport(context,null,path.getName());
            if (files!=null) {
                Arrays.sort(files);
                for (File file : files) {
                    Files fileSpec = new Files();
                    fileSpec.name = file.getName();
                    fileSpec.size = String.valueOf(file.length());
                    try {
                        if (exporter.IsSteffensRezept(file)) {
                            fileSpec.resID = R.drawable.ic_svg_rezept;
                        } else {
                            if (exporter.IsSteffensDatenbank(file)) {
                                fileSpec.resID = R.drawable.ic_svg_database;
                            } else fileSpec.resID = android.R.drawable.presence_busy;
                        }
                    } catch (Exception e) {

                    }
                    fileSpec.thumb = null;
                    fileList.add(fileSpec);
                }
            }

            // refresh the user interface
            dialog.setTitle(currentPath.getPath());
            list.setAdapter(new FileChooserAdapter(context, R.layout.file_chooser_datensatz, fileList));
        }
    }


    /**
     * Convert a relative filename into an actual File object.
     */
    private File getChosenFile(String fileChosen) {
        if (fileChosen.equals(PARENT_DIR)) {
            return currentPath.getParentFile();
        } else {
            return new File(currentPath, fileChosen);
        }
    }
}