package com.breeze.storage;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Environment;

import androidx.core.content.ContextCompat;

import com.breeze.R;
import com.breeze.datatypes.BrzMessage;
import com.breeze.packets.BrzPacket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BrzStorage {
    private ContextWrapper cw;

    private BrzStorage(Context c) {
        this.cw = new ContextWrapper(c);
    }

    // Singleton stuff
    private static BrzStorage instance = null;

    public static BrzStorage getInstance() {
        return instance;
    }

    public static BrzStorage initialize(Context c) {
        if (instance == null) instance = new BrzStorage(c);
        return instance;
    }

    /*
     *
     *      Profile image handling
     *
     */

    private final String PROFILE_IMAGE_DIR = "profile_images";

    public void saveProfileImage(Bitmap bm, String fileName) {
        this.saveImage(bm, fileName, PROFILE_IMAGE_DIR);
    }

    public void deleteProfileImage(String fileName) {
        this.deleteImage(fileName, PROFILE_IMAGE_DIR);
    }

    public Bitmap getProfileImage(String fileName, Context ctx) {
        Bitmap b = getImage(fileName, PROFILE_IMAGE_DIR);
        if (b == null)
            b = bitmapFromVector(ctx, R.drawable.ic_person_black_24dp);
        return b;
    }

    /*
     *
     *      Chat image handling
     *
     */

    private final String CHAT_IMAGE_DIR = "chat_images";

    public void saveChatImage(Bitmap bm, String fileName) {
        this.saveImage(bm, fileName, CHAT_IMAGE_DIR);
    }

    public void deleteChatImage(String fileName) {
        this.deleteImage(fileName, CHAT_IMAGE_DIR);
    }

    public Bitmap getChatImage(String fileName, Context ctx) {
        Bitmap b = getImage(fileName, CHAT_IMAGE_DIR);
        if (b == null)
            b = bitmapFromVector(ctx, R.drawable.ic_person_black_24dp);
        return b;
    }

    /*

        Stream message test

     */

    public void saveFileMessage(BrzPacket packet, InputStream stream) {
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File messageFile = new File(downloads.getPath(), packet.stream.fileName);

        try {
            OutputStream out = new FileOutputStream(messageFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = stream.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }














    /*
     *
     *      File access helpers
     *
     */

    private void saveImage(Bitmap bm, String name, String dir) {
        File imageDirectory = cw.getDir(dir, Context.MODE_PRIVATE);
        File imagePath = new File(imageDirectory, name);

        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(imagePath);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException e) {
                //  e.printStackTrace();
            }
        }
    }

    private Bitmap getImage(String name, String dir) {
        File imageDirectory = cw.getDir(dir, Context.MODE_PRIVATE);

        try {
            File image = new File(imageDirectory, name);
            return BitmapFactory.decodeStream(new FileInputStream(image));
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
        }

        return null;
    }

    private void deleteImage(String name, String dir) {
        File imageDirectory = cw.getDir(dir, Context.MODE_PRIVATE);
        File imagePath = new File(imageDirectory, name);
        imagePath.delete();
    }

    public Bitmap bitmapFromVector(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        Bitmap bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

}
