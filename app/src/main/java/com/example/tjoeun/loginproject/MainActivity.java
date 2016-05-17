package com.example.tjoeun.loginproject;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginDefine;
import com.nhn.android.naverlogin.OAuthLoginHandler;
import com.nhn.android.naverlogin.data.OAuthLoginState;
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    final String OAUTH_CLIENT_ID = "ZsLHUof7EZP851NQS3_x";  //클라이언트 키
    final String OAUTH_CLIENT_SECRET = "AXYu9a_4Up";        // 시크릿 키
    final String OAUTH_CLIENT_NAME = "LoginProject";    // 앱 네임

    private static Context mContext;
    private OAuthLogin mOAuthLoginModule;           // 로그인객체
    private OAuthLoginButton mOAuthLoginButton;
    private TextView mApiResultText;                // api 요청결과. null이면 실패
    private static TextView mOauthAT;               // 접근 토큰
    private static TextView mOauthRT;               // 갱신 토큰
    private static TextView mOauthExpires;          // 접근 토큰 만료시간
    private static TextView mOauthTokenType;        // 토큰 타입
    private static TextView mOAuthState;            // 인증상태. 연동 되면 OK 상태로 됨.
    private UserInfo userInfo;                      // 로그인한 유저 정보를 담을 클래스

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userInfo = new UserInfo();
        OAuthLoginDefine.DEVELOPER_VERSION = true;      // 버젼을 개발 버전으로 해야 정보누출이 안생김?
        mContext = this;
        mOAuthLoginModule = OAuthLogin.getInstance();   // 로그인 객체의 getInstance호출로 ㅊ
        mOAuthLoginModule.init(
                mContext
                ,OAUTH_CLIENT_ID
                ,OAUTH_CLIENT_SECRET
                ,OAUTH_CLIENT_NAME
                //,OAUTH_CALLBACK_INTENT
                // SDK 4.1.4 버전부터는 OAUTH_CALLBACK_INTENT변수를 사용하지 않습니다.
        );

        mOAuthLoginButton = (OAuthLoginButton)findViewById(R.id.buttonOAuthLoginImg);
        // 버튼을 이용해 로그인을 할경우 핸들러 지정.
        mOAuthLoginButton.setOAuthLoginHandler(mOAuthLoginHandler);
        mOAuthLoginButton.setBgResourceId(R.drawable.naver_login_green);

        mApiResultText = (TextView) findViewById(R.id.api_result_text);
        mOauthAT = (TextView) findViewById(R.id.oauth_access_token);
        mOauthRT = (TextView) findViewById(R.id.oauth_refresh_token);
        mOauthExpires = (TextView) findViewById(R.id.oauth_expires);
        mOauthTokenType = (TextView) findViewById(R.id.oauth_type);
        mOAuthState = (TextView) findViewById(R.id.oauth_state);

    }


    @Override
    protected void onResume() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        super.onResume();

    }

    @Override
    protected void onStop() {
        super.onStop();

        mOAuthLoginModule.getInstance().logout(mContext);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 기존에 저장된 접근 토큰과 갱신 토큰을 삭제하려면 메소드 호출
        //mOAuthLoginModule.logout(this);
        // 또는
        new DeleteTokenTask().execute();
    }

    private OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler() {
        @Override
        public void run(boolean success) {
            if (success) {
                String accessToken = mOAuthLoginModule.getAccessToken(mContext);
                String refreshToken = mOAuthLoginModule.getRefreshToken(mContext);
                long expiresAt = mOAuthLoginModule.getExpiresAt(mContext);
                String tokenType = mOAuthLoginModule.getTokenType(mContext);

                mOauthAT.setText("접근 토큰: "+accessToken);
                mOauthRT.setText("로그인결과로 얻은 갱신 토큰: "+refreshToken);
                mOauthExpires.setText("접근 토큰의 만료시간: "+String.valueOf(expiresAt));
                mOauthTokenType.setText("토큰타입: "+tokenType);
                mOAuthState.setText("인증상태: "+mOAuthLoginModule.getState(mContext).toString());
                Toast.makeText(getApplicationContext(), "로그인 성공" ,Toast.LENGTH_SHORT).show();


                // 토큰이 있는 상태로 버튼을 보여주지 않음.
                if (OAuthLoginState.OK.equals(OAuthLogin.getInstance().getState(mContext))) {
                    mOAuthLoginButton.setVisibility(View.INVISIBLE);
                } else {
                    mOAuthLoginButton.setVisibility(View.VISIBLE);
                }

                new RequestApiTask().execute();

                AlertDialog.Builder getUser = new AlertDialog.Builder(mContext);
                getUser.setMessage("email : "+userInfo.getEmail()+"\n"+"닉네임 : "+userInfo.getNickname()+"이름 : "+userInfo.getName()+
                        "나이 : "+userInfo.getAge()+"\n"+"생일 : "+userInfo.getBirthday()+"성별 : "+userInfo.getGender()+
                        "Enc_id : "+userInfo.getEnc_id()+"\n"+"프로필 이미지 : "+userInfo.getProfile_image()+"아이디 : "+userInfo.getId());

                /*
                RequestApiTask requestApiTask = new RequestApiTask();
                String requestApi = requestApiTask.doInBackground();
                requestApiTask.onPostExecute(requestApi);*/
                //Toast.makeText(getApplicationContext(), "결과는 : "+requestApi, Toast.LENGTH_LONG).show();
            } else {
                String errorCode = mOAuthLoginModule.getLastErrorCode(mContext).getCode();
                String errorDesc = mOAuthLoginModule.getLastErrorDesc(mContext);

                Toast.makeText(getApplicationContext(), "로그인 실패", Toast.LENGTH_SHORT).show();
            }
        };

    };

    private class DeleteTokenTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            boolean isSuccessDeleteToken = mOAuthLoginModule.logoutAndDeleteToken(mContext);

            if (!isSuccessDeleteToken) {
                // 서버에서 token 삭제에 실패했어도 클라이언트에 있는 token 은 삭제되어 로그아웃된 상태이다
                // 실패했어도 클라이언트 상에 token 정보가 없기 때문에 추가적으로 해줄 수 있는 것은 없음
                Log.d("TAG", "errorCode:" + mOAuthLoginModule.getLastErrorCode(mContext));
                Log.d("TAG", "errorDesc:" + mOAuthLoginModule.getLastErrorDesc(mContext));
            }
            else
                Toast.makeText(getApplicationContext(), "로그아웃:" ,Toast.LENGTH_SHORT).show();
            return null;
        }
        protected void onPostExecute(Void v) {
            updateView();
        }
    }

    private void updateView() {
        mOauthAT.setText(mOAuthLoginModule.getAccessToken(mContext));
        mOauthRT.setText(mOAuthLoginModule.getRefreshToken(mContext));
        mOauthExpires.setText(String.valueOf(mOAuthLoginModule.getExpiresAt(mContext)));
        mOauthTokenType.setText(mOAuthLoginModule.getTokenType(mContext));
        mOAuthState.setText(mOAuthLoginModule.getState(mContext).toString());
    }


    private class RequestApiTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            mApiResultText.setText((String) "");
        }
        @Override
        protected String doInBackground(Void... params) {
            String url = "https://openapi.naver.com/v1/nid/getUserProfile.xml";
            String at = mOAuthLoginModule.getAccessToken(mContext);
            pasingVersionData(mOAuthLoginModule.requestApi(mContext,at,url));
            return null;
        }
        protected void onPostExecute(String content) {
            if (userInfo.getEmail() == null) {
                Toast.makeText(MainActivity.this, "로그인 실패하였습니다.  잠시후 다시 시도해 주세요!!",Toast.LENGTH_SHORT).show();
            } else {
                Log.d("myLog", "email " + userInfo.getEmail());
                Log.d("myLog", "name " + userInfo.getName());
                Log.d("myLog", "nickname " + userInfo.getNickname());
            }
        }
    }

    private void pasingVersionData(String data){
        String f_array[]  = new String[9];

        try {
            XmlPullParserFactory parserCreator = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserCreator.newPullParser();
            InputStream input = new ByteArrayInputStream(data.getBytes("UTF-8"));
            parser.setInput(input, "UTF-8");

            int parserEvent = parser.getEventType();
            String tag;
            boolean inText = false;
            boolean lastMatTag = false;
            int colIdx = 0;

            while (parserEvent != XmlPullParser.END_DOCUMENT) {
                switch (parserEvent) {
                    case XmlPullParser.START_TAG:
                        tag = parser.getName();
                        if (tag.compareTo("xml") == 0) {
                            inText = false;
                        } else if (tag.compareTo("data") == 0) {
                            inText = false;
                        } else if (tag.compareTo("result") == 0) {
                            inText = false;
                        } else if (tag.compareTo("resultcode") == 0) {
                            inText = false;
                        } else if (tag.compareTo("message") == 0) {
                            inText = false;
                        } else if (tag.compareTo("response") == 0) {
                            inText = false;
                        } else {
                            inText = true;
                        }
                        break;
                    case XmlPullParser.TEXT:
                        tag = parser.getName();
                        if (inText) {
                            if (parser.getText() == null) {
                                f_array[colIdx] = "";
                            } else {
                                f_array[colIdx] = parser.getText().trim();
                            }
                            colIdx++;
                        }
                        inText = false;
                        break;
                    case XmlPullParser.END_TAG:
                        tag = parser.getName();
                        inText = false;
                        break;
                }
                parserEvent = parser.next();
            }

        } catch (Exception e) {
            Log.e("dd", "Error in network call", e);
        }
        userInfo.setEmail(f_array[0]);
        userInfo.setNickname(f_array[1]);
        userInfo.setEnc_id(f_array[2]);
        userInfo.setProfile_image(f_array[3]);
        userInfo.setAge(f_array[4]);
        userInfo.setGender(f_array[5]);
        userInfo.setId(f_array[6]);
        userInfo.setName(f_array[7]);
        userInfo.setBirthday(f_array[8]);

    }
}
