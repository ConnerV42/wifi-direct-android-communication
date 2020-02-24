package com.breeze.storage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.breeze.EventEmitter;
import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class BrzStorage extends EventEmitter {
    // Singleton stuff
    private static BrzStorage instance = null;

    public static BrzStorage getInstance() {
        return instance;
    }

    public static BrzStorage initialize(BreezeAPI api) {
        if (instance == null) instance = new BrzStorage(api);
        return instance;
    }

    // Constructor

    private BreezeAPI api;

    private BrzStorage(BreezeAPI api) {
        this.api = api;
    }

    // File streaming helpers

    HashMap<String, Boolean> downloadingFiles = new HashMap<>();

    public boolean isDownloading(String fileKey) {
        return downloadingFiles.get(fileKey) != null;
    }

    public void saveStreamToFile(String fileKey, File outputFile, InputStream data) {
        try {
            outputFile.mkdirs();
            if (outputFile.exists()) outputFile.delete();
            outputFile.createNewFile();
        } catch (Exception e) {
            Log.e("STORAGE", "Failed to create file " + outputFile.getAbsolutePath(), e);
        }

        downloadingFiles.put(fileKey, true);
        new BrzStorageStreamWorker(fileKey, outputFile, data).execute();
    }

    public void saveStreamToFileSync(File outputFile, InputStream data) {
        try {
            outputFile.mkdirs();
            if (outputFile.exists()) outputFile.delete();
            outputFile.createNewFile();
        } catch (Exception e) {
            Log.e("STORAGE", "Failed to create file " + outputFile.getAbsolutePath(), e);
        }

        try {
            OutputStream out = new FileOutputStream(outputFile);
            byte[] buf = new byte[5000];
            int len;
            while ((len = data.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            data.close();
        } catch (Exception e) {
            Log.e("STORAGE", "Failed to output stream to the file " + outputFile.getAbsolutePath(), e);
        }
    }

    public void saveBitmapToFile(File outputFile, Bitmap bm) {
        try {
            outputFile.mkdirs();
            if (outputFile.exists()) outputFile.delete();
            outputFile.createNewFile();
        } catch (Exception e) {
            Log.e("STORAGE", "Failed to create file " + outputFile.getAbsolutePath(), e);
        }

        try {
            FileOutputStream stream = new FileOutputStream(outputFile);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();
        } catch (Exception e) {
            Log.i("STORAGE", "Failed to save bitmap to file " + outputFile.getAbsolutePath(), e);
        }
    }

    public Bitmap getFileAsBitmap(File imageFile) {
        try {
            InputStream imageInputStream = new FileInputStream(imageFile);
            Bitmap b = BitmapFactory.decodeStream(imageInputStream);
            imageInputStream.close();
            return b;
        } catch (Exception e) {
            Log.i("STORAGE", "Failed to retrieve image file " + imageFile.getAbsolutePath(), e);
        }

        return null;
    }

    public Bitmap getVectorAsBitmap(int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(api, drawableId);
        Bitmap bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public void deleteFile(File outputFile) {
        try {
            outputFile.delete();
        } catch (Exception e) {
            Log.e("STORAGE", "Failed to delete file " + outputFile.getAbsolutePath(), e);
        }
    }

    /*
     *
     *      Profile image handling
     *
     */

    public final String PROFILE_DIR = "profile_images";
    public final String CHAT_DIR = "chat_images";

    public void saveProfileImage(String directory, String fileName, InputStream stream) {
        File profileImageFile = new File(api.getExternalFilesDir(directory), fileName);
        saveStreamToFile(fileName, profileImageFile, stream);
    }

    public File getHostNodeImageAsFile() {
        File profileImageFile = new File(api.getExternalFilesDir(PROFILE_DIR), this.api.hostNode.id);
        return profileImageFile;
    }
    public void saveProfileImage(String directory, String fileName, Bitmap bm) {
        File profileImageFile = new File(api.getExternalFilesDir(directory), fileName);
        saveBitmapToFile(profileImageFile, bm);
    }

    public void deleteProfileImage(String directory, String fileName) {
        File profileImageFile = new File(api.getExternalFilesDir(directory), fileName);
        deleteFile(profileImageFile);
    }

    public Bitmap getProfileImage(String directory, String fileName) {
        File profileImageFile = new File(api.getExternalFilesDir(directory), fileName);
        Bitmap b = getFileAsBitmap(profileImageFile);
        if (b == null) return getVectorAsBitmap(R.drawable.ic_person_black_24dp);
        return b;
    }

    public boolean hasProfileImage(String directory, String fileName) {
        File profileImageFile = new File(api.getExternalFilesDir(directory), fileName);
        return profileImageFile.exists();
    }

    /*
     *
     *      Message file handling
     *
     */

    public final String FILE_MESSAGES_DIR = "BreezeMedia";

    public void saveMessageFile(BrzMessage message, InputStream stream) {
        File messagesDir = api.getExternalFilesDir(FILE_MESSAGES_DIR);
        File chatDir = new File(messagesDir, message.chatId);
        File messageFile = new File(chatDir, message.id);

        saveStreamToFile(message.id, messageFile, stream);
    }

    public void saveMessageFileSync(BrzMessage message, InputStream stream) {
        File messagesDir = api.getExternalFilesDir(FILE_MESSAGES_DIR);
        File chatDir = new File(messagesDir, message.chatId);
        File messageFile = new File(chatDir, message.id);

        saveStreamToFileSync(messageFile, stream);
    }

    public boolean messageFileIsDownloading(BrzMessage message) {
        return isDownloading(message.id);
    }

    public boolean messageFileExists(BrzMessage message) {
        File messagesDir = api.getExternalFilesDir(FILE_MESSAGES_DIR);
        File chatDir = new File(messagesDir, message.chatId);
        File messageFile = new File(chatDir, message.id);
        return messageFile.exists();
    }

    public File getMessageFile(BrzMessage message) {
        if (messageFileExists(message)) {
            File messagesDir = api.getExternalFilesDir(FILE_MESSAGES_DIR);
            File chatDir = new File(messagesDir, message.chatId);
            return new File(chatDir, message.id);
        }
        return null;
    }

    public Bitmap getMessageFileAsBitmap(BrzMessage message) {
        File messagesDir = api.getExternalFilesDir(FILE_MESSAGES_DIR);
        File chatDir = new File(messagesDir, message.chatId);
        File messageFile = new File(chatDir, message.id);
        Bitmap bm = getFileAsBitmap(messageFile);
        if (bm == null) return null;
        return scaleBitmap(bm, 750);
    }

    /*
     *
     *      Bitmap helpers
     *
     */

    private Bitmap scaleBitmap(Bitmap image, final int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 0) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public static InputStream bitmapToInputStream(Bitmap bm, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, quality, baos);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        return is;
    }
}
