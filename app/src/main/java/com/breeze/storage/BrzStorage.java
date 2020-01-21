package com.breeze.storage;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.ContextCompat;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzFileInfo;
import com.breeze.datatypes.BrzMessage;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.ProfileEvents.BrzProfileResponse;

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

    public void saveProfileImage(BrzPacket p, InputStream stream) {
        BrzProfileResponse response = p.profileResponse();
        BreezeAPI api = BreezeAPI.getInstance();
        File profileImageDirectory = api.getExternalFilesDir(PROFILE_IMAGE_DIR);
        File profileImage = new File(profileImageDirectory, response.from);

        try {
            profileImage.mkdirs();
            if (profileImage.exists()) profileImage.delete();
            profileImage.createNewFile();

            OutputStream out = new FileOutputStream(profileImage);
            byte[] buf = new byte[80000];
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

    public boolean hasProfileImage(String fileName) {
        Bitmap b = getImage(fileName, PROFILE_IMAGE_DIR);
        if (b == null)
            return false;
        return true;
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

    private final String FILE_MESSAGES_DIR = "BreezeMedia";

    public void saveMessageFile(BrzMessage message, InputStream stream) {
        BreezeAPI api = BreezeAPI.getInstance();
        File messagesDir = api.getExternalFilesDir(FILE_MESSAGES_DIR);
        File chatDir = new File(messagesDir, message.chatId);
        File messageFile = new File(chatDir, message.id);

        this.saveMessageFile(messageFile, stream);

    }

    public void saveMessageFile(File messageFile, InputStream stream) {
        try {
            messageFile.mkdirs();
            if (messageFile.exists()) messageFile.delete();
            messageFile.createNewFile();

            OutputStream out = new FileOutputStream(messageFile);
            byte[] buf = new byte[80000];
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


    public boolean hasMessageFile(BrzMessage message) {
        BreezeAPI api = BreezeAPI.getInstance();
        File messagesDir = api.getExternalFilesDir(FILE_MESSAGES_DIR);
        File chatDir = new File(messagesDir, message.chatId);
        File messageFile = new File(chatDir, message.id);
        return messageFile.exists();
    }

    public File getMessageFile(BrzMessage message) {
        if (hasMessageFile(message)) {
            BreezeAPI api = BreezeAPI.getInstance();
            File messagesDir = api.getExternalFilesDir(FILE_MESSAGES_DIR);
            File chatDir = new File(messagesDir, message.chatId);
            return new File(chatDir, message.id);
        }
        return null;
    }

    public Bitmap getMessageFileAsBitmap(BrzMessage message) {
        BreezeAPI api = BreezeAPI.getInstance();
        File messagesDir = api.getExternalFilesDir(FILE_MESSAGES_DIR);
        File chatDir = new File(messagesDir, message.chatId);
        File messageFile = new File(chatDir, message.id);
        return getImage(messageFile);
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

    private Bitmap getImage(File imageFile) {
        try {
            Bitmap image = BitmapFactory.decodeStream(new FileInputStream(imageFile));
            final int maxSize = 750;

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
        } catch (
                FileNotFoundException e) {
            e.printStackTrace();
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
