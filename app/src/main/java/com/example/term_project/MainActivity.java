package com.example.term_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private Button startGameButton, leaderboardButton, logoutButton;
    private TextView welcomeTextView;
    private String uId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        startGameButton = findViewById(R.id.start_game_button);
        leaderboardButton = findViewById(R.id.leaderboard_button);
        logoutButton = findViewById(R.id.return_button);
        welcomeTextView = findViewById(R.id.swipe_area);

        welcomeTextView.setText("Welcome to PvP Quiz!");

        startGameButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
        });

        leaderboardButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, LeadboardAct.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
            finish();
        });
    }
}
