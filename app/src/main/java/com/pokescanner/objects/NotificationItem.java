
package com.pokescanner.objects;

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
public class NotificationItem extends RealmObject{
    @PrimaryKey
    int Number;
    String Name;
    boolean filtered;

    public NotificationItem() {}

    public NotificationItem(int number) {
        setNumber(number);
    }
}
