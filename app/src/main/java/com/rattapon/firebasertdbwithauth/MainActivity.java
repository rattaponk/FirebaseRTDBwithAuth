package com.rattapon.firebasertdbwithauth;

import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etMessage;
    private EditText etUser;
    private Button btnSave;
    private Button btnShow;
    private TextView tvMessage;
    private SignInButton btnSignIn;

    private DatabaseReference mRootRef;
    private GoogleSignInClient mGoogleSignInClient;

    private FirebaseAuth mAuth;
    private String user_id;

    private static final int RC_SIGN_IN = 596;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitInstance();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        mRootRef = FirebaseDatabase.getInstance().getReference();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(MainActivity.this, gso);

        mAuth = FirebaseAuth.getInstance();
    }

    private void InitInstance() {
        etMessage = findViewById(R.id.et_message);
        etUser = findViewById(R.id.et_user);
        btnSave = findViewById(R.id.btn_save);
        btnShow = findViewById(R.id.btn_show);
        tvMessage = findViewById(R.id.tv_message);
        btnSignIn = findViewById(R.id.btn_signin);

        btnSave.setOnClickListener(this);
        btnShow.setOnClickListener(this);
        btnSignIn.setOnClickListener(this);
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);

    }

    private void signOut() {
        // Google sign out
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MainActivity.this, "Sign Out complete", Toast.LENGTH_SHORT).show();
                        TextView textView = (TextView) btnSignIn.getChildAt(0);
                        textView.setTextColor(Color.GRAY);
                        textView.setText("Sign In");
                    }
                });
        mGoogleSignInClient.revokeAccess();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
                TextView textView = (TextView) btnSignIn.getChildAt(0);
                textView.setTextColor(Color.RED);
                textView.setText("Sign Out");
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("GoogleAuth", "Google sign in failed", e);
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d("GoogleAuth", "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("GoogleAuth", "signInWithCredential:success");
                            Toast.makeText(MainActivity.this, "Authentication success.",
                                    Toast.LENGTH_SHORT).show();
                            FirebaseUser user = mAuth.getCurrentUser();

                            user_id = user.getUid();

//                            DatabaseReference mUserId = mRootRef.child("records");
//                            mUserId.setValue(user_id);
//                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("GoogleAuth", "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

//                            updateUI(null);
                        }

                    }
                });
    }

    private void saveRecord() {
//         Write a message to the database

        String stringMessage = etMessage.getText().toString();
        String stringUser = etUser.getText().toString();

        if (stringMessage.equals("") || stringUser.equals("")) {
            return;
        }

        DatabaseReference mUserRef = mRootRef
                .child("records").child(user_id).child("name");
        DatabaseReference mMessageRef = mRootRef
                .child("records").child(user_id).child("message");
        mUserRef.setValue(stringUser);
        mMessageRef.setValue(stringMessage);
        Toast.makeText(this, "Save complete", Toast.LENGTH_SHORT).show();
        etMessage.setText("");
        etUser.setText("");
    }

    private void fetchQuote() {
        mRootRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String stringUser = dataSnapshot.child("records").child(user_id).child("name").getValue().toString();
                String stringMessage = dataSnapshot.child("records").child(user_id).child("message").getValue().toString();

                tvMessage.setText("\"" + stringMessage + "\" --- " + stringUser + " ไม่ได้กล่าวไว้");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.btn_signin) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null)
                signIn();
            else {
                signOut();
                tvMessage.setText("");
            }
        } else if (i == R.id.btn_save) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null)
                saveRecord();
            else
                Toast.makeText(this, "Please sign in.", Toast.LENGTH_SHORT).show();
        } else if (i == R.id.btn_show) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null)
                fetchQuote();
            else
                Toast.makeText(this, "Please sign in.", Toast.LENGTH_SHORT).show();
        }
    }
}
