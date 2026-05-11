package com.example.foottest;

import static android.content.Intent.getIntent;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import android.net.Uri;

public class ImageViewActivity extends AppCompatActivity {

    private ImageView imageViewAgrandie;
    private Button btnRetour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        imageViewAgrandie = findViewById(R.id.imageViewAgrandie);
        btnRetour = findViewById(R.id.btnRetour);

        // Récupérer l'URI de l'image passée en paramètre
        String imageUriString = getIntent().getStringExtra("image_uri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            imageViewAgrandie.setImageURI(imageUri);
        }

        // Bouton pour retourner à l'écran précédent
        btnRetour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Optionnel: permettre de fermer en cliquant sur l'image
        imageViewAgrandie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}