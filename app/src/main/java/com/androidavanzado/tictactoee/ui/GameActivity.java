package com.androidavanzado.tictactoee.ui;

import android.media.Image;
import android.os.Bundle;

import com.androidavanzado.tictactoee.R;
import com.androidavanzado.tictactoee.app.Constantes;
import com.androidavanzado.tictactoee.model.Jugada;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;

import javax.annotation.Nullable;

public class GameActivity extends AppCompatActivity {

    List<ImageView> casillas;
    TextView tvPlayer1, tvPlayer2;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore db;
    String uid, jugadaId, playerOneName = "", playerTwoName = "";
    Jugada jugada;
    ListenerRegistration listenerJugadas = null;
    FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        initViews();

        initGame();

    }

    private void initGame() {

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        uid = firebaseUser.getUid();

        Bundle extras = getIntent().getExtras();
        jugadaId = extras.getString(Constantes.EXTRA_JUGADA_ID);

    }

    private void initViews() {

        tvPlayer1 = findViewById(R.id.textViewPlayer1);
        tvPlayer2 = findViewById(R.id.textViewPlayer2);

        casillas = new ArrayList<>();
        casillas.add((ImageView) findViewById(R.id.imageView0));
        casillas.add((ImageView) findViewById(R.id.imageView1));
        casillas.add((ImageView) findViewById(R.id.imageView2));
        casillas.add((ImageView) findViewById(R.id.imageView3));
        casillas.add((ImageView) findViewById(R.id.imageView4));
        casillas.add((ImageView) findViewById(R.id.imageView5));
        casillas.add((ImageView) findViewById(R.id.imageView6));
        casillas.add((ImageView) findViewById(R.id.imageView7));
        casillas.add((ImageView) findViewById(R.id.imageView8));

    }

    @Override
    protected void onStart() {
        super.onStart();
        jugadaListener();
    }

    private void jugadaListener() {

        listenerJugadas = db.collection("jugadas")
                .document(jugadaId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(GameActivity.this, "Error al obtener los datos de la jugada", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String source = snapshot != null
                                && snapshot.getMetadata().hasPendingWrites() ? "Local" : "Server";

                        if (snapshot.exists() && source.equals("Server")) {
                            // Parseando DocumentSnapshot > Jugada
                            jugada = snapshot.toObject(Jugada.class);
                            if (playerOneName.isEmpty() || playerTwoName.isEmpty()) {
                                // Obtener los nombres de usuario de la partida
                                getPlayerNames();
                            }
                            
                            updateUI();
                        }

                        cambioColorJugador();

                    }
                });

    }

    private void cambioColorJugador() {

        if (jugada.isTurnoJugadorUno()) {
            tvPlayer1.setTextColor(getResources().getColor(R.color.colorPrimary));
            tvPlayer2.setTextColor(getResources().getColor(R.color.colorGris));
        } else {
            tvPlayer1.setTextColor(getResources().getColor(R.color.colorGris));
            tvPlayer2.setTextColor(getResources().getColor(R.color.colorAccent));
        }

    }

    private void updateUI() {

        for(int i = 0; i < 9; i++){
            int casilla = jugada.getCeldasSeleccionadas().get(i);
            ImageView ivCasillaActual = casillas.get(i);

            if(casilla == 0){
                ivCasillaActual.setImageResource(R.drawable.ic_empty_square);
            }else if(casilla == 1){
                ivCasillaActual.setImageResource(R.drawable.ic_player_one);
            }else{
                ivCasillaActual.setImageResource(R.drawable.ic_player_two);
            }
        }

    }

    private void getPlayerNames() {

        // Obtener el nombre del player 1
        db.collection("users")
                .document(jugada.getJugadorUnoId())
                .get()
                .addOnSuccessListener(GameActivity.this, new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        playerOneName = documentSnapshot.get("name").toString();
                        tvPlayer1.setText(playerOneName);
                    }
                });

        // Obtener el nombre del player 2
        db.collection("users")
                .document(jugada.getJugadorDosId())
                .get()
                .addOnSuccessListener(GameActivity.this, new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        playerTwoName = documentSnapshot.get("name").toString();
                        tvPlayer2.setText(playerTwoName);
                    }
                });

    }

    @Override
    protected void onStop() {
        if (listenerJugadas != null) {
            listenerJugadas.remove();
        }
        super.onStop();
    }

    public void casillaSeleccionada(View view) {

        if (!jugada.getGanadorId().isEmpty()) {
            Toast.makeText(this, "La partida ha terminado", Toast.LENGTH_SHORT).show();
        } else {
            if (jugada.isTurnoJugadorUno() && jugada.getJugadorUnoId().equals(uid)) {
                // Está jugando el jugador 1
                actualizarJugada(view.getTag().toString());
            } else if (!jugada.isTurnoJugadorUno() && jugada.getJugadorDosId().equals(uid)) {
                // Estájugando el jugador 2
                actualizarJugada(view.getTag().toString());
            } else {
                Toast.makeText(this, "Aún no es tu turno", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void actualizarJugada(String numeroCasilla) {

        int posicionCasilla = Integer.parseInt(numeroCasilla);

        if (jugada.getCeldasSeleccionadas().get(posicionCasilla) != 0) {
            Toast.makeText(this, "Seleccione una casilla libre", Toast.LENGTH_SHORT).show();
        } else {
            if (jugada.isTurnoJugadorUno()) {
                casillas.get(posicionCasilla).setImageResource(R.drawable.ic_player_one);
                jugada.getCeldasSeleccionadas().set(posicionCasilla, 1);
            } else {
                casillas.get(posicionCasilla).setImageResource(R.drawable.ic_player_two);
                jugada.getCeldasSeleccionadas().set(posicionCasilla, 2);
            }


            cambioTurno();

            // Actualizar en Firestore los datos de la jugada
            db.collection("jugadas")
                    .document(jugadaId)
                    .set(jugada)
                    .addOnSuccessListener(GameActivity.this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w("ERROR", "Error al guardar la jugada");
                }
            });

        }

    }

    private void cambioTurno() {



        // Cambio de turno
        jugada.setTurnoJugadorUno(!jugada.isTurnoJugadorUno());

    }


}
