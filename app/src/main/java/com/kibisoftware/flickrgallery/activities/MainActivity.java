package com.kibisoftware.flickrgallery.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.google.gson.Gson;
import com.kibisoftware.flickrgallery.Interfaces.Observer;
import com.kibisoftware.flickrgallery.R;
import com.kibisoftware.flickrgallery.data.FlickrData;
import com.kibisoftware.flickrgallery.data.Photo;
import com.kibisoftware.flickrgallery.datarequests.GetPhotos;
import com.kibisoftware.flickrgallery.recyclers.EndlessRecyclerViewScrollListener;
import com.kibisoftware.flickrgallery.recyclers.MainGridListAdapter;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements Observer {

    public static final String recentCache = Environment.getExternalStorageDirectory().getPath()
                                                        + "/flickrgallery/feed/";

    // adding &nojsoncallback=1 means we take away the "jsonFlickrApi(" prefix
    private static final String recent_url = "https://api.flickr.com/services/rest/?method=flickr.photos.getRecent&extras=url_s&api_key=c84cdde1e14e047f3a40f41c3eefcc1d&format=json&nojsoncallback=1&page=";

    private RecyclerView gridView;
    private ProgressBar progressBar;
    private GridLayoutManager layoutManager;
    private MainGridListAdapter adapter;
    private FlickrData result;
    private GetPhotos getPhotos;
    private static String currentUrl;

    private static final int REQUEST_PERMISSIONS = 123;

    private ArrayList<String> filePaths = new ArrayList<>();
    private int currentPage = 1;

    private EndlessRecyclerViewScrollListener scrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initImageDownlaoder();

        if (currentUrl == null) { //clean run
            clearSavedFiles();
            currentUrl = recent_url + currentPage;
        }

        gridView = findViewById(R.id.gridView);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        int displayWidth = getResources().getDisplayMetrics().widthPixels;

        int imageWidth = displayWidth / 3;
        //gridView.setColumnWidth(imageWidth);
        //gridView.setStretchMode(GridView.NO_STRETCH);
        adapter = new MainGridListAdapter(this);
        gridView.setAdapter(adapter);
        layoutManager = new GridLayoutManager(this, 3);
        gridView.setLayoutManager(layoutManager);
        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                loadNextData(page);
            }
        };

        gridView.addOnScrollListener(scrollListener);

        // We're going to be looking through the phone to find all the pictures, so we
        // need permission to read & write storage outside the app
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
             ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                AlertDialog dialog = builder.setMessage(getString(R.string.permissions_message)).
                        setCancelable(false).
                        setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        REQUEST_PERMISSIONS);
                                dialog.cancel();
                            }
                        }).
                        create();
                dialog.show();

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_PERMISSIONS);
            }
        } else {
            // already have permission
            finishSetup();
        }

    }

    private void initImageDownlaoder() {
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(this);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        config.writeDebugLogs(); // Remove for release app

        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());
    }

    private void loadNextData(int page) {
        currentPage = page;
        currentUrl = recent_url + currentPage;
        new DownloadListFromURL().execute(currentUrl);
    }

    private void clearSavedFiles() {
        File dir = new File(recentCache);
        if (dir.exists())
            for (File file : dir.listFiles()) {
                file.delete();
            }
    }

    private void finishSetup() {
        new DownloadListFromURL().execute(currentUrl);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        finishSetup();
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        AlertDialog dialog = builder.setMessage(getString(R.string.permission_fail)).
                                setCancelable(false).
                                setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        MainActivity.this.finish();
                                        dialog.cancel();
                                    }
                                }).
                                create();
                        dialog.show();
                    }
                }
            }
        }
    }

    @Override
    public void gotInfo(String string) {
        synchronized (filePaths) {
            filePaths.add(string);
        }

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                synchronized (filePaths) {
                    adapter.setList(filePaths);
                }
            }
        });
    }

//    public FlickrData getResult() {
//        return result;
//    }

    /**
     * Background Async Task to download data
     * */
    class DownloadListFromURL extends AsyncTask<String, String, String> {

        public DownloadListFromURL() {
        }
        /**
         * Before starting background thread Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... urls) {
            int count;
            try {
                URL url = new URL(urls[0]);
                // download the data
                InputStream input = url.openStream();

                DataInputStream dis = new DataInputStream(input);

                OutputStream output = new ByteArrayOutputStream();

                int writeLength;
                byte data[] = new byte[1024];
                while ((writeLength = dis.read(data)) > 0) {
                    output.write(data, 0, writeLength);
                }


                String jsonString = output.toString();

                Gson gson = new Gson();
                result = gson.fromJson(jsonString, FlickrData.class);
                getPhotos = new GetPhotos();
                getPhotos.registerObserver(MainActivity.this);
                getPhotos.execute(result);

//                if (result != null && result.photosResult != null
//                        && result.photosResult.photosList != null
//                        && result.photosResult.photosList.size() > 0) {
//                    for (Photo photos : result.photosResult.photosList) {
//
//                        notifyString = "https://farm" + photos.farm
//                                + ".staticflickr.com/" + photos.server + "/"
//                                + photos.id + "_" + photos.secret + "_" + "q" + ".jpg";
//                        notifyObservers();
//                    }
//                }

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            progressBar.setVisibility(View.GONE);

        }

    }

}