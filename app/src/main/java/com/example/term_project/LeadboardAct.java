package com.example.term_project;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeadboardAct extends AppCompatActivity {
    private ListView leaderboardList;
    private LeaderboardAdapter adapter;
    private List<LeaderboardEntry> leaderboardEntries;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_leaderboard);

        leaderboardList = findViewById(R.id.leaderboard_list);
        leaderboardEntries = new ArrayList<>();
        adapter = new LeaderboardAdapter(this, leaderboardEntries);
        leaderboardList.setAdapter(adapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("user_data");
        loadLeaderboardData();
    }
    // TODO add button to go back to main act page
    private void loadLeaderboardData() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                leaderboardEntries.clear();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    int correct = 0;
                    DataSnapshot answers = userSnapshot.child("answers");
                    for (DataSnapshot answer : answers.getChildren()) {
                        Boolean isCorrect = answer.child("correct").getValue(Boolean.class);
                        if (isCorrect != null && isCorrect) {
                            correct++;
                        }
                    }
                    String username = userSnapshot.getKey();
                    leaderboardEntries.add(new LeaderboardEntry(username, correct));
                }

                Collections.sort(leaderboardEntries, (a, b) -> Integer.compare(b.score, a.score));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("LEADERBOARD", "Firebase read failed", error.toException());
            }
        });
    }

    static class LeaderboardEntry {
        String username;
        int score;

        LeaderboardEntry(String username, int score) {
            this.username = username;
            this.score = score;
        }
    }

    static class LeaderboardAdapter extends ArrayAdapter<LeaderboardEntry> {
        private final Context context;
        private final List<LeaderboardEntry> entries;

        public LeaderboardAdapter(Context context, List<LeaderboardEntry> entries) {
            super(context, android.R.layout.simple_list_item_2, entries);
            this.context = context;
            this.entries = entries;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // TODO make leader board look better
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            LeaderboardEntry entry = entries.get(position);
            text1.setText((position + 1) + ". " + entry.username);
            text2.setText("Score: " + entry.score);

            if (position == 0) convertView.setBackgroundColor(0xFFFFD700); // Gold
            else if (position == 1) convertView.setBackgroundColor(0xFFC0C0C0); // Silver
            else if (position == 2) convertView.setBackgroundColor(0xFFCD7F32); // Bronze
            else convertView.setBackgroundColor(0x00000000);

            return convertView;
        }
    }
}
