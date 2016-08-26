
package com.pokescanner.objects;

import android.content.Context;

import com.pokescanner.settings.Settings;
import com.pokescanner.utils.DrawableUtils;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * Created by Brian on 7/22/2016.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"Name","filtered"}, callSuper = false)
public class FilterItem extends RealmObject{
    @PrimaryKey
    int Number;
    String Name;
    boolean filtered;

    public FilterItem() {}

    public FilterItem(int number) {
        setNumber(number);
    }

    public String getFormalName(Context context) {
        String name = getName();

        if (!Settings.get(context).isForceEnglishNames()) {
            name = context.getString(DrawableUtils.getStringID(getNumber(), context));
        }

        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}
