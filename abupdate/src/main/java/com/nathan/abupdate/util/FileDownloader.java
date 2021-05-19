/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nathan.abupdate.util;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Downloads chunk of a file from given url using {@code offset} and {@code size},
 * and saves to a given location.
 *
 * In a real-life application this helper class should download from HTTP Server,
 * but in this sample app it will only download from a local file.
 */
public final class FileDownloader {

    private String mUrl;
    private long mOffset;
    private long mSize;
    private File mDestination;

    public FileDownloader(String url, long offset, long size, File destination) {
        this.mUrl = url;
        this.mOffset = offset;
        this.mSize = size;
        this.mDestination = destination;
    }

    /**
     * Downloads the file with given offset and size.
     * @throws IOException when can't download the file
     */
    public void download() throws IOException {
        Log.d("FileDownloader", "downloading " + mDestination.getName()
                + " from " + mUrl
                + " to " + mDestination.getAbsolutePath());

        URL url = new URL(mUrl);
        URLConnection connection = url.openConnection();
        connection.connect();

        // download the file
        Log.d("FileDownloader", "download: step1");
        try (InputStream input = connection.getInputStream()) {
            Log.d("FileDownloader", "download: step2");
            try (OutputStream output = new FileOutputStream(mDestination)) {
                Log.d("FileDownloader", "download: step3");

                Log.d("FileDownloader", "download: step44444");
                long skipped = input.skip(mOffset);
                Log.d("FileDownloader", "download: step4");
                if (skipped != mOffset) {
                    throw new IOException("Can't download file "
                            + mUrl
                            + " with given offset "
                            + mOffset);
                }
                Log.d("FileDownloader", "download: step5");
                byte[] data = new byte[4096];
                long total = 0;
                while (total < mSize) {
                    int needToRead = (int) Math.min(4096, mSize - total);
                    int count = input.read(data, 0, needToRead);
                    if (count <= 0) {
                        break;
                    }
                    output.write(data, 0, count);
                    total += count;
                }
                Log.d("FileDownloader", "download: step6");
                if (total != mSize) {
                    throw new IOException("Can't download file "
                            + mUrl
                            + " with given size "
                            + mSize);
                }
                Log.d("FileDownloader", "download: step7");
            }
            Log.d("FileDownloader", "download: step8");
        }
        Log.d("FileDownloader", "download: step end");
    }


    public void downloadFile() throws IOException {
        Log.d("FileDownloader", "downloading " + mDestination.getName()
                + " from " + mUrl
                + " to " + mDestination.getAbsolutePath());

        URL url = new URL(mUrl);
        URLConnection connection = url.openConnection();
        connection.connect();

        // download the file
        try (InputStream input = connection.getInputStream()) {
            try (OutputStream output = new FileOutputStream(mDestination)) {
                Log.d("FileDownloader", "download: step5");
                byte[] data = new byte[1024];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }
            }
        }
    }

}
