package com.vnd.superbible7;

import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Button btnHome = findViewById(R.id.btnHome);
        Button btnAbout = findViewById(R.id.btnAbout);

        btnHome.setOnClickListener(view -> {
            finish();
        });

        btnAbout.setOnClickListener(view -> {
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setIcon(R.mipmap.ic_launcher);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        TextView aboutText = findViewById(R.id.aboutText);

        String aboutHtml = "This app demonstrates all programs from the <b>7th edition</b> of the <b>OpenGL SuperBible: Comprehensive Tutorial and Reference</b>." +
                "This is the original repository for the example source code:- https://github.com/openglsuperbible/sb7code.<br/><br/>" +
                "🖥️ Technologies Used:<br/>" +
                "Programming Language: <b>Java</b><br/>" +
                "Rendering API: <b>OpenGL ES GLSL ES 3.20</b><br/>" +
                "Operating System: <b>Android</b><br/>" +
                "User Interface & Windowing: <b>Android SDK</b><br/><br/>" +
                "👩‍💻 Programming by <b>Vaishali Dudhmal</b>.";

        Spanned formattedText = Html.fromHtml(aboutHtml, Html.FROM_HTML_MODE_COMPACT);
        aboutText.setText(formattedText);
        aboutText.setMovementMethod(LinkMovementMethod.getInstance());
        aboutText.setLinkTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_light));
    }
}
