package com.mag_solutions.dooreyebox1.Model;


import java.util.Date;

public class Ring extends EventHistory {

    private String visitorImage;
    private int id;
    private Date time;
    private String status;
    private String responder;
    public Ring(){
        super();
    }

    public Ring(int id, Date time, String responder, String visitorImage)
    {
        super(id,time, "Ring",responder, visitorImage);

    }
    public Ring(int id, Date time)
    {
        super(id,time,  "Ring", "No one", null);
    }
    public Ring(int id, Date time, String visitorImage)
    {
        super(id,time,  "Ring", "No one",visitorImage);
    }


}
