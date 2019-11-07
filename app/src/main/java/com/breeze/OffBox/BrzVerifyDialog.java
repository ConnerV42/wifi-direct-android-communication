package com.breeze.OffBox;


import android.os.Bundle;
import android.app.*;
import java.lang.*;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

public class BrzVerifyDialog extends AppCompatDialogFragment
{
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Confirm Connection");
        builder.setMessage("Connect to following device?");
        builder.setPositiveButton("Connect", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int id)
            {
                //Connection approval
            }
        });
        builder.setNegativeButton("Ignore", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int id)
            {
                //User cancelled the dialog
            }
        });

        return builder.create();
    }
}

