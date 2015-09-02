/*
   Copyright 2015 IBM Corp.
    Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.ibm.mobileclientaccess.clientsdk.android.auth.facebook;

import android.content.Context;
import android.content.Intent;

import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationContext;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationListener;

import org.json.JSONException;
import org.json.JSONObject;

public class MCAFacebookAuthenticationManager implements
        AuthenticationListener
{
    private static String TAG = "MCAFBOAuth";

    private MCAFacebookAuthentication facebookAuthenticationHandler;

    private static final String FACEBOOK_REALM = "wl_facebookRealm";
    private static final String FACEBOOK_APP_ID_KEY = "facebookAppId";
    private static final String ACCESS_TOKEN_KEY = "accessToken";

    private CallbackManager callbackmanager;

    //singelton
    private static final Object lock = new Object();
    private static volatile MCAFacebookAuthenticationManager instance;
    private AuthenticationContext authContext;

    public static MCAFacebookAuthenticationManager getInstance() {
        MCAFacebookAuthenticationManager r = instance;
        if (r == null) {
            synchronized (lock) {    // While we were waiting for the lock, another
                r = instance;        // thread may have instantiated the object.
                if (r == null) {
                    r = new MCAFacebookAuthenticationManager();
                    instance = r;
                }
            }
        }
        return r;
    }

    private MCAFacebookAuthenticationManager() {
        callbackmanager = CallbackManager.Factory.create();
    }

    public void registerWithDefaultAuthenticationHandler(Context ctx) {
        registerWithAuthenticationHandler(ctx, new MCADefaultFacebookAuthenticationHandler(ctx));
    }

    public void registerWithAuthenticationHandler(Context ctx, MCAFacebookAuthentication handler) {
//        this.ctx = ctx;
        facebookAuthenticationHandler = handler;

        // Initialize SDK before setContentView(Layout ID)
        FacebookSdk.sdkInitialize(ctx);

        //register as authListener
        BMSClient.getInstance().registerAuthenticationListener(FACEBOOK_REALM, this);
    }

    public void onActivityResultCalled(int requestCode, int resultCode, Intent data) {
        facebookAuthenticationHandler.onActivityResultCalled(requestCode, resultCode, data);
    }

    public void onFacebookAccessTokenReceived(String facebookAccessToken) {
        JSONObject object = new JSONObject();
        try {
            object.put(ACCESS_TOKEN_KEY, facebookAccessToken);
            authContext.submitAuthenticationChallengeAnswer(object);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onFacebookAuthenticationFailure(JSONObject userInfo) {
        authContext.submitAuthenticationFailure(userInfo);
        authContext = null;
    }

    void setAuthenticationContext(AuthenticationContext authContext) {
        this.authContext = authContext;
    }


    @Override
    public void onAuthenticationChallengeReceived(AuthenticationContext authContext, JSONObject challenge, Context context) {
        try {
            String appId = challenge.getString(FACEBOOK_APP_ID_KEY);
            setAuthenticationContext(authContext);
            facebookAuthenticationHandler.handleAuthentication(context, appId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAuthenticationSuccess(JSONObject info) {
        authContext = null;
    }

    @Override
    public void onAuthenticationFailure(JSONObject info) {
        authContext = null;
    }
}