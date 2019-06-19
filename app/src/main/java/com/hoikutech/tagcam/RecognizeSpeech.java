package com.hoikutech.tagcam;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.hoikutech.tagcam.preview.Preview;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.ArrayList;

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

        //ImageButton view = main_activity.findViewById(R.id.voice_tag);
        //view.setImageResource(R.drawable.ic_dictation_on);
        //main_activity.getPreview().showToast(null ,msg+":"+error);
    }
    public boolean isRecognizing(){ return mSpeechRecognizer!=null;}
    public void interruptRecognition() {
        if (!isRecognizing()) return;

        //setRecordingState(false,null);
        ImageButton view = main_activity.findViewById(R.id.voice_tag);
            view.setImageResource(R.drawable.ic_dictation_on);
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
    MainActivity main_activity;
    public void doRecognize(MainActivity _main_activity ) {
        if(isRecognizing()) return ;
        main_activity = _main_activity ;
        setRecordingState(true,null);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");

        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(main_activity);
        mSpeechRecognizer.setRecognitionListener(this);
        mSpeechRecognizer.startListening(intent);
    }

    private void setRecordingState(boolean bRecord,ArrayList<String> strings){
        ImageButton view = main_activity.findViewById(R.id.voice_tag);

        if( bRecord ){ // Start recording
            view.setImageResource(R.drawable.ic_dictation_red);
        } else { // Finish recognition
            view.setImageResource(R.drawable.ic_dictation_on);

            // Show image saving dialog
            String dialogTitle ;
            if( strings == null ) {
                strings = new ArrayList<String>();
                strings.add("");
                dialogTitle = main_activity.getResources().getString(R.string.voice_recognition_error);
            } else {
                dialogTitle = main_activity.getResources().getString( R.string.add_voice_tag_confirm );
            }

            // Finalize
            final ArrayList<String> candidateStrings = strings;

            class TagGenerator {
                public String getText(int idx){
                    return candidateStrings.get(idx);
                    // Considered cancel button but not used anymore
                    //return (idx+1 >= candidateStrings.size() ? "" : candidateStrings.get(idx));
                }
                public void addTagMain() {
                    String text = ""+descTextBox.getText();

                    main_activity.getPreview().mDelayedImageSaver.addTag(
                            Preview.DelayedImageSaver.TAG_TRANSCRIPT,text);
                }
                public EditText descTextBox;
            };
            final TagGenerator tagGenerator = new TagGenerator();


            // リスト項目とListViewを対応付けるArrayAdapterを用意する
            final AlertDialog.Builder alertDlgBuilder = new AlertDialog.Builder(main_activity);

            ArrayAdapter adapter = new ArrayAdapter(
                    main_activity, android.R.layout.simple_list_item_1, candidateStrings);
            alertDlgBuilder.setView(R.layout.voicetag_dialog);

            final AlertDialog mAlertDialog = alertDlgBuilder.setTitle(dialogTitle)
                    //.setMessage(dateStr)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            tagGenerator.addTagMain();
                            main_activity.mRecognizeSpeech = null ;
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            main_activity.mRecognizeSpeech = null ;
                        }
                    })
                    .show();

            ////////    Preview imageを設定する
            //ImageView previewView = (ImageView)mAlertDialog .findViewById(R.id.imageview_preview);
            //previewView.setImageBitmap(mTagCamFragment.getStoredFileBitmap());

            if( candidateStrings.size()>1 ) {
                // ListViewにArrayAdapterを設定する
                ListView listView = (ListView) mAlertDialog.findViewById(R.id.text_candidates_list);
                listView.setAdapter(adapter);

                // List item click
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        final String curName = tagGenerator.descTextBox.getText() + "";
                        final String newName = tagGenerator.getText(position);
                        if (curName.equals(newName)) { // Double tap to save and close
                            tagGenerator.addTagMain();
                            mAlertDialog.dismiss();
                            main_activity.mRecognizeSpeech = null ;
                        } else
                            tagGenerator.descTextBox.setText(newName);
                    }
                });
            }
            tagGenerator.descTextBox = (EditText)mAlertDialog.findViewById(R.id.description_text);
            tagGenerator.descTextBox.setText(tagGenerator.getText(0));
        }
    }

    private MediaRecorder mMediaRecorder;
    private String recordSoundPath;
    public void StartRecordSound(MainActivity _main_activity){
        this.main_activity = _main_activity;
        // 出力するファイルパスを指定
        try {
            recordSoundPath =
                File.createTempFile("voiceMemo", ".wav", main_activity.getCacheDir())
                .getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            recordSoundPath = null ;
            return ; // Cannot create temporary file
        }

        if( isRecordingSound() ){
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            //mMediaRecorder = null ;
        }

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        //mMediaRecorder.setAudioSamplingRate(44100);
        //mMediaRecorder.setAudioEncodingBitRate(160000);



        mMediaRecorder.setOutputFile(recordSoundPath);

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        }
        catch (final Exception e) {

        }

    }
    public String StopRecordSound(){
        try {
            if( mMediaRecorder == null || recordSoundPath == null ) return null ;
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder = null ;

            byte[] waveBytes = getFileByteArray(recordSoundPath) ;
            if( waveBytes == null ) return null ;

            byte[] encodeBytes = Base64.encode(waveBytes, Base64.DEFAULT);
            if(encodeBytes == null) return null ;

            return new String(encodeBytes, "UTF-8");
        } catch (/*UnsupportedEncoding*/ Exception e) {
            e.printStackTrace();
        }

        return null ;
    }

    public boolean isRecordingSound(){return mMediaRecorder!=null;}

    public byte[] getFileByteArray(String path) {
        File file = new File(path);
        try (FileInputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream bout = new ByteArrayOutputStream();) {
            byte[] buffer = new byte[1024];
            int len = 0;
            while((len = inputStream.read(buffer)) != -1) {
                bout.write(buffer, 0, len);
            }
            return bout.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
        /*
    class WaveRecorder {
        private AudioRecord record;

        // 定数
        private final int AUDIO_SAMPLE_FREQ = 44100;
        private final int AUDIO_BUFFER_SIZE = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_FREQ, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        private final int FRAME_BUFFER_SIZE = AUDIO_BUFFER_SIZE / 2 ;

        private short data[] = new short[FRAME_BUFFER_SIZE];

        public void start() {
            // AudioRecord作成
            record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    AUDIO_SAMPLE_FREQ, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, AUDIO_BUFFER_SIZE);
            record.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
                // フレームごとの処理
                @Override
                public void onPeriodicNotification(AudioRecord recorder) {
                    recorder.read(data, 0, FRAME_BUFFER_SIZE);
                }

                @Override
                public void onMarkerReached(AudioRecord recorder) {
                }
            });

            record.setPositionNotificationPeriod(FRAME_BUFFER_SIZE);

            // 録音開始
            record.startRecording();

            record.read(data, 0, FRAME_BUFFER_SIZE);
        }

        public void stop() {
            record.stop();
            record.release();
            record = null;
        }
    }
        */
}
