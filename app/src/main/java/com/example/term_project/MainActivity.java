package com.example.term_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private RadioGroup modeSelector;
    private Button startGameButton, logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        modeSelector = findViewById(R.id.mode_selector);
        startGameButton = findViewById(R.id.button_start_game);
        logoutButton = findViewById(R.id.button_logout);

        startGameButton.setOnClickListener(v -> {
            int selectedId = modeSelector.getCheckedRadioButtonId();
            String mode = "single"; // default fallback

            if (selectedId == R.id.radio_1v1) mode = "1v1";

            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("mode", mode);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, Login.class));
            finish();
        });
    }
}
