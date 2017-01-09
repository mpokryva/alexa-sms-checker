package com.example.miki.alexasmschecker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SetUpActivity extends AppCompatActivity {

    private EditText pinEditText;
    private Button confirmButton;
    // Write a message to the database
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("message");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        pinEditText = (EditText) findViewById(R.id.edittext_pin);
        confirmButton = (Button) findViewById(R.id.confirm_button);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String editText = pinEditText.getText().toString();
                myRef.setValue(editText);
            }
        });
    }
}
