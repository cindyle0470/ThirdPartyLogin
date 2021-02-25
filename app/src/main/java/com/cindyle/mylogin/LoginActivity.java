package com.cindyle.mylogin;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private static final int SIGN_IN_REQUEST_CODE = 1;
    private SignInButton signBtn;
    private Button signOut, fbSignInBtn, fbSignOutBtn;
    private TextView isLogin, id, token, mail;
    private ImageView img;
    private GoogleSignInClient googleSignInClient;
    private LoginManager fbLoginManager;
    private CallbackManager fbCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();

        // google 登入
        signBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignIn();
            }
        });

        // google 登出
        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignInClient.signOut();
                img.setImageDrawable(getDrawable(R.drawable.ic_angry_man));
                id.setText("");
                token.setText("");
                mail.setText("");
                isLogin.setText("帳號已登出");
            }
        });

        // Fb 登入
        fbSignInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fbCallbackManager = CallbackManager.Factory.create();
                fbLoginManager = LoginManager.getInstance();

                // 設定登錄模式
                fbLoginManager.setLoginBehavior(LoginBehavior.NATIVE_WITH_FALLBACK);

                loginFb();
            }
        });

        // Fb 登出
        fbSignOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fbLoginManager.logOut();
                img.setImageDrawable(getDrawable(R.drawable.ic_angry_man));
                id.setText("");
                token.setText("");
                mail.setText("");
                isLogin.setText("帳號已登出");
            }
        });
    }

    private void googleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.google_client_id))
                .build();

        googleSignInClient = GoogleSignIn.getClient(getApplicationContext(), gso);

        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, SIGN_IN_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SIGN_IN_REQUEST_CODE ) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }

        if (requestCode == CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode()) {
            fbCallbackManager.onActivityResult(requestCode, resultCode, data);
        }

    }

    private void handleSignInResult(Task<GoogleSignInAccount> task) {
        try{
            GoogleSignInAccount account = task.getResult(ApiException.class);

            isLogin.setText("登入成功");
            id.setText(account.getId());
            token.setText(account.getIdToken());
            mail.setText(account.getEmail());
            Uri uri = account.getPhotoUrl();

            Glide.with(this).load(uri).into(img);

            Log.i("TAG__", "获取登录信息成功：" + "\n" +
                    "ID：" + account.getId() + "\n" +
                    "photoUrl：" + account.getPhotoUrl() + "\n" +
                    "IdToken：" + account.getIdToken() + "\n" +
                    "Email：" + account.getEmail() );

            Log.i("TAG", uri.toString());

        } catch (Exception e) {
            isLogin.setText("登錄失敗");
            e.printStackTrace();
        }
    }

    private void loginFb() {
        List<String> permissions = new ArrayList<>();
        permissions.add("public_profile");
        permissions.add("email");
        permissions.add("user_friends");

        // 設定讀取權限
        fbLoginManager.logInWithReadPermissions(this, permissions);
        fbLoginManager.registerCallback(fbCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AccessToken currentAccessToken = loginResult.getAccessToken();

                GraphRequest request = GraphRequest.newMeRequest( currentAccessToken, new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {
                            if (response.getConnection().getResponseCode() == 200) {
                                String _id = object.getString("id");
                                String _name = object.getString("name");
                                String email = object.getString("email");
                                String url = object.getJSONObject("picture").getJSONObject("data").getString("url");
                                Log.i("TAG", url);

                                Glide.with(LoginActivity.this).load(url).into(img);

                                isLogin.setText("Name : " +_name);
                                id.setText("id : " + _id);
                                mail.setText(email);
                            }

                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                // 包入你想要得到的資料 送出request
                Bundle bundle = new Bundle();
                bundle.putString("fields", "id, name, email, picture");
                request.setParameters(bundle);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
                Log.i("TAG", "用戶取消登入");
            }

            @Override
            public void onError(FacebookException error) {
                Log.i("TAG", "登入失敗");
            }
        });
    }


    private void findViews() {
        signBtn = findViewById(R.id.sign_in_button);
        isLogin = findViewById(R.id.txt_login);
        id = findViewById(R.id.txt_id);
        token = findViewById(R.id.txt_token);
        mail = findViewById(R.id.txt_mail);
        img = findViewById(R.id.img);
        signOut = findViewById(R.id.btn2);
        fbSignInBtn = findViewById(R.id.fb_sign_in_button);
        fbSignOutBtn = findViewById(R.id.btn_out);
    }
}