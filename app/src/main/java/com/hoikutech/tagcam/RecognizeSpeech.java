package com.hoikutech.tagcam;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class RecognizeSpeech implements RecognitionListener {
    final String TAG="RecognizeSpeech";

    public RecognizeSpeech(){}

    public void onReadyForSpeech(Bundle params)
    {
        Log.d(TAG, "onReadyForSpeech");
    }
    public void onBeginningOfSpeech()
    {
        Log.d(TAG, "onBeginningOfSpeech");
    }
    public void onRmsChanged(float rmsdB)
    {
        Log.d(TAG, "onRmsChanged");
    }
    public void onBufferReceived(byte[] buffer)
    {
        Log.d(TAG, "onBufferReceived");
    }
    public void onEndOfSpeech()
    {
        Log.d(TAG, "onEndofSpeech");
    }
    public void onError(int error)
    {
        Log.d(TAG,  "error " +  error);
        if( !isRecognizing() ) return ;

        setRecordingState(false,null);
        mSpeechRecognizer.destroy();
        mSpeechRecognizer = null ;
    }
    public boolean isRecognizing(){ return mSpeechRecognizer!=null;}
    public void interruptRecognition() {
        if (!isRecognizing()) return;

        setRecordingState(false,null);
        mSpeechRecognizer.destroy();
        mSpeechRecognizer = null ;
    }
    public void onResults(Bundle results)
    {
        ArrayList<String> strings = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        setRecordingState(false,strings);
        mSpeechRecognizer.destroy();
        mSpeechRecognizer = null ;
    }
    public void onPartialResults(Bundle partialResults)
    {
        Log.d(TAG, "onPartialResults");
    }
    public void onEvent(int eventType, Bundle params)
    {
        Log.d(TAG, "onEvent " + eventType);
    }

    SpeechRecognizer mSpeechRecognizer;
/*    private TagCamFragment mTagCamFragment;
    public void doRecognize(TagCamFragment tagCamFragment) {*/
    Context context;
    ImageSaver imageSaver;
    public void doRecognize(Context _context, ImageSaver _imageSaver ) {
        if(isRecognizing()) return ;
        context = _context ;
        imageSaver = _imageSaver ;
        setRecordingState(true,null);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");

        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);



        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context /*mTagCamFragment.getActivity()*/);
        mSpeechRecognizer.setRecognitionListener(this);
        mSpeechRecognizer.startListening(intent);
    }

    private void setRecordingState(boolean bRecord,ArrayList<String> strings){
        if( bRecord ){ // Start recording
            /*
            mTagCamFragment.mShutterButton.setText(R.string.shutter_button_recording);
            mTagCamFragment.mShutterButton
                    .setBackgroundResource(R.drawable.shutter_button_background_recording);
            */
        } else { // Finish recognition or interrupted

            /*
            // Change shutton button style
            mTagCamFragment.mShutterButton.setText(R.string.shutter_button_default);
            mTagCamFragment.mShutterButton
                    .setBackgroundResource(R.drawable.shutter_button_background);
            */

            // Show image saving dialog
            // Generate filePrefix
            Date date = new Date(System.currentTimeMillis());
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            String dateStr = df.format(date);

            //Context context = mTagCamFragment.getActivity();

            if( strings == null )
                strings = new ArrayList<String>();
            else
                strings.add(context.getResources().getString(R.string.save_without_voice_tag));
            final ArrayList<String> candidateStrings = strings;

            class FileNameGenerator{
                String prefix;
                public FileNameGenerator(String pref){prefix = pref;}
                public String getText(int idx){
                    return (idx+1 >= candidateStrings.size()
                            ? "" : candidateStrings.get(idx));
                }
                public void saveStoredImage() {
                    String text = ""+descTextBox.getText();

                    renameFileAndAddTag(text);
                    /*mTagCamFragment.saveStoredImage(
                            prefix + text + ".jpg" ,text);*/

                    imageSaver.start();
                }
                public EditText descTextBox;
            };
            final FileNameGenerator fng = new FileNameGenerator("photo_"+dateStr+"_1_");


            // リスト項目とListViewを対応付けるArrayAdapterを用意する
            final AlertDialog.Builder alertDlgBuilder = new AlertDialog.Builder(context);

            ArrayAdapter adapter = new ArrayAdapter(
                    context, android.R.layout.simple_list_item_1, candidateStrings);
            alertDlgBuilder.setView(R.layout.photo_save_dialog);

            final AlertDialog mAlertDialog = alertDlgBuilder.setTitle(R.string.save_with_voice_tag_confirm)
                    .setMessage(dateStr)
                    /*.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            fng.saveStoredImage();
                            //mTagCamFragment.saveStoredImage(fng.getFullFileName());
                        }
                    })*/
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            //mTagCamFragment.changeState(TagCamFragment.State.PREVIEW);
                        }
                    })
                    .show();

            ////////    Preview imageを設定する
            //ImageView previewView = (ImageView)mAlertDialog .findViewById(R.id.imageview_preview);
            //previewView.setImageBitmap(mTagCamFragment.getStoredFileBitmap());



            // ListViewにArrayAdapterを設定する
            ListView listView = (ListView) mAlertDialog .findViewById(R.id.text_candidates_list);
            listView.setAdapter(adapter);

            // List item click
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final String curName = fng.descTextBox.getText()+"";
                    final String newName = fng.getText(position);
                    if( curName.equals(newName)) { // Double tap to save and close
                        fng.saveStoredImage();
                        mAlertDialog.dismiss();
                    } else
                        fng.descTextBox.setText(newName);
                }
            });

            fng.descTextBox = (EditText)mAlertDialog.findViewById(R.id.description_text);
            fng.descTextBox.setText(fng.getText(0));

            // Click save button
            Button saveButton = (Button) mAlertDialog .findViewById(R.id.save_voice_tagged_image_btn);
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fng.saveStoredImage();
                    mAlertDialog.dismiss();
                }
            });
        }
    }

    private void renameFileAndAddTag(String text){

    }
}
