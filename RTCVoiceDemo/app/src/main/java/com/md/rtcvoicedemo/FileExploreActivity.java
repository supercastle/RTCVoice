package com.md.rtcvoicedemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FileExploreActivity extends AppCompatActivity {


    public final static String file_path = "file_path";

    private RecyclerView rvFiles;
    private FileAdapter adapter;


    public static void start(Activity activity, int code){
        Intent intent = new Intent(activity, FileExploreActivity.class);
        activity.startActivityForResult(intent, code);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explore);

        rvFiles = (RecyclerView) findViewById(R.id.rv_files);
        rvFiles.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false ));
        rvFiles.addItemDecoration(new ColorDecoration(Color.BLACK, 0 , 10 , true));

        adapter = new FileAdapter();
        rvFiles.setAdapter(adapter);


        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q){
            ContentResolver contentResolver = getContentResolver();
            Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    FileExploreActivity.Music music = new FileExploreActivity.Music();
                    music.id =  cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                    music.title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    music.uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,music.id);
                    music.path = music.uri.toString();
                    adapter.addData(music);
                }while (cursor.moveToNext());

                adapter.notifyDataSetChanged();

            }
            if(cursor != null){
                cursor.close();
            }
        }else {

            File dir = Environment.getExternalStorageDirectory();
            List<File> files = new ArrayList<>(Arrays.asList(dir.listFiles()));
            Iterator<File> iterator = files.iterator();
            while (iterator.hasNext()) {
                File next = iterator.next();
                int index = next.getName().lastIndexOf(".");
                if (index == -1) {
                    continue;
                }
                String suffix = next.getName().substring(index);
                if (".MP3".equals(suffix.toUpperCase())) {
                    FileExploreActivity.Music music = new FileExploreActivity.Music();
                    music.title = next.getName().substring(0,next.getName().lastIndexOf("."));
                    music.path = next.getAbsolutePath();
                    adapter.addData(music);
                }

            }
            adapter.notifyDataSetChanged();
        }


    }



    public static class Music {

        public String title;
        public String artist;
        public long id;
        public String path;
        public Uri uri;
        public String duration;



    }


    class FileAdapter extends BaseQuickAdapter<Music, BaseViewHolder>{

        public FileAdapter() {
            super(R.layout.item_file);
        }

        @Override
        protected void convert(@NotNull BaseViewHolder baseViewHolder, final Music music) {
            TextView tvName = baseViewHolder.findView(R.id.tv_file_name);
            tvName.setText(music.id + "\n"
                    + music.title + "\n"
                    + music.path);
            tvName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.putExtra(file_path, music.path);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            });

        }



    }




}