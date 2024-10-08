package com.agcurations.aggallerymanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AdapterMaturityRatings extends ArrayAdapter<String[]> {

    //Don't change the ratings IDs or levels unless user data is also updated.
    //  Doing so may expose users to content not intended for their maturity level.
    //  You must update tags, catalog items, and user accounts if you wish to modify.

    //Adapted from ESRB Ratings
    public final static int MATURITY_RATING_RP = 0;  //DO NOT CHANGE WITHOUT UPDATING STORED USER DATA.
    public final static int MATURITY_RATING_EC = 1;  //DO NOT CHANGE WITHOUT UPDATING STORED USER DATA.
    public final static int MATURITY_RATING_E = 2;  //DO NOT CHANGE WITHOUT UPDATING STORED USER DATA.
    public final static int MATURITY_RATING_E10 = 3;  //DO NOT CHANGE WITHOUT UPDATING STORED USER DATA.
    public final static int MATURITY_RATING_T = 4;  //DO NOT CHANGE WITHOUT UPDATING STORED USER DATA.
    public final static int MATURITY_RATING_M = 5;  //DO NOT CHANGE WITHOUT UPDATING STORED USER DATA.
    public final static int MATURITY_RATING_AO = 6;  //DO NOT CHANGE WITHOUT UPDATING STORED USER DATA.
    public final static int MATURITY_RATING_IB = 7;  //DO NOT CHANGE WITHOUT UPDATING STORED USER DATA.
    public final static int MATURITY_RATING_X = 8;  //DO NOT CHANGE WITHOUT UPDATING STORED USER DATA.

    public final static int RATINGS_COUNT = 9;

    public final static int MATURITY_RATING_CODE_INDEX = 0;
    public final static int MATURITY_RATING_NAME_INDEX = 1;
    public final static int MATURITY_RATING_DESCRIPTION_INDEX = 2;
    public final static String[][] MATURITY_RATINGS = {  //DO NOT CHANGE WITHOUT UPDATING STORED USER DATA.
            {"RP",  "Rating Pending",           "Titles with the RP rating have not yet been assigned a final ESRB rating."},
            {"EC",  "Early Childhood",          "Suitable for children aged 3 or older; there will be no inappropriate content. E.g., Dora the Explorer, Dragon Tales."},
            {"E",   "Everyone",                 "Suitable for all age groups. The game should not contain any sounds or images likely to scare young children. No bad language should be used. E.g., Just Dance, FIFA.",},
            {"E10+","Everyone 10 and Older",    "Suitable for those aged 10 or above. There could be mild forms of violence, and some scenes might be frightening for children. E.g., Minecraft Dungeons, Plants vs Zombies.",},
            {"T",	"Teen",                     "Suitable for those aged 13 or above. The game could feature more realistic and graphic scenes of violence. E.g., Fortnite, Sims 4.",},
            {"M",	"Mature",                   "Suitable for those aged 17 or above. This rating is used when the violence becomes realistic and would be expected in real life. Bad language, and the use of tobacco, alcohol, or illegal drugs can also be present. E.g., Ark: Survival Evolved, Destiny 2.",},
            {"AO",	"Adults Only",              "Suitable for adults aged 18 or above. The adult classification is used when there are extreme levels of violence and motiveless killing. Glamorization of drugs, gambling, and sexual activity can also be featured. E.g., Grand Theft Auto V, Fallout 4.",},
            {"IB",  "Implicit Bias",            "Associated with items eschewed by mainstream society, may vary by country, religion, or culture. Implicit Bias is defined as negative associations expressed automatically. May include L.G.B.T.Q.I.A topics in socially-repressive countries. Includes some NFSW content."},
            {"X",  "X-Rated",                   "Associated with items highly eschewed by mainstream society, such as pron, or some topics in certain countries, religions, or cultures. All content should be considered NFSW."},
             //DO NOT CHANGE ORDER OR INDEXING WITHOUT UPDATING STORED USER DATA.
    };

    public static ArrayList<String> GetMaturityStringsForUserAssignment(){
        ArrayList<String> alsShortStrings = new ArrayList<>();
        for(int i = 0; i < RATINGS_COUNT; i++){
            if(i <= GlobalClass.gicuCurrentUser.iMaturityLevel) {
                //Don't allow a user to create a user with a maturity rating higher than theirself.
                if (i != MATURITY_RATING_RP) {
                    //Don't add "rating pending" to list of short strings. Those are for tags only.
                    String sLine = MATURITY_RATINGS[i][0] + " - " + MATURITY_RATINGS[i][1];
                    alsShortStrings.add(sLine);
                }
            }
        }
        return alsShortStrings;
    }

    public static ArrayList<String> GetMaturityStringsForTagAssigment(){
        ArrayList<String> alsShortStrings = new ArrayList<>();
        for(int i = 0; i < RATINGS_COUNT; i++){
            if(i <= GlobalClass.gicuCurrentUser.iMaturityLevel) {
                //Don't allow a user to create a user with a maturity rating higher than theirself.
                String sLine = MATURITY_RATINGS[i][0] + " - " + MATURITY_RATINGS[i][1];
                alsShortStrings.add(sLine);
            }
        }
        return alsShortStrings;
    }

    public static int GetMaturityFromShortString(String sShortString){
        String[] sSplit = sShortString.split(" . ");
        String sItem = sSplit[0];
        int iMaturityLevel = GlobalClass.giDefaultUserMaturityRating;
        for(int i = 0; i < RATINGS_COUNT; i++){
            String[] sMaturityRecord = MATURITY_RATINGS[i];
            if(sItem.equals(sMaturityRecord[AdapterMaturityRatings.MATURITY_RATING_CODE_INDEX])){
                iMaturityLevel = i;
                break;
            }
        }
        return iMaturityLevel;
    }

    public static ArrayList<String[]> GetAssignmentArrayList() {
        //This function used to seed the adapter the same way for every usage.
        ArrayList<String[]> alsItems = new ArrayList<>();
        for (int i = 0; i < AdapterMaturityRatings.MATURITY_RATINGS.length; i++) {
            if (GlobalClass.gicuCurrentUser.iMaturityLevel >= i) {
                //Don't let the user add a tag or modify a tag to a maturity level greater than their user level, or the tag will be lost
                //  to them unless their user rating is modified.
                String[] sESRBRating = AdapterMaturityRatings.MATURITY_RATINGS[i];
                alsItems.add(sESRBRating);
            }
        }
        return alsItems;
    }


    public AdapterMaturityRatings(@NonNull Context context, int resource, @NonNull List<String[]> objects ) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    private View initView(int position, View convertView,
                          ViewGroup parent)
    {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview_item_maturity_rating, parent, false);
        }

        String[] sItemData = getItem(position);

        //todo: don't preset restrictions greater than that of the user or the won't be able to see the item they just created.

        TextView textView_AgeRatingCode = convertView.findViewById(R.id.textView_AgeRatingCode);
        if(sItemData != null) {
            textView_AgeRatingCode.setText(sItemData[MATURITY_RATING_CODE_INDEX]);
        }

        TextView textView_AgeRatingDescription = convertView.findViewById(R.id.textView_AgeRatingDescription);
        if(sItemData != null) {
            String sTemp = sItemData[MATURITY_RATING_NAME_INDEX] + " - " + sItemData[MATURITY_RATING_DESCRIPTION_INDEX];
            textView_AgeRatingDescription.setText(sTemp);
        }

        return convertView;
    }


}
