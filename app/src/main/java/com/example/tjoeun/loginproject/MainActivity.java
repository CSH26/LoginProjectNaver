package com.example.tjoeun.loginproject;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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

/*
    OAuth 2.0
    인증 결과로 접근 토큰과 갱신 토큰을 얻습니다.
    접근 토큰의 만료 기간은 몇 시간 정도이며, 갱신 토큰의 만료 기간은 몇 년으로 접근 토큰의 만료 기간보다 더 깁니다.
    API 호출 등으로 접근 토큰을 사용할 때는 만료 기간을 확인해야 합니다. 접근 토큰이 만료됐다면 갱신 토큰을 사용해 접근 토큰을 갱신해야 합니다.
    네이버 아이디로 로그인 라이브러리 4.x.x 버전에서는 OAuthLogin.refreshAccessToken() 메서드로 접근 토큰을 갱신합니다.
 */
public class MainActivity extends AppCompatActivity {

    final String OAUTH_CLIENT_ID = "ZsLHUof7EZP851NQS3_x";  //애플리케이션 등록 후 발급받은 클라이언트 아이디
    final String OAUTH_CLIENT_SECRET = "AXYu9a_4Up";        // 애플리케이션 등록 후 발급받은 클라이언트 시크릿 키
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
        OAuthLoginDefine.DEVELOPER_VERSION = true; // 배포 버전에서는 false로 설정 할 것.
        // logcat 로그에 네이버 아이디로 로그인 로그를 확인 할 수 있게 하려면 위의 코드 추가
        // 네이버 아이디로 로그인 라이브러리가 출력하는 logcat의 로그의 접두어는 NaverLoginOAuth입니다.
        mContext = this; // 타 메서드 호출에서 사용할 컨텍스트 객체
        mOAuthLoginModule = OAuthLogin.getInstance();   // 로그인 객체의 getInstance호출
        // 로그인 인스턴스 초기화
        // 이 메소드가 여러번 실행되어도 기존에 저장된 접근 토큰과 갱신 토큰은 삭제 되지 않는다.
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
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mOAuthLoginModule.logout(mContext); // 로그 아웃
        // 클라이언트에 저장된 토큰이 삭제되고 getState메서드가 NEED_LOGIN값을 반환한다.
        if(mOAuthLoginModule.getState(mContext) == OAuthLoginState.NEED_LOGIN) {
            Log.d("TAG", "로그아웃 결과 : 성공");
            new DeleteTokenTask().execute();
        }
        // 기존에 저장된 접근 토큰과 갱신 토큰을 삭제하려면 메소드 호출
        //mOAuthLoginModule.logout(this);
        // 또는
       // new DeleteTokenTask().execute();
    }

    private OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler() {
        @Override
        public void run(boolean success) {
            if (success) {
                // 로그인이 성공했을 경우 토큰을 얻을 수 있음.
                String accessToken = mOAuthLoginModule.getAccessToken(mContext);
                String refreshToken = mOAuthLoginModule.getRefreshToken(mContext);
                long expiresAt = mOAuthLoginModule.getExpiresAt(mContext);
                String tokenType = mOAuthLoginModule.getTokenType(mContext);

                mOauthAT.setText("접근 토큰: "+accessToken);
                mOauthRT.setText("로그인결과로 얻은 갱신 토큰: "+refreshToken);
                mOauthExpires.setText("접근 토큰의 만료시간: "+String.valueOf(expiresAt));
                // 단위 초
                mOauthTokenType.setText("토큰타입: "+tokenType);
                mOAuthState.setText("인증상태: "+mOAuthLoginModule.getState(mContext).toString());
                Toast.makeText(getApplicationContext(), "로그인 성공" ,Toast.LENGTH_SHORT).show();

                // 토큰이 있는 상태로 버튼을 보여주지 않음.
                if (OAuthLoginState.OK.equals(OAuthLogin.getInstance().getState(mContext))) {
                    mOAuthLoginButton.setVisibility(View.INVISIBLE);
                    new RequestApiTask().execute();  // 사용자 정보를 메시지박스로 띄운다.

                } else {
                    mOAuthLoginButton.setVisibility(View.VISIBLE);
                }

            } else {  //로그인이 실패했을 경우
                String errorCode = mOAuthLoginModule.getLastErrorCode(mContext).getCode();
                // 오류 코드 반환
                String errorDesc = mOAuthLoginModule.getLastErrorDesc(mContext);
                // 오류의 설명 반환
                Toast.makeText(getApplicationContext(), "로그인 실패 \n errorCode :"+errorCode+"\n errorDesc :"+errorDesc, Toast.LENGTH_SHORT).show();
            }
        };
    };

    private class DeleteTokenTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            //연동 해제 메소드
            boolean isSuccessDeleteToken = mOAuthLoginModule.logoutAndDeleteToken(mContext);

            if (!isSuccessDeleteToken) {
                //서버에 저장된 토큰을 삭제하지 못하고 클라이언트의 토큰만 삭제되어 연동해제 실패
                Log.d("TAG", "errorCode:" + mOAuthLoginModule.getLastErrorCode(mContext));
                Log.d("TAG", "errorDesc:" + mOAuthLoginModule.getLastErrorDesc(mContext));
            }
            else
                Log.d("TAG", "연동해제 결과: 성공");

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
            // 접근 토큰과 url을 같이 넘겨준다.
            String url = "https://openapi.naver.com/v1/nid/getUserProfile.xml";
            String at = mOAuthLoginModule.getAccessToken(mContext);
            // api를 요청하기전에 접근토큰의 만료시간이 얼마 남지 않았다면 갱신하고 api호출
            if(mOAuthLoginModule.getExpiresAt(mContext) <= 1000){
                try {
                    at = mOAuthLoginModule.refreshAccessToken(mContext);
                }
                catch (Exception e){
                    Log.d("TAG", "갱신 실패");
                    e.printStackTrace();
                }
            }
            pasingVersionData(mOAuthLoginModule.requestApi(mContext,at,url));
            // API를 호출할 때 인증 헤더에 접근 토큰 값을 넣습니다.
            // api호출을 성공하면 content body를 반환합니다.
            return null;
        }
        protected void onPostExecute(String content) {
            // requestApi를 요청 후에 바로 실행되는 메소드
            // requestApi가 호출 된 후에는 유저 정보에 로그인한 유저정보가 들어가 있어서 참조가능
            if (userInfo.getEmail() == null) {
                Log.d("TAG", "API호출에 실패했습니다. ");

            } else {
                Log.d("myLog", "email " + userInfo.getEmail());
                Log.d("myLog", "name " + userInfo.getName());
                Log.d("myLog", "nickname " + userInfo.getNickname());

                // 메시지 박스 띄우기
                AlertDialog.Builder getUser = new AlertDialog.Builder(mContext);
                getUser.setMessage("email : "+userInfo.getEmail()+"\n닉네임 : "+userInfo.getNickname()+"\n이름 : "+userInfo.getName()+
                        "\n나이 : "+userInfo.getAge()+"\n생일 : "+userInfo.getBirthday()+"\n성별 : "+userInfo.getGender());

                final Button profileButton = new Button(mContext);
                profileButton.setText("프로필 이미지 보기");
                profileButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ImageView profileImage = new ImageView(mContext);
                        profileImage.setImageResource(R.drawable.sghf0202);
                        AlertDialog.Builder showImage = new AlertDialog.Builder(mContext);
                        showImage.setView(profileImage);
                        showImage.show();
                    }
                });
                getUser.setView(profileButton);
                getUser.show();
            }
        }
    }
    private void pasingVersionData(String data){
        String f_array[]  = new String[9];

        try {
            XmlPullParserFactory parserCreator = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserCreator.newPullParser();
            // re
            InputStream input = new ByteArrayInputStream(data.getBytes("UTF-8"));
            parser.setInput(input, "UTF-8");

            int parserEvent = parser.getEventType();
            // 파서 이벤트 타입을 호출하면 이벤트의 상태가 반환.
            String tag;
            boolean inText = false;
            boolean lastMatTag = false;
            int colIdx = 0;

            //문서 끝까지 반복
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
