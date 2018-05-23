package com.kibisoftware.flickrgallery.datarequests;

import com.kibisoftware.flickrgallery.Interfaces.Observable;
import com.kibisoftware.flickrgallery.Interfaces.Observer;
import com.kibisoftware.flickrgallery.data.FlickrData;
import com.kibisoftware.flickrgallery.data.Photo;

import android.os.AsyncTask;


public class GetPhotos extends AsyncTask<FlickrData, Void, String> implements Observable{

    private Observer observer;
    private String notifyString;

    public GetPhotos() {
    }

    @Override
    protected String doInBackground(FlickrData... flickrData) {
        FlickrData object = flickrData[0];
        if (object != null && object.photosResult != null
                && object.photosResult.photosList != null
                && object.photosResult.photosList.size() > 0) {
            for (Photo photos : object.photosResult.photosList) {

                notifyString = "https://farm" + photos.farm
                        + ".staticflickr.com/" + photos.server + "/"
                        + photos.id + "_" + photos.secret + "_" + "q" + ".jpg";
                notifyObservers();
            }
        }
        return null;
    }

    @Override
    public void registerObserver(Observer observer) {
        this.observer = observer;
    }

    @Override
    public void notifyObservers() {
        this.observer.gotInfo(notifyString);
    }
}
