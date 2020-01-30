package com.breeze.storage;

import android.content.Context;
import android.content.ContextWrapper;
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
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.ProfileEvents.BrzProfileImageEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BrzStorage extends EventEmitter {
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

    public void saveProfileImageFile(BrzPacket p, InputStream stream) {
        BrzProfileImageEvent event = p.profileImageEvent();
        BreezeAPI api = BreezeAPI.getInstance();

        File profileImageDirectory = api.getExternalFilesDir(PROFILE_IMAGE_DIR);
        File profileImage = new File(profileImageDirectory, event.nodeId);

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

            emit("profileImage", event.nodeId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveProfileImage(Bitmap bm, String fileName) {
        this.saveImage(bm, fileName, cw.getExternalFilesDir(PROFILE_IMAGE_DIR));
    }

    public void deleteProfileImage(String fileName) {
        this.deleteImage(fileName, PROFILE_IMAGE_DIR);
    }

    public Bitmap getProfileImage(String fileName, Context ctx) {
        Bitmap b = getImage(fileName, cw.getExternalFilesDir(PROFILE_IMAGE_DIR));
        if (b == null)
            b = bitmapFromVector(ctx, R.drawable.ic_person_black_24dp);
        return b;
    }

    public boolean hasProfileImage(String fileName) {
        Bitmap b = getImage(fileName, cw.getExternalFilesDir(PROFILE_IMAGE_DIR));
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
        this.saveImage(bm, fileName, cw.getExternalFilesDir(CHAT_IMAGE_DIR));
    }

    public void deleteChatImage(String fileName) {
        this.deleteImage(fileName, CHAT_IMAGE_DIR);
    }

    public Bitmap getChatImage(String fileName, Context ctx) {
        Bitmap b = getImage(fileName, cw.getExternalFilesDir(CHAT_IMAGE_DIR));
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
        } catch (Exception e) {
            Log.e("STORAGE", "Failed to create message file", e);
        }

        Thread saverThread = new Thread(() -> {
            try {
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

            }
        });
        saverThread.start();
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

    private void saveImage(Bitmap bm, String name, File imageDir) {
        File imagePath = new File(imageDir, name);
        try {
            imageDir.mkdir();
            imagePath.createNewFile();
            FileOutputStream stream = new FileOutputStream(imagePath);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();
        } catch (Exception e) {
            Log.i("STORAGE", "Profile image failed to save", e);
        }
    }

    private Bitmap getImage(String name, File imageDir) {
        try {
            File image = new File(imageDir, name);
            InputStream imageInputStream = new FileInputStream(image);
            Bitmap b = BitmapFactory.decodeStream(imageInputStream);
            imageInputStream.close();
            return b;
        } catch (Exception e) {
            Log.i("STORAGE", "Failed to retrieve a profile image for " + name);
        }

        return null;
    }

    private Bitmap getImage(File imageFile) {
        try {
            InputStream imageInputStream = new FileInputStream(imageFile);
            Bitmap image = BitmapFactory.decodeStream(imageInputStream);
            imageInputStream.close();

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
        } catch (Exception e) {
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

    public static InputStream bitmapToInputStream(Bitmap bm, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, quality, baos);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        return is;
    }
}
