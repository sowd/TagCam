package com.hoikutech.tagcam;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.hoikutech.tagcam.preview.Preview;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    public class WaveRecorder {
        // Logic from: https://gist.github.com/ohtangza/b5da3b7247b7eaa737a9d3695d909093
        private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
        private static final int SAMPLE_RATE = 16000; //44100; // Hz
        private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
        private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;

        private final int BUFFER_SIZE =
                2 * AudioRecord.getMinBufferSize( SAMPLE_RATE, CHANNEL_MASK, ENCODING );
        private byte[] buffer = new byte[BUFFER_SIZE];

        private AudioRecord audioRecord;
        private FileOutputStream wavOut ;
        private String outFilePath ;

        public void start(MainActivity _main_activity ) {
            main_activity = _main_activity ;
            if( isRecording() ){
                stop(); // force restart recording
            }

            File file ;
            try {
                //file = new File( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) , "voiceMemo.wav");
                file = File.createTempFile("voiceMemo", ".wav", main_activity.getCacheDir());
                outFilePath = file.getAbsolutePath();

                // Create recorder
                audioRecord = new AudioRecord(
                        AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE);
                wavOut = new FileOutputStream(outFilePath);
                writeWavHeader(wavOut, CHANNEL_MASK, SAMPLE_RATE, ENCODING);

            } catch (IOException e) {
                e.printStackTrace();
                if (audioRecord != null)
                    stop();
                return;
            }

            audioRecord.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
                // フレームごとの処理
                @Override
                public void onPeriodicNotification(AudioRecord recorder) {
                    if( audioRecord == null ) return ; // Stopped.
                    int read = recorder.read(buffer, 0, buffer.length);
                    try {
                        wavOut.write(buffer, 0, read);
                    } catch( Exception e ){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMarkerReached(AudioRecord recorder) {
                }
            });

            audioRecord.setPositionNotificationPeriod(BUFFER_SIZE/2);
            audioRecord.startRecording();

            // Initial read?
            audioRecord.read(buffer, 0, buffer.length);
        }

        public String stop() {
            if( audioRecord != null ) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if( wavOut != null ) try {
                    wavOut.write(buffer, 0, read);
                } catch( Exception e ){
                    e.printStackTrace();
                }
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }

            if( wavOut != null ) {
                try {
                    wavOut.close();
                    wavOut = null;
                    updateWavHeader(new File(outFilePath));

                    // Convert to base64
                    byte[] waveBytes = getFileByteArray(outFilePath) ;
                    if( waveBytes == null ) return null ;

                    byte[] encodeBytes = Base64.encode(waveBytes, Base64.DEFAULT);
                    if(encodeBytes == null) return null ;

                    String encStr = new String(encodeBytes, "UTF-8");

                    return encStr.replaceAll("\n","").replaceAll("\r","");

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            return null;
        }

        public boolean isRecording(){ return audioRecord != null;}

        private byte[] getFileByteArray(String path) {
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

        /**
         * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
         * Two size fields are left empty/null since we do not yet know the final stream size
         *
         * @param out         The stream to write the header to
         * @param channelMask An AudioFormat.CHANNEL_* mask
         * @param sampleRate  The sample rate in hertz
         * @param encoding    An AudioFormat.ENCODING_PCM_* value
         * @throws IOException
         */
        private void writeWavHeader(OutputStream out, int channelMask, int sampleRate, int encoding) throws IOException {
            short channels;
            switch (channelMask) {
                case AudioFormat.CHANNEL_IN_MONO:
                    channels = 1;
                    break;
                case AudioFormat.CHANNEL_IN_STEREO:
                    channels = 2;
                    break;
                default:
                    throw new IllegalArgumentException("Unacceptable channel mask");
            }

            short bitDepth;
            switch (encoding) {
                case AudioFormat.ENCODING_PCM_8BIT:
                    bitDepth = 8;
                    break;
                case AudioFormat.ENCODING_PCM_16BIT:
                    bitDepth = 16;
                    break;
                case AudioFormat.ENCODING_PCM_FLOAT:
                    bitDepth = 32;
                    break;
                default:
                    throw new IllegalArgumentException("Unacceptable encoding");
            }

            writeWavHeader(out, channels, sampleRate, bitDepth);
        }

        /**
         * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
         * Two size fields are left empty/null since we do not yet know the final stream size
         *
         * @param out        The stream to write the header to
         * @param channels   The number of channels
         * @param sampleRate The sample rate in hertz
         * @param bitDepth   The bit depth
         * @throws IOException
         */
        private void writeWavHeader(OutputStream out, short channels, int sampleRate, short bitDepth) throws IOException {
            // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
            byte[] littleBytes = ByteBuffer
                    .allocate(14)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putShort(channels)
                    .putInt(sampleRate)
                    .putInt(sampleRate * channels * (bitDepth / 8))
                    .putShort((short) (channels * (bitDepth / 8)))
                    .putShort(bitDepth)
                    .array();

            // Not necessarily the best, but it's very easy to visualize this way
            out.write(new byte[]{
                    // RIFF header
                    'R', 'I', 'F', 'F', // ChunkID
                    0, 0, 0, 0, // ChunkSize (must be updated later)
                    'W', 'A', 'V', 'E', // Format
                    // fmt subchunk
                    'f', 'm', 't', ' ', // Subchunk1ID
                    16, 0, 0, 0, // Subchunk1Size
                    1, 0, // AudioFormat
                    littleBytes[0], littleBytes[1], // NumChannels
                    littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                    littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
                    littleBytes[10], littleBytes[11], // BlockAlign
                    littleBytes[12], littleBytes[13], // BitsPerSample
                    // data subchunk
                    'd', 'a', 't', 'a', // Subchunk2ID
                    0, 0, 0, 0, // Subchunk2Size (must be updated later)
            });
        }

        /**
         * Updates the given wav file's header to include the final chunk sizes
         *
         * @param wav The wav file to update
         * @throws IOException
         */
        private void updateWavHeader(File wav) throws IOException {
            byte[] sizes = ByteBuffer
                    .allocate(8)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    // There are probably a bunch of different/better ways to calculate
                    // these two given your circumstances. Cast should be safe since if the WAV is
                    // > 4 GB we've already made a terrible mistake.
                    .putInt((int) (wav.length() - 8)) // ChunkSize
                    .putInt((int) (wav.length() - 44)) // Subchunk2Size
                    .array();

            RandomAccessFile accessWave = null;
            //noinspection CaughtExceptionImmediatelyRethrown
            try {
                accessWave = new RandomAccessFile(wav, "rw");
                // ChunkSize
                accessWave.seek(4);
                accessWave.write(sizes, 0, 4);

                // Subchunk2Size
                accessWave.seek(40);
                accessWave.write(sizes, 4, 4);
            } catch (IOException ex) {
                // Rethrow but we still close accessWave in our finally
                throw ex;
            } finally {
                if (accessWave != null) {
                    try {
                        accessWave.close();
                    } catch (IOException ex) {
                        //
                    }
                }
            }
        }
    }

    public WaveRecorder mWaveRecorder = new WaveRecorder();
}
