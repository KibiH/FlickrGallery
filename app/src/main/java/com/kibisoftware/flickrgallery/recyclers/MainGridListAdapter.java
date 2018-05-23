package com.kibisoftware.flickrgallery.recyclers;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.kibisoftware.flickrgallery.R;
import com.kibisoftware.flickrgallery.activities.ImageDetailActivity;
import com.kibisoftware.flickrgallery.activities.MainActivity;
import com.kibisoftware.flickrgallery.data.Photo;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;

public class MainGridListAdapter extends RecyclerView.Adapter<MainGridListAdapter.ViewHolder> {
    private ArrayList<Photo> thePhotos;
    private LayoutInflater inflater;
    private RecyclerView recyclerView;
    private MainActivity activity;

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            int itemPosition = recyclerView.getChildLayoutPosition(v);
            Photo photo = thePhotos.get(itemPosition);

            Intent intent = new Intent(activity,
                    ImageDetailActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("id", photo.id);
            bundle.putString("farm", photo.farm);
            bundle.putString("server", photo.server);
            bundle.putString("secret", photo.secret);
            bundle.putString("title", photo.title);
            intent.putExtra("key", bundle);
            activity.startActivity(intent);

        }
    };

    public void setList(ArrayList<Photo> incomingPhotos) {
        if (null == thePhotos) {
            thePhotos = incomingPhotos;
            notifyDataSetChanged();
        } else {
            synchronized (thePhotos) {
                thePhotos = incomingPhotos;
                notifyDataSetChanged();
            }
        }
    }

    private DisplayImageOptions options;

    public MainGridListAdapter(MainActivity activity, RecyclerView recyclerView) {
        this.activity = activity;
        this.recyclerView = recyclerView;
        inflater = LayoutInflater.from(activity);

        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ic_stub)
                .showImageForEmptyUri(R.drawable.ic_empty)
                .showImageOnFail(R.drawable.ic_error)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }

    @Override
    public int getItemCount() {
        return (thePhotos != null) ? thePhotos.size() : 0;
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public MainGridListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.grid_item, parent, false);
        v.setOnClickListener(mOnClickListener);
        return  new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        if (thePhotos != null) {
            synchronized (thePhotos) {
                if (thePhotos.size() > position) {
                    // get the URL from the photo
                    Photo photo = thePhotos.get(position);
                    String url = "https://farm" + photo.farm
                            + ".staticflickr.com/" + photo.server + "/"
                            + photo.id + "_" + photo.secret + "_" + "q" + ".jpg";
                    ImageLoader.getInstance()
                            .displayImage(url, holder.imageView, options, new SimpleImageLoadingListener() {
                                @Override
                                public void onLoadingStarted(String imageUri, View view) {
                                    holder.progressBar.setProgress(0);
                                    holder.progressBar.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                                    holder.progressBar.setVisibility(View.GONE);
                                }

                                @Override
                                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                    holder.progressBar.setVisibility(View.GONE);
                                }
                            }, new ImageLoadingProgressListener() {
                                @Override
                                public void onProgressUpdate(String imageUri, View view, int current, int total) {
                                    holder.progressBar.setProgress(Math.round(100.0f * current / total));
                                }
                            });
                }
            }
        }
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a bitmap in this case
        public ImageView imageView;
        public ProgressBar progressBar;
        public ViewHolder(View v)   {
            super(v);
            imageView = v.findViewById(R.id.image_view);
            progressBar = v.findViewById(R.id.progress);
        }
    }


}
