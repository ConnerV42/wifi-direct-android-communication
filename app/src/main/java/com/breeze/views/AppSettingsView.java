package com.breeze.views;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.breeze.R;
import com.breeze.application.BreezeAPI;

public class AppSettingsView extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView bWipeDB = view.findViewById(R.id.wipe_database);
        TextView bRestartService = view.findViewById(R.id.restart_service);
        TextView bShowBlacklist = view.findViewById(R.id.block_users);
        TextView bAboutMenu = view.findViewById(R.id.aboutButton);

        setColorStateList(bAboutMenu);
        setColorStateList(bRestartService);
        setColorStateList(bShowBlacklist);
        setColorStateList(bWipeDB);

        bWipeDB.setOnClickListener((View v) -> {
            this.showWipeDatabaseDialog();
        });
        bRestartService.setOnClickListener((View v) -> {
            this.showRestartServiceDialog();
        });
        bShowBlacklist.setOnClickListener((View v) -> {
            this.showBlacklistView();
        });
        bAboutMenu.setOnClickListener((View v) -> {
            this.showAboutMenu();
        });

    }

    @Override
    public void onStart() {
        super.onStart();

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return;
        ImageButton ac = activity.findViewById(R.id.scanButton);
        ac.setVisibility(View.INVISIBLE);
        ActionBar ab = activity.getSupportActionBar();
        if (ab == null) return;
        ab.setTitle("Settings");

    }

    private void showWipeDatabaseDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Wipe the Database")
                .setMessage("Are you sure you would like to wipe the database?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        BreezeAPI api = BreezeAPI.getInstance();
                        api.db.onUpgrade(api.db.getWritableDatabase(), 0, 0);
                        Toast.makeText(api.getApplicationContext(), "DB wiped", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void showRestartServiceDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Restart Background Service")
                .setMessage("Are you sure you'd like to restart the background service?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(BreezeAPI.getInstance().getApplicationContext(), "TODO", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
    private void showBlacklistView(){
        NavController nav = Navigation.findNavController(this.getView());
        NavOptions.Builder builder = new NavOptions.Builder();
        NavOptions options = builder.setEnterAnim(R.anim.slide_from_left)
                .setExitAnim(R.anim.slide_out_right)
                .setPopEnterAnim(R.anim.nav_default_pop_enter_anim)
                .setPopExitAnim(R.anim.nav_default_pop_exit_anim)
                .build();
        nav.navigate(R.id.blacklistView,null, options);
    }
    private void showAboutMenu(){
        new AlertDialog.Builder(getActivity())
                .setTitle("About Breeze")
                .setMessage("Breeze v0.1 By: Zach Sanders, Conner  Verret, Pavel Chernichenko, Jake Stevens")
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                })
                .show();
    }
    private void setColorStateList(TextView view) {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_pressed}, // pressed
                new int[] { android.R.attr.state_focused}, // focused
                new int[] { android.R.attr.state_enabled}  // enabled
        };
        int[] colors = new int[] {
                ContextCompat.getColor(this.getContext(), R.color.colorAccent),
                ContextCompat.getColor(this.getContext(), R.color.colorAccent),
                ContextCompat.getColor(this.getContext(), R.color.design_default_color_on_primary),
        };
        ColorStateList list = new ColorStateList(states, colors);
        view.setTextColor(list);
    }


}

