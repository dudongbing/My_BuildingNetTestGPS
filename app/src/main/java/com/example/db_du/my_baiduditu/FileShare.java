package com.example.db_du.my_baiduditu;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.os.Environment.getExternalStorageDirectory;

public class FileShare extends AppCompatActivity {

    // The path to the root of sdcard
    private File mSdcardDir;
    // The path to the "Downloads" subdirectory
    private File mDownloadDir;
    // Array of files in the Download subdirectory
    File[] mDownloadFiles;
    // Array of filenames corresponding to mDownloadFiles
    List<String> mDownloadFilenames=new ArrayList<>();
    // ListView 显示文件列表
    ListView mFileListView;
    //被选择上传文件的uri
    Uri fileUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_share);

        try {
            //get the sdcard path
            mSdcardDir = getExternalStorageDirectory();
            // Get the Download subdirectory;
            mDownloadDir = new File(mSdcardDir, "Download");
            // Get the files in the Download subdirectory
            mDownloadFiles = mDownloadDir.listFiles();
            if (mDownloadFiles==null) {//if no files find, finish activity
                Toast.makeText(this,"没有数据文件",Toast.LENGTH_SHORT).show();
                finish();
            }

            //ListView
            mFileListView = (ListView) findViewById(R.id.mfilelistview);
        /*
         * Display the file names in the ListView mFileListView.
         * Back the ListView with the array mDownloadFilenames, which
         * you can create by iterating through mDownloadFiles and
         * calling File.getAbsolutePath() for each File
         */

            mDownloadFilenames.clear();//清空文件名数组

            for(File  mCurrentFile:mDownloadFiles){//获得所有文件名称
                mDownloadFilenames.add(mCurrentFile.getName());
            }

            ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, mDownloadFilenames);
            mFileListView.setAdapter(mAdapter);//ListView显示Download文件夹下所有文件
        }catch (Exception  e){
            Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show();

        }

        // Define a listener that responds to clicks on a file in the ListView
        mFileListView.setOnItemClickListener(
            new AdapterView.OnItemClickListener(){
                @Override
                /*
                 * When a filename in the ListView is clicked, get its
                 * content URI and send it to the requesting app
                 */
                public void onItemClick(AdapterView<?> adapterView,
                                            View view,
                                            int position,
                                            long rowId) {
                    /*
                     * Get a File for the selected file name.
                     * Assume that the file names are in the
                     * mImageFilename array.
                     */
                    File requestFile = mDownloadFiles[position];
                    if(!requestFile.isFile())//如果不是一个文件，返回。
                        return;
                    /*
                     * Most file-related method calls need to be in
                     * try-catch blocks.
                     */

                    // Use the FileProvider to get a content URI
                    try {
                        fileUri = FileProvider.getUriForFile(
                                FileShare.this,
                                "com.example.db_du.my_baiduditu.fileprovider",
                                requestFile);
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        shareIntent.setType("application/octet-stream");
                        startActivity(Intent.createChooser(shareIntent, "上传测试数据"));

                    } catch (IllegalArgumentException e) {
                        Log.e("File Selector",
                                "The selected file can't be shared ");
                    }
                }
            }
        );
    }
}
