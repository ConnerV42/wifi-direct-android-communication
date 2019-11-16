package com.breeze.storage;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BrzStorage {
    // Singleton stuff
    private static BrzStorage instance = null;

    public static BrzStorage getInstance(Context c) {
        if (instance == null) instance = new BrzStorage(c);
        return instance;
    }

    public static BrzStorage getInstance() {
        return instance;
    }


    private Context context;

    private BrzStorage(Context c) {
        this.context = c;
    }

    // Profile image handling

    private final String PROFILE_IMAGE_DIR = "profile_images";
    public void saveProfileImage(Bitmap bm, String fileName) {
        this.saveImage(bm, fileName, PROFILE_IMAGE_DIR);
    }

    public Bitmap getProfileImage(String fileName) {
        return this.getImage(fileName, PROFILE_IMAGE_DIR);
    }


    // File access helpers

    private void saveImage(Bitmap bm, String name, String dir) {
        ContextWrapper cw = new ContextWrapper(context);

        File imageDirectory = cw.getDir(dir, Context.MODE_PRIVATE);
        File imagePath = new File(imageDirectory, name);

        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(imagePath);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap getImage(String name, String dir) {
        ContextWrapper cw = new ContextWrapper(context);
        File imageDirectory = cw.getDir(dir, Context.MODE_PRIVATE);

        try {
            File image = new File(imageDirectory, name);
            return BitmapFactory.decodeStream(new FileInputStream(image));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

}
