package com.kibisoftware.flickrgallery.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kibisoftware.flickrgallery.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

public class ImageDetailActivity extends AppCompatActivity {

    private String id;
    private String farm, server, secret, title;
    private ImageView imageView;
    private TextView imageTitle;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            farm = savedInstanceState.getString("farm");
            id = savedInstanceState.getString("id");
            server = savedInstanceState.getString("server");
            secret = savedInstanceState.getString("secret");
            title = savedInstanceState.getString("title");
        }

        setContentView(R.layout.full_image);

        imageView = findViewById(R.id.imageView);
        imageTitle = findViewById(R.id.imageTitle);
        progress = findViewById(R.id.progressBar);

        imageTitle.setText(title);

        Bundle arguments = null;
        if (getIntent().getExtras() != null) {
            arguments = getIntent().getExtras().getBundle("key");
        }
        if (arguments != null) {
            farm = arguments.getString("farm");
            id = arguments.getString("id");
            server = arguments.getString("server");
            secret = arguments.getString("secret");
            title = arguments.getString("title");

            String url = "https://farm" + farm
                    + ".staticflickr.com/" + server + "/"
                    + id + "_" + secret + "_" + "b" + ".jpg";

            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .showImageOnLoading(R.drawable.ic_stub)
                    .showImageForEmptyUri(R.drawable.ic_empty)
                    .showImageOnFail(R.drawable.ic_error)
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .considerExifParams(true)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .build();

            ImageLoader.getInstance()
                    .displayImage(url, imageView, options, new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                            progress.setProgress(0);
                            progress.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            progress.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            progress.setVisibility(View.GONE);
                        }
                    }, new ImageLoadingProgressListener() {
                        @Override
                        public void onProgressUpdate(String imageUri, View view, int current, int total) {
                            progress.setProgress(Math.round(100.0f * current / total));
                        }
                    });


        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) {
            outState.putString("farm", farm);
            outState.putString("id", id);
            outState.putString("server", server);
            outState.putString("secret", secret);
            outState.putString("title", title);
        }
    }
}
