package com.breeze.storage;

import android.os.AsyncTask;
import android.util.Log;

import com.breeze.application.BreezeAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class BrzStorageStreamWorker extends AsyncTask<String, Integer, String> {

    private String fileKey;
    private File outputFile;
    private InputStream data;

    public BrzStorageStreamWorker(String fileKey, File outputFile, InputStream data) {
        this.fileKey = fileKey;
        this.outputFile = outputFile;
        this.data = data;
    }

    @Override
    protected String doInBackground(String[] arguments) {
        try {
            OutputStream out = new FileOutputStream(outputFile);
            byte[] buf = new byte[1000];
            int len;
            int size = 0;
            while ((len = data.read(buf)) > 0) {
                out.write(buf, 0, len);
                size += len;
                if (isCancelled()) break;
            }
            out.flush();
            out.close();
            data.close();
        } catch (Exception e) {
            Log.e("STORAGE", "Failed to output stream to the file " + outputFile.getAbsolutePath(), e);
        }

        return fileKey;
    }

    @Override
    protected void onPostExecute(String s) {
        BreezeAPI api = BreezeAPI.getInstance();
        api.storage.downloadingFiles.remove(s);
        api.storage.emit("downloadDone", s);
    }
}

