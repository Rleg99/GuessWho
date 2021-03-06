package com.rnagames.guesswho;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.InternalHelpers;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rnagames.guesswho.Adapter.PartidaAdapter;
import com.rnagames.guesswho.Pojos.PojoJuego;
import com.rnagames.guesswho.Pojos.PojoPartida;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.google.firebase.firestore.FieldValue.delete;


public class activity_lobby extends AppCompatActivity {

    private static final String KEY_JUGADORS = "jugadorS";

    public FirebaseFirestore db = FirebaseFirestore.getInstance();
    public CollectionReference cola = db.collection("Cola");
    public PartidaAdapter partidaAdapter;
    String getNivelURL = "https://guess-who-223421.appspot.com/getNivel.php";
    String gamertag;
    String TAG = "MENSAJE!";
    String GamerTagUnirse = "";
    String IdJuegoCreado = "";
    TextView tvGamertag, tvNivel;
    RecyclerView rvCola;
    Button bCrear;
    Button bUnirse;

    public int res;
    public int VistaTablero = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        tvGamertag = findViewById(R.id.tvGamertag);
        tvNivel = findViewById(R.id.tvNivel);
        rvCola = findViewById(R.id.rvListaPartidas);
        bCrear = findViewById(R.id.bCrearPartida);
        bUnirse = findViewById(R.id.bUnirse);

        Bundle recibirUsuario = getIntent().getExtras();
        gamertag = recibirUsuario.getString("gamertag");
        tvGamertag.setText(gamertag);
        getNivel();

        generarRecyclerView();

        partidaAdapter.setOnClickListener(new PartidaAdapter.OnClickListener() {
            @Override
            public void onItemClick(final DocumentSnapshot documentSnapshot, int Posicion) {
                db.collection("Cola").get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                PojoPartida Cola = documentSnapshot.toObject(PojoPartida.class);
                                GamerTagUnirse = Cola.getNombre();
                                VistaTablero = Cola.getVistaTablero();
                                IdJuegoCreado = Cola.getIdJuego();

                                db.collection("Juego").document(IdJuegoCreado)
                                        .update(KEY_JUGADORS, gamertag)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(activity_lobby.this,
                                                        "Se ha unido a la partida de "
                                                                + GamerTagUnirse, Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                Intent i = new Intent(activity_lobby.this, activity_juego.class);
                                i.putExtra("IdJuego", "" + IdJuegoCreado);
                                i.putExtra("numTablero", VistaTablero);
                                i.putExtra("tipoJugador", false);
                                i.putExtra("gamertag", gamertag);

                                startActivity(i);
                            }
                        });
                db.collection("Cola").document(documentSnapshot.getId()).delete();
            }
        });

    }

    public void generarRecyclerView() {
        Query query = cola.orderBy("nombre", Query.Direction.ASCENDING);
        FirestoreRecyclerOptions<PojoPartida> opciones = new FirestoreRecyclerOptions.Builder<PojoPartida>()
                .setQuery(query, PojoPartida.class)
                .build();

        partidaAdapter = new PartidaAdapter(opciones);
        rvCola.setHasFixedSize(true);
        rvCola.setLayoutManager(new LinearLayoutManager(this));
        rvCola.setAdapter(partidaAdapter);


    }

    @Override
    protected void onStart() {
        super.onStart();
        partidaAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        partidaAdapter.stopListening();

        //db.collection("cities").document(documentReference.getId()).delete();

    }

    public void getNivel() {

        StringRequest postRequest = new StringRequest(Request.Method.POST, getNivelURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        tvNivel.setText(tvNivel.getText().toString() + response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Toast.makeText(activity_lobby.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("gamertag", gamertag);


                return params;
            }
        };
        RequestQueue pide = Volley.newRequestQueue(this);

        pide.add(postRequest);
    }

    public void clickCrearPartida(View view) {
        Random rand = new Random();
        VistaTablero = rand.nextInt(5 - 1) + 1;
        PojoJuego Juego = new PojoJuego(gamertag, "", "Pregunta",
                "00", false, true,true,"","");

        db.collection("Juego").add(Juego)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(activity_lobby.this, "Se ha creado la partida",
                                Toast.LENGTH_SHORT).show();
                        IdJuegoCreado = documentReference.getId();
                        PojoPartida Cola = new PojoPartida(gamertag, VistaTablero, IdJuegoCreado);
                        db.collection("Cola").add(Cola)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Toast.makeText(activity_lobby.this,
                                                "Esperando a otro Jugador", Toast.LENGTH_SHORT).show();
                                        Intent i = new Intent(activity_lobby.this, activity_juego.class);
                                        i.putExtra("IdJuego", IdJuegoCreado);
                                        i.putExtra("numTablero", VistaTablero);
                                        i.putExtra("tipoJugador", true);
                                        i.putExtra("gamertag", gamertag);

                                        startActivity(i);
                                    }
                                });
                    }
                });
    }

    public void clickUnirsePartida(View view) {
        Toast.makeText(this, "Lo estan presionando", Toast.LENGTH_SHORT).show();


    }
}
