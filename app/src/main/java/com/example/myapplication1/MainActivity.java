package com.example.myapplication1;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication1.ml.Model;
import com.github.dhaval2404.imagepicker.ImagePicker;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    int INPUT_SIZE = 224;
    Button btn_cam, btn_gall;
    ImageView img_view;
    Button predict;
    TextView tv;
    Bitmap bitmap = null;
    private float IMAGE_MEAN = 0;
    private float IMAGE_STD = 255.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_cam = (Button) findViewById(R.id.button);
        btn_gall = (Button) findViewById(R.id.button2);
        img_view = (ImageView) findViewById(R.id.imageView);
        predict = (Button) findViewById(R.id.button3);
        tv = (TextView) findViewById(R.id.textView2);


        btn_cam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImagePicker.with(MainActivity.this)
                        .compress(256)
                        .cameraOnly()
                        .maxResultSize(512, 512)
                        .start();
            }
        });
        btn_gall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImagePicker.with(MainActivity.this)
                        .compress(256)
                        .galleryOnly()
                        .maxResultSize(512, 512)
                        .start();
            }
        });
        predict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ByteBuffer byteBuffer = bitmapToByteBuffer(scaleImage(bitmap));

                //TensorImage tbuffer = TensorImage.fromBitmap(bitmap1);
                //ByteBuffer byteBuffer = new ByteBuffer()



                ImageProcessor imageProcessor =
                        new ImageProcessor.Builder()
                                .add(new ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                                .build();

// Create a TensorImage object. This creates the tensor of the corresponding
// tensor type (uint8 in this case) that the TensorFlow Lite interpreter needs.
                TensorImage tensorImage = new TensorImage(DataType.FLOAT32);

// Analysis code for every frame
// Preprocess the image
                tensorImage.load(bitmap);
                tensorImage = imageProcessor.process(tensorImage);

                //var tb//uffer = TensorImage.fromBitmap(resized)
                ByteBuffer byteBuffer1 = tensorImage.getBuffer();

                Model model = null;
                try {
                    model = Model.newInstance(MainActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
                inputFeature0.loadBuffer(byteBuffer1);

                Model.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();


                float[] max = outputFeature0.getFloatArray();
                Log.d("clima", String.valueOf(max.length));

                ArrayList<String> list = new ArrayList<>();
                list.add("Alternaria Leaf Spot Fungal");
                list.add("Bacterial Blight");
                list.add("Healthy");
                list.add("Leaf Curl Virus");
                list.add("Thirps Insect");

                int index = getMax(max);
                tv.setText(list.get(index));

                model.close();

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK || requestCode == 250) {

            Uri uri = null;
            if (data != null) {
                uri = data.getData();


                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                img_view.setImageBitmap(bitmap);

            }

        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.Companion.getError(data), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show();
        }
    }


    public final Bitmap scaleImage(Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaledWidth = (float) 200 / (float) width;
        float scaledHeight = (float) 200 / (float) height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaledWidth, scaledHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    private final ByteBuffer bitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[this.INPUT_SIZE * this.INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        int var5 = 0;

        for (int var6 = this.INPUT_SIZE; var5 < var6; ++var5) {
            int var7 = 0;

            for (int var8 = this.INPUT_SIZE; var7 < var8; ++var7) {
                int val = intValues[pixel++];
                byteBuffer.putFloat((float) ((val >> 16 & 255) - this.IMAGE_MEAN) / this.IMAGE_STD);
                byteBuffer.putFloat((float) ((val >> 8 & 255) - this.IMAGE_MEAN) / this.IMAGE_STD);
                byteBuffer.putFloat((float) ((val & 255) - this.IMAGE_MEAN) / this.IMAGE_STD);
            }
        }


        return byteBuffer;
    }

    public final int getMax(float[] arr) {
        int ind = 0;
        float min = 0.0F;
        int i = 0;

        for (int var5 = arr.length - 1; i <= var5; ++i) {
            if (arr[i] > min) {
                min = arr[i];
                ind = i;
            }
        }

        return ind;
    }
}