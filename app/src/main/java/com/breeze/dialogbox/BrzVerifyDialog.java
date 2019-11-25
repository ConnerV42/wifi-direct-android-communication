package com.breeze.dialogbox;

import android.app.*;
import android.os.*;
import java.lang.*;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.breeze.MainActivity;

public class BrzVerifyDialog extends AppCompatDialogFragment
{
    private AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    private MainActivity ma;

    public void setPositiveButton(String text, DialogInterface.OnClickListener onclick){
        builder.setPositiveButton(text, onclick);
    }

    public void setNegativeButton(String text, DialogInterface.OnClickListener onclick){
        builder.setNegativeButton(text, onclick);
    }
    public void setActivity(MainActivity ma)
    {
        this.ma = ma;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState)
    {
        builder.setTitle("Confirm Connection");
        builder.setMessage("Connect to following device?");
        return builder.create();
    }
}

