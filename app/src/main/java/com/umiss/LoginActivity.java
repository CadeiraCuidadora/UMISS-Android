package com.umiss;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Response;

import org.json.JSONException;

import network.UMISSRest;

public class LoginActivity extends AppCompatActivity {

    public static final String IS_LOGGED = "isLogged";
    public static final String TOKEN = "token";
    public static final String PASSWORD = "password";
    private String LOGIN_REQUEST = "api-auth-token/";
    private String CONNECTION_ERROR = "Server is offline!";

    private EditText userEditText;
    private EditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        TextView register = (TextView) findViewById(R.id.button_register);
        register.setOnClickListener(registerOnClickListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }

    public void login(View view) throws JSONException {

        userEditText = (EditText) findViewById(R.id.editText_user);
        passwordEditText = ((EditText) findViewById(R.id.editText_password));
        String token = FirebaseInstanceId.getInstance().getToken();

        authenticate(userEditText.getText().toString(), passwordEditText.getText().toString(), token);
    }

    private void authenticate(String user, final String password, final String token) {

        final JsonObject jsonObject = getJson(user, password, token);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setMessage("Buscando informações...");
        progressDialog.show();

        UMISSRest.login(UMISSRest.getAbsoluteURL(LOGIN_REQUEST), jsonObject, getApplicationContext(), new FutureCallback<JsonObject> (){
            @Override
            public void onCompleted(Exception e, JsonObject result) {

                Log.d("Login", "tried");

                if ( e instanceof ClassCastException ){

                    loginHasFailed(userEditText, passwordEditText);
                    Log.d("Login", "failed");
                    progressDialog.dismiss();
                }else {

                    try {

                        if (result.has("token")) {

                            SharedPreferences sharedPreferences = getSharedPreferences("data", MODE_PRIVATE);
                            pushLoginCredentials(sharedPreferences, result.get(TOKEN).getAsString(), password);
                            sendAndroidToken(password, result.get(TOKEN).getAsString(), token);
                            System.out.println("token;:: " + result.get(TOKEN).getAsString());
                            startMainActivity();
                            Log.d("Login", "success");
                            progressDialog.dismiss();
                        }else{

                            Log.d("Login", result.toString());
                            loginHasFailed(userEditText, passwordEditText);
                            progressDialog.dismiss();
                        }
                    } catch (Exception x) {

                        Toast.makeText(getApplicationContext(), CONNECTION_ERROR, Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                        Log.d("Login", "Connection error");
                    }
                }
            }
        });
    }

    private JsonObject getJson(String user, String password, String token){

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", user);
        jsonObject.addProperty("password", password);

        return jsonObject;
    }

    private void sendAndroidToken(final String password, final String token,final String androidToken){


        UMISSRest.get(UMISSRest.MONITORS + "/1", token, new FutureCallback<JsonObject>() {
            @Override
            public void onCompleted(Exception e, JsonObject result) {

                JsonObject jsonObject = new JsonObject();

                try {
                    jsonObject.addProperty("username", result.get("data").getAsJsonObject().get("attributes").getAsJsonObject()
                            .get("username").getAsString());
                    jsonObject.addProperty("token", result.get("data").getAsJsonObject().get("attributes").getAsJsonObject()
                            .get("token").getAsString());
                    jsonObject.addProperty("password", password);
                    jsonObject.addProperty("android_token", androidToken);

                    String id = result.get("data").getAsJsonObject().get("id").getAsString();
                    jsonObject.addProperty("id", id);

                    if (result == null)
                        Log.d("LoginActivityget3", e.toString());
                    else
                        Log.d("LoginActivityget3", result.toString());

                    Log.d("LoginActivityput", UMISSRest.MONITORS + "/" + id);

                    UMISSRest.sendAndroidToken(UMISSRest.MONITORS + "/" + id, getApplicationContext(), jsonObject,
                            token, new FutureCallback<Response<JsonObject>>() {
                                @Override
                                public void onCompleted(Exception e, Response<JsonObject> result) {
                                    Log.d("LoginActivity4", String.valueOf(result.getHeaders().code()));

                                    if ( result.getHeaders().code() != 200 ){

                                        Toast.makeText(getApplicationContext(),
                                                "Não foi possível enviar o token para o servidor, faça login novamente.", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }, "false");
                }catch (Exception exception){
                    Toast.makeText(getApplicationContext(), "Problemas de conexão...", Toast.LENGTH_LONG).show();
                }
            }
        }, getApplicationContext());
    }

    private void pushLoginCredentials(SharedPreferences sharedPreferences, String token, String password) {

        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        prefEditor.putString(IS_LOGGED, "logged");
        prefEditor.commit();
        //TODO: in case this token is necessary
        prefEditor.putString(TOKEN, token);
        prefEditor.commit();

        prefEditor.putString(PASSWORD, password);
        prefEditor.commit();
    }

    private void loginHasFailed(EditText user, EditText password){

        Toast.makeText(this, "Invalid credentials!", Toast.LENGTH_SHORT).show();
        user.setText("");
        password.setText("");
        user.requestFocus();
    }

    private void startMainActivity(){

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /* on clicks */

    private View.OnClickListener registerOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(intent);
        }
    };
}
