package com.kibisoftware.flickrgallery.data;

import com.google.gson.annotations.SerializedName;

public class FlickrData {
    @SerializedName("photos")
    public Photos photosResult;
    @SerializedName("stat")
    public String stat;
}
