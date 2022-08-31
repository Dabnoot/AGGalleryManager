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

public class adapterTagAgeRatings extends ArrayAdapter<String[]> {

    //Adapted from ESRB Ratings
    public final static int TAG_AGE_RATING_EC = 0;
    public final static int TAG_AGE_RATING_E = 1;
    public final static int TAG_AGE_RATING_E10 = 2;
    public final static int TAG_AGE_RATING_T = 3;
    public final static int TAG_AGE_RATING_M = 4;
    public final static int TAG_AGE_RATING_AO = 5;
    public final static int TAG_AGE_RATING_RP = 6;
    public final static int TAG_AGE_RATING_IB = 7;
    public final static int TAG_AGE_RATING_HE = 8;
    public final static int TAG_AGE_RATING_UR = 9;

    public final static int TAG_AGE_RATING_CODE_INDEX = 0;
    public final static int TAG_AGE_RATING_DESCRIPTION_INDEX = 1;
    public final static String[][] TAG_AGE_RATINGS = {
            {"EC",  "Early Childhood - Suitable for children aged 3 or older; there will be no inappropriate content. E.g., Dora the Explorer, Dragon Tales."},
            {"E",   "Everyone - Suitable for all age groups. The game should not contain any sounds or images likely to scare young children. No bad language should be used. E.g., Just Dance, FIFA.",},
            {"E10+","Everyone 10 and Older - Suitable for those aged 10 or above. There could be mild forms of violence, and some scenes might be frightening for children. E.g., Minecraft Dungeons, Plants vs Zombies.",},
            {"T",	"Teen - Suitable for those aged 13 or above. The game could feature more realistic and graphic scenes of violence. E.g., Fortnite, Sims 4.",},
            {"M",	"Mature - Suitable for those aged 17 or above. This rating is used when the violence becomes realistic and would be expected in real life. Bad language, and the use of tobacco, alcohol, or illegal drugs can also be present. E.g., Ark: Survival Evolved, Destiny 2.",},
            {"AO",	"Adults Only - Suitable for adults aged 18 or above. The adult classification is used when there are extreme levels of violence and motiveless killing. Glamorization of drugs, gambling, and sexual activity can also be featured. E.g., Grand Theft Auto V, Fallout 4.",},
            {"RP",  "Rating Pending - Titles with the RP rating have not yet been assigned a final ESRB rating."},
            {"IB",  "Implicit Bias - Tag associated with items eschewed by mainstream society, may vary by country, religion, or culture. Implicit Bias is defined as negative associations expressed automatically. May include L.G.B.T.Q.I.A topics in socially-repressive countries. Includes some N.S.F.W. content."},
            {"HE",  "Highly Eschewed - Tag associated with items highly eschewed by mainstream society, such as p.o.r.n, or some topics in certain countries, religions, or cultures. All content should be considered N.S.F.W.."},
            {"UR",  "User Restricted - Items only available for viewing by the assigned user(s)."}
    };


    public adapterTagAgeRatings(@NonNull Context context, int resource, @NonNull List<String[]> objects ) {
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
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.spinner_item_age_rating, parent, false);
        }

        TextView textView_AgeRatingCode = convertView.findViewById(R.id.textView_AgeRatingCode);
        textView_AgeRatingCode.setText(TAG_AGE_RATINGS[position][TAG_AGE_RATING_CODE_INDEX]);

        TextView textView_AgeRatingDescription = convertView.findViewById(R.id.textView_AgeRatingDescription);
        textView_AgeRatingDescription.setText(TAG_AGE_RATINGS[position][TAG_AGE_RATING_DESCRIPTION_INDEX]);

        return convertView;
    }


}
