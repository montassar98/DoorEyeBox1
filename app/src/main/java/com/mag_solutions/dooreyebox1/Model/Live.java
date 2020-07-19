package com.mag_solutions.dooreyebox1.Model;

import java.util.Date;

public class Live extends EventHistory {


    public Live(int id, Date time, String responder)
    {
        super(id,time, "Door Check", responder , null);
    }



}
