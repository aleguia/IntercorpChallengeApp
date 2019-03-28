package com.leguia.intercorpchallengeapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final int RC_SIGN_IN = 4592;

    private FirebaseAuth mAuth;
    private FirebaseDatabase database;

    private ValueEventListener mUserListener;

    private static final String TAG = MainActivity.class.getSimpleName();

    private EditText nombre,apellido,edad,fechaNacimiento;
    private Button logOutBtn, saveBtn;
    private DatabaseReference myRef;
    private DatabaseReference userRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();


        nombre= findViewById(R.id.name_edittext);
        apellido = findViewById(R.id.lastname_edittext);
        edad = findViewById(R.id.age_edittext);
        fechaNacimiento= findViewById(R.id.birthdate_edittext);
        saveBtn = findViewById(R.id.save_button);
        logOutBtn = findViewById(R.id.signout_button);

        fechaNacimiento.setFocusable(false);
        fechaNacimiento.setClickable(true);

        fechaNacimiento.setOnClickListener(this);
        saveBtn.setOnClickListener(this);
        logOutBtn.setOnClickListener(this);

    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void updateUI(FirebaseUser currentUser) {
        if(currentUser!=null){

            myRef = database.getReference("users");
            userRef = database.getReference("users").child(currentUser.getUid());
            retreiveUserData();
            Log.d(TAG, currentUser.getUid());
        }else{
            goToAuthenticate();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mUserListener != null) {
            userRef.removeEventListener(mUserListener);
        }
    }

    private void signOut() {

        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                       
                        goToAuthenticate();
                    }
                });

    }


    private void goToAuthenticate(){
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build(),
                new AuthUI.IdpConfig.FacebookBuilder().setPermissions(Arrays.asList("email", "public_profile")).build(),
                new AuthUI.IdpConfig.PhoneBuilder().build()
        );


        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);


    }

    private void retreiveUserData(){
        ValueEventListener userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User userRetreived = dataSnapshot.getValue(User.class);

                setEditTextValues(userRetreived);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());

            }
        };

        userRef.addValueEventListener(userListener);

        mUserListener = userListener;
    }

    private void setEditTextValues(User user){
        if(user==null)
            return;
        if (user.getName()!=null) {
            nombre.setText(user.getName());
        }
        if (user.getLastname()!=null) {
            apellido.setText(user.getLastname());
        }

            edad.setText(String.valueOf(user.getAge()));

        if (user.getBirthdate()!=null) {
            fechaNacimiento.setText(user.getBirthdate());
        }
    }

    private void showMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void saveUserData(User user){
        myRef.child(mAuth.getCurrentUser().getUid()).setValue(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                showMessage("Se guardó datos usuario");
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showMessage("Algo falló. Por favor intente de nuevo");
                    }
                });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
//                Log.d(TAG, response.getIdpToken());
                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();



            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                    if(response==null){
                        showMessage("Cancelado");
                        return;
                    }



                    showMessage(String.valueOf(response.getError().getErrorCode()));


            }
        }
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        switch(id){
            case R.id.save_button:{
                if(!validateForm())
                    return;
                Date date = new Date();
                User userToSave = new User();
                userToSave.setEmail(firebaseUser.getEmail());
                userToSave.setPhoneNumber(firebaseUser.getPhoneNumber());
                userToSave.setName(nombre.getText().toString());
                userToSave.setLastname(apellido.getText().toString());
                userToSave.setAge(Integer.parseInt(edad.getText().toString()));
                userToSave.setBirthdate(fechaNacimiento.getText().toString());
                saveUserData(userToSave);

                break;
            }

            case R.id.signout_button:{
                signOut();
                break;
            }

            case R.id.birthdate_edittext:{
                Calendar calendar = Calendar.getInstance();
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int month = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);
                //AlertDialog.THEME_HOLO_LIGHT
                //R.style.datepicker
                DatePickerDialog pickerDialog = new DatePickerDialog(this, AlertDialog.THEME_HOLO_LIGHT, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

                        fechaNacimiento.setText(twoDigits(dayOfMonth)+ "/" + twoDigits(month +1)  + "/" + year);
                    }
                }, year, month, day);
                //pickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                //pickerDialog.getDatePicker().init(year,month,day,null);
                pickerDialog.show();
            }
        }
    }

    private boolean validateForm() {
        List<EditText> listToValidate = Arrays.asList(nombre,apellido, edad, fechaNacimiento);
        for(EditText editText : listToValidate){
            if(TextUtils.isEmpty(editText.getText())) {
                //Toast.makeText(this, "Por favor llenar todos los campos", Toast.LENGTH_LONG).show();
                editText.setError(getResources().getString(R.string.edittext_error));
               return false;
            }

        }

        return true;
    }

    private String twoDigits(int n) {
        return (n<=9) ? ("0"+n) : String.valueOf(n);
    }

}
