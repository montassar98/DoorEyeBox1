package com.mag_solutions.dooreyebox1.Model;

import androidx.annotation.NonNull;

import java.util.Date;

public class Motion extends EventHistory {


    public Motion(){
        super();
    }

    public Motion(int id, Date time, String visitorImage)
    {
        super(id,time, "Motion", null , visitorImage);
    }
    public Motion(int id, Date time)
    {
        super(id,time, "Motion", null , null);
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString();
    }
}
