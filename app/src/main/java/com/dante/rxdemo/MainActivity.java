package com.dante.rxdemo;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.dante.rxdemo.adapter.ImageAdapter;
import com.dante.rxdemo.model.Image;
import com.dante.rxdemo.utils.Util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import id.zelory.compressor.Compressor;
import id.zelory.compressor.FileUtil;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

import static com.dante.rxdemo.App.context;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CROP_REQUEST = 2;
    private static final int REQUEST_VIEW = 3;
    @BindView(R.id.choose)
    Button choose;
    @BindView(R.id.recycler)
    RecyclerView recycler;
    @BindView(R.id.compress)
    Button compress;

    private File originalImage;
    private File image;
    private File compressdImage;
    private LinearLayoutManager layoutManager;
    private ImageAdapter adapter;
    /*
    Add new compress tool here,
    and add the method with same name(`compressImage` will find and execute it)
    */

    private String[] compressType = {"Original", "Compressor", "Luban"};


    private void load(File file) {
        String type = compressType[adapter.getData().size()];
        String size = String.format("Size: %S", Util.getReadableSize(file.length()));
        Image image = new Image(file, type, size);
        adapter.addData(image);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        int position = PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                .getInt("position", 0);
        recycler.smoothScrollToPosition(position);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initRecyclerView();

    }

    private void initRecyclerView() {
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(layoutManager);
        adapter = new ImageAdapter();
        adapter.openLoadAnimation(BaseQuickAdapter.SCALEIN);
        recycler.setAdapter(adapter);
        recycler.addOnItemTouchListener(new OnItemClickListener() {
            @Override
            public void SimpleOnItemClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
                startViewer(view.findViewById(R.id.image), i);
            }
        });
    }

    public void chooseImage(View v) {
        adapter.getData().clear();
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            if (requestCode == PICK_IMAGE_REQUEST) showToast("Pick image canceled.");

        } else if (requestCode == PICK_IMAGE_REQUEST) {
            if (data == null) {
                showToast("Read picture failed.");
            } else {
                try {
                    originalImage = FileUtil.from(this, data.getData());
                    load(originalImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                cropPhoto(data.getData());

            }
        } else if (requestCode == CROP_REQUEST) {
            Bundle extras = data.getExtras();
            Bitmap bitmap = null;
            if (extras != null) {
                Log.i(TAG, "onActivityResult: extras not null");
                bitmap = extras.getParcelable("data");
                if (bitmap != null) {
                    Log.i(TAG, "bitmap : " + bitmap.getWidth() + " * " + bitmap.getHeight());
                }
            }
            load(image);
        }
    }

    private void clearCompressed() {
//        compressedImage.setImageBitmap(null);
//        compressedSize.setText("Size : -");
//        compressedImageLB.setImageBitmap(null);
//        compressedSizeLB.setText("Size : -");
    }

//    private void cropPhoto(Uri uri) {
//        if (image == null)
//            image = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "temp" + suffix);
//        if (!image.exists()) {
//            try {
//                boolean result = image.createNewFile();
//                Log.i(TAG, "cropPhoto: created " + result);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        Intent crop = new Intent("com.android.camera.action.CROP");
//        crop.setDataAndType(uri, "image/*");
//        crop.putExtra("crop", false);
////        crop.putExtra("scale", true);
////        crop.putExtra("outputX", size);
////        crop.putExtra("outputY", size);
////        crop.putExtra("return-data", false);
//        crop.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
//        startActivityForResult(crop, CROP_REQUEST);
//    }

    public void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void compressImage(View v) {
        if (adapter.getItemCount() <= 0) {
            chooseImage(null);
        } else if (adapter.getItemCount() == compressType.length) {
            return;
        }

        for (int i = 1; i < compressType.length; i++) {
            Method compress;
            try {
                compress = this.getClass().getDeclaredMethod(compressType[i]);
                Log.i(TAG, "compressImage: " + compressType[i]);
                compress.invoke(this);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

    }

    private void Luban() {
        Luban.get(this).load(originalImage)
                .putGear(Luban.THIRD_GEAR)
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onSuccess(File file) {
                        load(file);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                }).launch();
    }

    private void Compressor() {
        Compressor.getDefault(this)
                .compressToFileAsObservable(originalImage)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<File>() {
                    @Override
                    public void call(File file) {
//                        compressdImage = Util.drawableToFile(R.drawable.mm, MainActivity.this);
                        load(file);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        showToast(throwable.getMessage());
                    }
                });
    }


    private void startViewer(View view, int position) {
        Intent intent = new Intent(context, PictureActivity.class);
        intent.putExtra("position", position);
        ArrayList<Image> data = (ArrayList<Image>) adapter.getData();
        intent.putParcelableArrayListExtra("data", data);
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(this, view, view.getTransitionName());
        ActivityCompat.startActivity(this, intent, options.toBundle());
    }


}
