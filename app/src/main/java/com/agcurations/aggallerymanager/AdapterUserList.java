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

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;

public class AdapterUserList extends ArrayAdapter<ItemClass_User> {

    public boolean gbSimplifiedView = false;
    public boolean gbCompactMode = false;
    int[] giSelectedUnselectedBGColors = null;

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
            int iStyle = R.layout.listview_useritem;
            if(gbCompactMode) iStyle = R.layout.listview_useritem_compact;
            row = inflater.inflate(iStyle, parent, false);
        }

        //Get user data for this row:
        final ItemClass_User icu = getItem(position);

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
        if(!gbSimplifiedView) {
            String sMaturityCode = AdapterTagMaturityRatings.TAG_AGE_RATINGS[icu.iMaturityLevel][AdapterTagMaturityRatings.TAG_AGE_RATING_CODE_INDEX];
            sMaturityCode = "(" + sMaturityCode + ")";
            textView_Maturity.setText(sMaturityCode);
        } else {
            textView_Maturity.setVisibility(View.INVISIBLE);
        }

        //Set text to indicate user is an admin (text does not change, only the visibility):
        TextView textView_Admin = row.findViewById(R.id.textView_Admin);
        if(icu.bAdmin && !gbSimplifiedView){
            textView_Admin.setVisibility(View.VISIBLE);
        } else {
            textView_Admin.setVisibility(View.INVISIBLE);
        }

        if(giSelectedUnselectedBGColors != null) {
            if (icu.bIsChecked) {
                row.setBackgroundColor(giSelectedUnselectedBGColors[0]);
            } else {
                row.setBackgroundColor(giSelectedUnselectedBGColors[1]);
            }
        }

        //return super.getView(position, convertView, parent);
        return row;
    }

    public ArrayList<ItemClass_User> GetSelectedUsers(){

        ArrayList<ItemClass_User> alicu = new ArrayList<>();
        for(int i = 0; i < getCount(); i++){
            ItemClass_User icu = getItem(i);
            if(icu != null) {
                if (icu.bIsChecked) {
                    alicu.add(icu);
                }
            }
        }
        return alicu;

    }

    public void RemoveUsersFromList(ArrayList<ItemClass_User> alicu){
        for(ItemClass_User icuToRemove: alicu){
            int i;
            for(i = 0; i < getCount(); i++){
                ItemClass_User icu = getItem(i);
                if(icu != null) {
                    if (icu.sUserName.equals(icuToRemove.sUserName)) {
                        remove(icu);
                        break;
                    }
                }
            }

        }
        //notifyDataSetChanged(); //todo: is this necessary?

    }

    public void AddUsers(ArrayList<ItemClass_User> alicu){

        for(ItemClass_User icu: alicu){
            add(icu);
        }
        //notifyDataSetChanged(); //todo: is this necessary?

    }




}
