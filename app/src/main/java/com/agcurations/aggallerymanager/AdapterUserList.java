package com.agcurations.aggallerymanager;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;

public class AdapterUserList extends ArrayAdapter<ItemClass_User> {



    public AdapterUserList(@NonNull Context context, int resource, @NonNull List<ItemClass_User> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            //My custom list item design is here
            row = inflater.inflate(R.layout.listview_useritem, parent, false);
        }

        //Get user data for this row:
        ItemClass_User icu = getItem(position);

        //Set the icon color:
        AppCompatImageView imageView_UserIcon = row.findViewById(R.id.imageView_UserIcon);
        Drawable drawable = AppCompatResources.getDrawable(getContext(), R.drawable.login).mutate();
        drawable.setColorFilter(new PorterDuffColorFilter(icu.iUserIconColor, PorterDuff.Mode.SRC_IN));
        imageView_UserIcon.setImageDrawable(drawable);

        //Set the user name:
        TextView textView_UserName = row.findViewById(R.id.textView_UserName);
        textView_UserName.setText(icu.sUserName);

        //Set the maturity rating code text for the readout:
        TextView textView_Maturity = row.findViewById(R.id.textView_Maturity);
        String sMaturityCode = AdapterTagMaturityRatings.TAG_AGE_RATINGS[icu.iMaturityLevel][AdapterTagMaturityRatings.TAG_AGE_RATING_CODE_INDEX];
        sMaturityCode = "(" + sMaturityCode + ")";
        textView_Maturity.setText(sMaturityCode);

        //Set text to indicate user is an admin (text does not change, only the visibility):
        TextView textView_Admin = row.findViewById(R.id.textView_Admin);
        if(icu.bAdmin){
            textView_Admin.setVisibility(View.VISIBLE);
        } else {
            textView_Admin.setVisibility(View.INVISIBLE);
        }

        //return super.getView(position, convertView, parent);
        return row;
    }
}
