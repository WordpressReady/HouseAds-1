package com.lazygeniouz.houseAds.sample;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.lazygeniouz.house.ads.HouseAdsDialog;
import com.lazygeniouz.house.ads.HouseAdsInterstitial;
import com.lazygeniouz.house.ads.helper.HouseAdsHelper;
import com.lazygeniouz.house.ads.listener.AdListener;

public class MainActivity extends AppCompatActivity {

    private HouseAdsInterstitial interstitial;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //final String adURL = "https://www.lazygeniouz.com/houseAds/ads.json";
        //20190703 FZSM Test moving the file to a repository in github.com . Watchout raw format
        final String adURL = "https://raw.githubusercontent.com/WordpressReady/json/master/ads.json";
        final TextView txt = findViewById(R.id.txt);

        //20190703: FZSM: This example won't work if the app is installed.
        //However, how the object will tell that app is installed?
        final HouseAdsDialog houseAds = setupHA1(adURL);

        final HouseAdsDialog houseAdsDialog = setupHA2(adURL);

        setupI(adURL, txt);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                houseAds.loadAds();
                //houseAds.showAd();
            }
        });

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                houseAdsDialog.loadAds();
            }
        });

        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (interstitial.isAdLoaded()) interstitial.show();
            }
        });

        findViewById(R.id.clearCache).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HouseAdsHelper.clearGlideCache(MainActivity.this);
            }
        });

        findViewById(R.id.nativeActivity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, NativeAdActivity.class));
            }
        });
    }

    private void setupI(String adURL, final TextView txt) {
        interstitial = new HouseAdsInterstitial(MainActivity.this,adURL);
        interstitial.setAdListener(new AdListener() {
            @Override
            public void onAdLoadFailed() {
//                interstitial.loadAd();
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onAdLoaded() {
                txt.setText("Interstitial Ad Loaded");
                findViewById(R.id.button3).setEnabled(true);
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onAdClosed() {
                txt.setText("Interstitial Ad Closed");
                findViewById(R.id.button3).setEnabled(false);
                interstitial.loadAd();
            }

            @Override
            public void onAdShown() {
                Toast.makeText(MainActivity.this, "AdShown", Toast.LENGTH_SHORT).show();
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onApplicationLeft() {
                txt.setText("Interstitial Ad Closed");
                findViewById(R.id.button3).setEnabled(false);
                interstitial.loadAd();
                Toast.makeText(MainActivity.this, "Application Left", Toast.LENGTH_SHORT).show();
            }
        });
        interstitial.loadAd();
    }

    @NonNull
    private HouseAdsDialog setupHA2(String adURL) {
        final HouseAdsDialog houseAdsDialog = new HouseAdsDialog(this, adURL);

        //houseAdsDialog.setUrl(adURL);
        //A slightly changed version
        houseAdsDialog.setCardCorners(0);
        houseAdsDialog.setCtaCorner(0);
        houseAdsDialog.setForceLoadFresh(true); //reload json everytime.
        houseAdsDialog.showHeaderIfAvailable(true);
        //houseAdsDialog.loadAds();
        houseAdsDialog.setAdListener(new AdListener() {
            @Override
            public void onAdLoadFailed() {}

            @Override
            public void onAdLoaded() {}

            @Override
            public void onAdClosed() {}

            @Override
            public void onAdShown() {
                Toast.makeText(MainActivity.this, "AdShown", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onApplicationLeft() {}
        });
        return houseAdsDialog;
    }

    @NonNull
    private HouseAdsDialog setupHA1(String adURL) {
        final HouseAdsDialog houseAds = new HouseAdsDialog(MainActivity.this,adURL);
        //Defaults:
        //hideIfAppInstalled = true
        //setCardCorners /CtaCorner = 10 (modified by me)
        //setForceLoadFresh  = false
        // showHeader=true
        //So, we cant suppress safely many variables and I'm going to add a better and simple constructor, due we ALWAYS need an url

        //Unnecesary settings because they are default
        //houseAds.hideIfAppInstalled(true);
        //houseAds.setCardCorners(10); //FZSM: changed from 100 to 50. 0 means no corner
        //houseAds.setCtaCorner(10); //FZSM:changed 100 to 50 0 menas no corner or I think so
        //houseAds.setForceLoadFresh(false); //FZSM: Refresh loading all the ads ? (Not sure and nonsense!)

        //Disable header for this example
        houseAds.showHeaderIfAvailable(false); //20190705:I'm not actually sure why we would need that?
        //Let's add some category to filter ads based on this categories
        houseAds.adCategory("pregnancy");
        houseAds.adCategory("quizz");

        houseAds.setAdListener(new AdListener() {
            @Override
            public void onAdLoadFailed() {}

            @Override
            public void onAdLoaded() { }

            @Override
            public void onAdClosed() {}

            @Override
            public void onAdShown() {
                Toast.makeText(MainActivity.this, "AdShown", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onApplicationLeft() {}
        });
        return houseAds;
    }

}

