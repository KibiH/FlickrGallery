package com.kibisoftware.flickrgallery.activities;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;

import com.google.gson.Gson;
import com.kibisoftware.flickrgallery.Interfaces.Observer;
import com.kibisoftware.flickrgallery.R;
import com.kibisoftware.flickrgallery.data.FlickrData;
import com.kibisoftware.flickrgallery.data.Photo;
import com.kibisoftware.flickrgallery.recyclers.EndlessRecyclerViewScrollListener;
import com.kibisoftware.flickrgallery.recyclers.MainGridListAdapter;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements Observer {

    // adding &nojsoncallback=1 means we take away the "jsonFlickrApi(" prefix
    private static final String recent_url = "https://api.flickr.com/services/rest/?method=flickr.photos.getRecent&extras=url_s&api_key=c84cdde1e14e047f3a40f41c3eefcc1d&format=json&nojsoncallback=1";

    private static final String search_url = "https://api.flickr.com/services/rest/?method=flickr.photos.search&extras=url_s&api_key=c84cdde1e14e047f3a40f41c3eefcc1d&format=json&nojsoncallback=1&text=";

    private RecyclerView gridView;
    private ProgressBar progressBar;
    private GridLayoutManager layoutManager;
    private MainGridListAdapter adapter;
    private FlickrData result;
    private static String currentUrl;

    private static final int REQUEST_PERMISSIONS = 123;

    private ArrayList<Photo> thePhotos = new ArrayList<>();
    private int currentPage = 1;

    private EndlessRecyclerViewScrollListener scrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (currentUrl == null) { //clean run
            currentUrl = recent_url;
        }

        handleIntent(getIntent());
        initImageDownlaoder();

        gridView = findViewById(R.id.gridView);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        int displayWidth = getResources().getDisplayMetrics().widthPixels;

        int imageWidth = displayWidth / 3;
        adapter = new MainGridListAdapter(this, gridView);
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
        String urlToUse = currentUrl + "&page=" + currentPage;
        new DownloadListFromURL().execute(urlToUse);
    }

    private void finishSetup() {
        String urlToUse = currentUrl + "&page=" + currentPage;
        new DownloadListFromURL().execute(urlToUse);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.optionsmenu, menu);

        MenuItem search = menu.findItem(R.id.search);
        search.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // we collapsed the menu, go back to regular list
                currentUrl = recent_url;

                currentPage = 1;
                thePhotos.clear();
                adapter.notifyDataSetChanged();
                scrollListener.resetState();
                String urlToUse = currentUrl + "&page=" + currentPage;
                new DownloadListFromURL().execute(urlToUse);
                return true;
            }
        });

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));


        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //use the query to search your data somehow
            currentUrl = search_url + query;

            currentPage = 1;
            thePhotos.clear();
            adapter.notifyDataSetChanged();
            scrollListener.resetState();
            String urlToUse = currentUrl + "&page=" + currentPage;
            new DownloadListFromURL().execute(urlToUse);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                for (int result : grantResults) {
                    if (result == PackageManager.PERMISSION_GRANTED) {
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
    }

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
                if (result != null && result.photosResult != null
                        && result.photosResult.photosList != null
                        && result.photosResult.photosList.size() > 0) {
                    for (Photo photo : result.photosResult.photosList) {
                        synchronized (thePhotos) {
                            thePhotos.add(photo);
                        }

                    }
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            synchronized (thePhotos) {
                                adapter.setList(thePhotos);
                            }
                        }
                    });

                }

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
