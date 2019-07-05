/*
 * Created by Darshan Pandya.
 * @itznotabug
 * Copyright (c) 2018.
 */

package com.lazygeniouz.house.ads;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.lazygeniouz.house.ads.helper.HouseAdsHelper;
import com.lazygeniouz.house.ads.helper.JsonPullerTask;
import com.lazygeniouz.house.ads.listener.AdListener;
import com.lazygeniouz.house.ads.modal.DialogModal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class HouseAdsDialog {
    private final Context mCompatActivity;
    private String jsonUrl;
    private String jsonRawResponse = "";

    private boolean showHeader = true;
    private boolean forceLoadFresh = false;
    private boolean hideIfAppInstalled  = true;
    private int cardCorner = 10;
    private int ctaCorner = 10;
    private ArrayList<DialogModal> adArray = new ArrayList<>();
    private static boolean isAdLoaded = false;
    private ArrayList<String> categories;


    //For version 1.0 is good enough.
    //20190704: Few things needed
    // - 'caching' json

    private AdListener mAdListener;
    private AlertDialog dialog;

    private static int lastLoaded = 0;

    public HouseAdsDialog(Context context,String url) {
        this.mCompatActivity = context;
        this.jsonUrl =url;
        isAdLoaded = false;
        categories = new ArrayList<String>();
    }

    public void setUrl(String url) {
        this.jsonUrl = url;
    }

    public void adCategory(String category){
        categories.add(category);
    }

    public void showHeaderIfAvailable(boolean val) {
        this.showHeader = val;
    }

    public void setCardCorners(int corners) {
        this.cardCorner = corners;
    }

    public void setCtaCorner(int corner) {
        this.ctaCorner = corner;
    }

    public void setForceLoadFresh(boolean val) {
        this.forceLoadFresh = val;
    }

    public void setAdListener(AdListener listener) {
        this.mAdListener = listener;
    }

    public boolean isArrayLoaded() {
        return (adArray.size()!=0);
    }

    public boolean isAdLoaded() {
       return isAdLoaded;
    }

    public void hideIfAppInstalled(boolean val) {
        this.hideIfAppInstalled = val;
    }

    /**
     * 20190705: name not very intuitive and in fact made me confuse about loading and showing.
     */
    public void loadAds() {

        if (jsonUrl.trim().equals("")) throw new IllegalArgumentException("Url is Blank!");
        else {
            if (forceLoadFresh || jsonRawResponse.equals("")) new JsonPullerTask(jsonUrl, new JsonPullerTask.JsonPullerListener() {
                @Override
                public void onPostExecute(String result) {
                    if (!result.trim().equals("")) {
                        jsonRawResponse = result;
                        setUp(result);
                    }
                    else {
                        if (mAdListener != null) mAdListener.onAdLoadFailed();
                    }
                }
            }).execute();
            if (!forceLoadFresh && !jsonRawResponse.trim().equals("")) setUp(jsonRawResponse);
        }
    }

    public void showAd() {
        //fillDialog();
        if (dialog != null) dialog.show();
    }


    /**
     * 20190704: FZSM Few things first:
     * -Added a feature to filter disabled ads
     * -Added a feature to filter ads by category
     * -IMPORTANT limitation: at least ALWAYS we need at least ONE ad from type dialog, which can't be disabled or from other category.
     * -Please analyze how to overcome this limitation, I wouldn't want to check every time if the entire array is like this.
     * -There is another important issue: setUp loads the array, and prefills AND displays an ad. We would need split a setup the array and THEN prefill it
     * without loading it. How to solve this?
     */
    @SuppressLint("NewApi")
    private void setUp(String response) {

        String app_category="";
        String app_disabled="";
        Boolean arrayLoaded;
        try {
            //If the ad isn't loaded , load it . If forceLoadFresh, re load it
            arrayLoaded=isArrayLoaded();
            if (!arrayLoaded || forceLoadFresh) {
                arrayLoaded=buildAdArray(response, "dialog");
            }
            if (!arrayLoaded) {
                Log.i("In House Ad","Array was not loaded. Refreshed...");
            }
            if (forceLoadFresh) {
                Log.i("In House Ad","Force load refresh in action...");
            }
        }
        catch (JSONException e) {
                e.printStackTrace();
                Log.i("In House Ad","Json exception: " + e.getMessage());
            }

        if (adArray.size() == 0) {
            Log.i("In House Ad","setUp - there is no ad to setup, early exit!");
            return;
        }

        fillDialog(); //prefill data
    }

    //Try to prefill the data of one ad.
    private void fillDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mCompatActivity);
        final DialogModal dialogModal = adArray.get(lastLoaded);
        //Circular ads
        if (lastLoaded == adArray.size() - 1) lastLoaded = 0;
        else lastLoaded++;

        @SuppressLint("InflateParams") final View view = LayoutInflater.from(mCompatActivity).inflate(R.layout.dialog, null);

        if (dialogModal.getIconUrl().trim().equals("") || !dialogModal.getIconUrl().trim().contains("http")) throw new IllegalArgumentException("Icon URL should not be Null or Blank & should start with \"http\"");
        if (!dialogModal.getLargeImageUrl().trim().equals("") && !dialogModal.getIconUrl().trim().contains("http")) throw new IllegalArgumentException("Header Image URL should start with \"http\"");
        if (dialogModal.getAppTitle().trim().equals("") || dialogModal.getAppDesc().trim().equals("")) throw new IllegalArgumentException("Title & description should not be Null or Blank.");


        CardView cardView = view.findViewById(R.id.houseAds_card_view);
        cardView.setRadius(cardCorner);

        final Button cta = view.findViewById(R.id.houseAds_cta);
        GradientDrawable gd = (GradientDrawable) cta.getBackground();
        gd.setCornerRadius(ctaCorner);

        final ImageView icon = view.findViewById(R.id.houseAds_app_icon);
        final ImageView headerImage = view.findViewById(R.id.houseAds_header_image);
        TextView title = view.findViewById(R.id.houseAds_title);
        TextView description = view.findViewById(R.id.houseAds_description);
        final RatingBar ratings = view.findViewById(R.id.houseAds_rating);
        TextView price = view.findViewById(R.id.houseAds_price);


        Glide.with(mCompatActivity).load(dialogModal.getIconUrl()).asBitmap().into(new SimpleTarget<Bitmap>(Integer.MIN_VALUE, Integer.MIN_VALUE) {
            @Override
            public void onResourceReady(@NonNull Bitmap glideBitmap, GlideAnimation<? super Bitmap> p2) {
                icon.setImageBitmap(glideBitmap);

                Palette palette = Palette.from(glideBitmap).generate();
                int dominantColor = palette.getDominantColor(ContextCompat.getColor(mCompatActivity, R.color.colorAccent));

                if (!showHeader) {
                    isAdLoaded = true;
                    if (mAdListener != null) mAdListener.onAdLoaded();
                }
                GradientDrawable drawable = (GradientDrawable)  cta.getBackground();
                drawable.setColor(dominantColor);
                Float rating = dialogModal.getRating();
                if (rating != 0) {
                    ratings.setRating(rating);
                    Drawable ratingsDrawable = ratings.getProgressDrawable();
                    DrawableCompat.setTint(ratingsDrawable, dominantColor);
                } else ratings.setVisibility(View.GONE);
            }});

        if (!dialogModal.getLargeImageUrl().trim().equals("") && showHeader) headerImage.setVisibility(View.VISIBLE);
        Glide.with(mCompatActivity).load(dialogModal.getLargeImageUrl()).asBitmap().listener(new RequestListener<String, Bitmap>() {
            @Override
            public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
                isAdLoaded = true;
                if (mAdListener != null) mAdListener.onAdLoaded();
                headerImage.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                return false;
            }
        }).into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable GlideAnimation<? super Bitmap> transition) {
                if (showHeader) {
                    isAdLoaded = true;
                    if (mAdListener != null) mAdListener.onAdLoaded();
                }
                headerImage.setImageBitmap(bitmap);
            }
        });

        title.setText(dialogModal.getAppTitle());
        description.setText(dialogModal.getAppDesc());
        cta.setText(dialogModal.getCtaText());
        if (dialogModal.getPrice().trim().equals("")) price.setVisibility(View.GONE);
        else price.setText(String.format("Price: %s", dialogModal.getPrice()));


        builder.setView(view);
        dialog = builder.create();
        //noinspection ConstantConditions
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (mAdListener != null) mAdListener.onAdShown();
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (mAdListener != null) mAdListener.onAdClosed();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (mAdListener != null) mAdListener.onAdClosed();
            }
        });

        cta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();

                String packageOrUrl = dialogModal.getPackageOrUrl();
                if (packageOrUrl.trim().startsWith("http")) {
                    mCompatActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(packageOrUrl)));
                    if (mAdListener != null) mAdListener.onApplicationLeft();
                }
                else {
                    try {
                        mCompatActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageOrUrl)));
                        if (mAdListener != null) mAdListener.onApplicationLeft();
                    } catch (ActivityNotFoundException e) {
                        if (mAdListener != null) mAdListener.onApplicationLeft();
                        mCompatActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + packageOrUrl)));
                    }
                }
            }
        });
        showAd();
    }

    /**
     * Build an array of ads.
     * Pay attention this method build o re-build an array according if we force the load or not.
     * @param response
     * @param ad_type
     * @throws JSONException
     */
    private boolean buildAdArray(String response, String ad_type) throws JSONException {
        String app_disabled;
        String app_category;
        JSONObject rootObject = new JSONObject(response);
        JSONArray array = rootObject.optJSONArray("apps"); //Json apps variable which is always an array.
        this.adArray = new ArrayList<>(); //Reset array

        for (int index = 0; index < array.length(); index++) {
            final JSONObject jsonObject = array.getJSONObject(index);


            if (hideIfAppInstalled && !jsonObject.optString("app_uri").startsWith("http") &&  HouseAdsHelper.isAppInstalled(mCompatActivity, jsonObject.optString("app_uri"))) array.remove(index);
            //ToDo: Handle remove() on pre 19!
            else {
                if (!jsonObject.optString("app_adType").equals(ad_type)) { //include only dialog types.
                    continue;
                }
                if (excludeAds(jsonObject)) continue;

                final DialogModal dialogModal = new DialogModal();
                dialogModal.setAppTitle(jsonObject.optString("app_title"));
                dialogModal.setAppDesc(jsonObject.optString("app_desc"));
                dialogModal.setIconUrl(jsonObject.optString("app_icon"));
                dialogModal.setLargeImageUrl(jsonObject.optString("app_header_image"));
                dialogModal.setCtaText(jsonObject.optString("app_cta_text"));
                dialogModal.setPackageOrUrl(jsonObject.optString("app_uri"));
                dialogModal.setRating(jsonObject.optString("app_rating"));
                dialogModal.setPrice(jsonObject.optString("app_price"));

                adArray.add(dialogModal);
            }
        }
        return isArrayLoaded();
    }

    //Find out if the current ad should be exlcuded.
    //Return true if it needs to be excluded

    /**
     * Find out if the ad should be excluded
     * It can happen if the ad is disabled (app_disabled=true) or the ad does not belong to the ad categories for this app.
     * @param jsonObject
     * @return
     */
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

}






















