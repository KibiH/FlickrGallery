package com.kibisoftware.flickrgallery.recyclers;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.kibisoftware.flickrgallery.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;

public class MainGridListAdapter extends RecyclerView.Adapter<MainGridListAdapter.ViewHolder> {
    private ArrayList<String> photosList;
    private LayoutInflater inflater;

    public void setList(ArrayList<String> incomingPhotos) {
        if (null == photosList) {
            photosList = incomingPhotos;
            notifyDataSetChanged();
        } else {
            synchronized (photosList) {
                photosList = incomingPhotos;
                notifyDataSetChanged();
            }
        }
    }

    private DisplayImageOptions options;

    public MainGridListAdapter(Context context) {
        inflater = LayoutInflater.from(context);

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
        return (photosList != null) ? photosList.size() : 0;
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public MainGridListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = null;
        // create a new view
        v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.grid_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        if (photosList != null) {
            synchronized (photosList) {
                if (photosList.size() > position) {
                    ImageLoader.getInstance()
                            .displayImage(photosList.get(position), holder.imageView, options, new SimpleImageLoadingListener() {
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
