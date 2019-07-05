/*
 * Created by Darshan Pandya.
 * @itznotabug
 * Copyright (c) 2018.
 */

package com.lazygeniouz.house.ads;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.lazygeniouz.house.ads.helper.JsonPullerTask;
import com.lazygeniouz.house.ads.listener.AdListener;
import com.lazygeniouz.house.ads.modal.InterstitialModal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class HouseAdsInterstitial {
    private final Context mContext;
    private static AdListener mAdListener;
    private String url;
    private int lastLoaded = 0;
    private static boolean isAdLoaded = false;
    private static Bitmap bitmap;
    private static String packageName;
    private ArrayList<String> categories;

    public HouseAdsInterstitial(Context context, String val) {
        this.mContext = context;
        this.url=val;
        categories = new ArrayList<String>();
    }

    public void setAdListener(AdListener adListener) {
        mAdListener = adListener;
    }

    public void setUrl(String val) {
        this.url = val;
    }

    public void loadAd() {
        if (url.trim().equals("")) throw new IllegalArgumentException("Url is Blank!");
        else new JsonPullerTask(url, new JsonPullerTask.JsonPullerListener() {
            @Override
            public void onPostExecute(String result) {
                if (!result.trim().equals("")) setUp(result);
                else {
                    if (mAdListener != null) mAdListener.onAdLoadFailed();
                }
            }
        }).execute();
    }

    public boolean isAdLoaded() {
        return isAdLoaded;
    }

    public void adCategory(String category){
        categories.add(category);
    }

    private void setUp(String val) {
        ArrayList<InterstitialModal> modalArrayList = new ArrayList<>();
        String x = new String(new StringBuilder().append(val));

        try {
            JSONObject rootObject = new JSONObject(x);
            JSONArray array = rootObject.optJSONArray("apps");
            String app_category="";

            for (int object = 0; object < array.length(); object++) {
                JSONObject jsonObject = array.getJSONObject(object);

                if (!jsonObject.optString("app_adType").equals("interstitial")) {
                    continue;
                }
                if (excludeAds(jsonObject)) continue;

                InterstitialModal interstitialModal = new InterstitialModal();
                interstitialModal.setInterstitialImageUrl(jsonObject.optString("app_interstitial_url"));
                interstitialModal.setPackageOrUrl(jsonObject.optString("app_uri"));
                modalArrayList.add(interstitialModal);
            }

        } catch (JSONException e) { e.printStackTrace(); }

        if (modalArrayList.size() > 0) {
            final InterstitialModal modal = modalArrayList.get(lastLoaded);
            if (lastLoaded == modalArrayList.size() - 1) lastLoaded = 0;
            else lastLoaded++;

            Glide.with(mContext).load(modal.getInterstitialImageUrl()).asBitmap().into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap glideBitmap, @Nullable GlideAnimation<? super Bitmap> transition) {
                    bitmap = glideBitmap;
                    if (mAdListener != null) mAdListener.onAdLoaded();
                    isAdLoaded = true;
                }
            });
            packageName = modal.getPackageOrUrl();
        }
    }

    private boolean excludeAds(JSONObject jsonObject) {
        String app_disabled;
        String app_category;
        //20190703: FZSM -Disable/enbale ads just adding app_disabled.
        //This is a NEW feature, so if this options is not available, the ad is enabled by default.
        //normally it would be: "app_disabled": "true". Valid values: "TRUE", "tRuE","true"," true", "True   "
        // Any other value different than true would be false
        app_disabled=jsonObject.optString("app_disabled","false").trim().toLowerCase();
        if (app_disabled.equals("true")) {
            Log.i("House Ads",jsonObject.optString("app_title","[No Name]") +  " ad is disabled and excluded");
            return true;
        }

        //20190704: FZSM: Category Ads: filtering ads based on category.
        //By default if ad does not have category, it is included.
        //Example: ads for pregnancy won't be displayed in game apps. However we could display an add of a game in this app if we want (or not).
        app_category = jsonObject.optString("app_category", Common.NO_CATEGORY);
        if (app_category.equals(Common.NO_CATEGORY)) {
            Log.i("House Ads", jsonObject.optString("app_title","[No Name]") +  " included because it has no category.");
            return false; //If there is no categories, just add the ad.
        }

        //We have a category here. Notice that categories could be also empty
        if (categories.size() ==0 ) {
            Log.i("House Ads", jsonObject.optString("app_title","[No Name]") +  " included because there is no filter. Category: " + app_category);
            return false;
        }
        if (categories.contains(app_category)) {
            Log.i("House Ads", jsonObject.optString("app_title","[No Name]") +  " included because belongs to category " + app_category);
            return false;
        }
        Log.i("House Ads", jsonObject.optString("app_title","[No Name]") +  " excluded, it belongs to category " + app_category);
        return true;
    }

    public void show() {
        mContext.startActivity(new Intent(mContext, InterstitialActivity.class));
        if (mContext instanceof AppCompatActivity) ((AppCompatActivity) mContext).overridePendingTransition(0, 0);
    }

    public static class InterstitialActivity extends Activity {

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (mAdListener != null) mAdListener.onAdShown();

            setContentView(R.layout.interstitial);
            ImageView imageView = findViewById(R.id.image);
            ImageButton button = findViewById(R.id.button_close);

            imageView.setImageBitmap(bitmap);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    isAdLoaded = false;
                    if (packageName.startsWith("http")) {
                        Intent val = new Intent(Intent.ACTION_VIEW, Uri.parse(packageName));
                        val.setPackage("com.android.chrome");
                        if (val.resolveActivity(getPackageManager()) != null) startActivity(val);
                        else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(packageName)));

                        if (mAdListener != null) mAdListener.onApplicationLeft();
                        finish();
                    }
                    else {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
                            if (mAdListener != null) mAdListener.onApplicationLeft();
                            finish();
                        } catch (ActivityNotFoundException e) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + packageName)));
                            if (mAdListener != null) mAdListener.onApplicationLeft();
                            finish();
                        }
                    }
                }
            });
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                    isAdLoaded = false;
                    if (mAdListener != null) mAdListener.onAdClosed();
                }
            });
        }

        @Override
        public void onBackPressed() {
            isAdLoaded = false;
            if (mAdListener != null) mAdListener.onAdClosed();
            super.onBackPressed();
        }
    }
}
