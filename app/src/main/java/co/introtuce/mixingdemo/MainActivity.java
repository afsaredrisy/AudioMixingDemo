package co.introtuce.mixingdemo;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 *
 * Please provide the permissions STORAGE READ and WRITE from phone setting
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView staus;
    private Button button;
    public static final String TAG = "MUXER";
    private boolean isWorking=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.btn);
        staus = findViewById(R.id.status);
        button.setOnClickListener(this);
    }




    @Override
    public void onClick(View v) {
        if(isWorking){
            Toast.makeText(this,"Working. Please wait", Toast.LENGTH_LONG).show();
        }
        else{
            isWorking = true;
            startMuxing();
        }
    }
    private boolean helper=false;
    private void startMuxing(){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                staus.setText("Muxing......");
            }

            @Override
            protected Void doInBackground(Void... voids) {

                /*String path1= Environment.getExternalStorageDirectory().getPath() + File.separator
                        + "recorded_audio_16bits_" + String.valueOf(48000) + "Hz"
                        + ((channelCount == 1) ? "_mono" : "_stereo") + ".pcm";*/

                //Original PCM file recorded from WebRTC Stream
                String path2= Environment.getExternalStorageDirectory().getPath() + File.separator
                        + "nex" +  ".pcm";

                // MP3 file converted into PCM (Music track)
                String path3= Environment.getExternalStorageDirectory().getPath() + File.separator
                        + "track_pcm" +  ".pcm";

                //Destination file
                String dest= Environment.getExternalStorageDirectory().getPath() + File.separator
                        + "all_mixed" +  ".pcm";

                try{
                    //rawToWave(new File(path1),new File(dest));
                    helper=muxTwoPCMFiles(path2,path3,dest);
                }catch (Exception e){
                    Log.d(TAG,e.toString());
                    helper=false;
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                isWorking=false;
                if(helper){
                    staus.setText("Conversion successfully done");
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    private boolean muxThreeFiles(String f1,String f2, String f3,String dest){

        try{
            Log.d(TAG,"Started");
            InputStream fin1=null, fin2=null,fin3=null;
            OutputStream fout = null;
            File destFile = new File(dest);
            File scr1 = new File(f1);
            File scr2 = new File(f2);
            File scr3 = new File(f3);
            long size = (scr1.length()>scr2.length())?scr1.length():scr2.length();
            fin1 = new FileInputStream(scr1);
            fin2 = new FileInputStream(scr2);
            fin3 = new FileInputStream(scr3);
            fout = new FileOutputStream(destFile);
            //short arr1[] = new short[(int)size/2];
            //short arr2[] = new short[(int)size/2];
            DataInputStream dip1 = new DataInputStream(fin1);
            DataInputStream dip2 = new DataInputStream(fin2);
            DataInputStream dip3 = new DataInputStream(fin3);
            DataOutputStream dop = new DataOutputStream(fout);
            Log.d(TAG,"size is "+size);

            for(int i=0;i<size/2;i++){
                short x1=0,x2=0, x3=0;
                try{ x1 = dip1.readShort();}catch (Exception e){
                    Log.d(TAG,e.toString());
                }
                try{ x2 = dip2.readShort();}catch (Exception e){
                    Log.d(TAG,e.toString());
                }
                try{
                    x3 += dip3.readShort();
                    x3 += dip3.readShort();

                    x3=(short) (x3/4);
                }catch (Exception e){
                    Log.d(TAG,e.toString());
                }
                Log.d(TAG," Muxing "+(x1+x2));
                int m = x1+x2+x3;
                if(m>32767) m=32767;
                if(m<-32768) m=-32768;
                dop.writeShort((short)((x1+80)+(x2+80)+x3));
            }
            Log.d(TAG,"Finished");
            fout.close();
            return true;
        }catch (final Exception e){
            Log.d(TAG," "+e.toString());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    staus.setText(e.toString()+"\n");

                }
            });
            return false;
        }
    }

    private boolean muxTwoPCMFiles(String f1,String f2,String dest){

        try{
            Log.d(TAG,"Started");
            InputStream fin1=null, fin2=null,fin3=null;
            OutputStream fout = null;
            File destFile = new File(dest);
            File scr1 = new File(f1);
            File scr2 = new File(f2);

            long size = (scr1.length()>scr2.length())?scr2.length():scr1.length();
            fin1 = new FileInputStream(scr1);
            fin2 = new FileInputStream(scr2);

            fout = new FileOutputStream(destFile);
            DataInputStream dip1 = new DataInputStream(fin1);
            DataInputStream dip2 = new DataInputStream(fin2);
            DataOutputStream dop = new DataOutputStream(fout);
            Log.d(TAG,"size is "+size);



            for(int i=0;i<size/2;i++){
                short x1=0,x2=0;
                try{ x1 = dip1.readShort();}catch (Exception e){
                    Log.d(TAG,e.toString());
                }
                try{ x2 = dip2.readShort();}catch (Exception e){
                    Log.d(TAG,e.toString());
                }

                Log.d(TAG," Muxing "+(x1+x2));
                int m = x1+x2;
                if(m>32767) m=32767;
                if(m<-32768) m=-32768;

                // Noisy result
                dop.writeShort((short)m);

                // Tried to increase vol so much noise
                //dop.writeShort((short)((x1+80)+(x2+80)));


            }
            Log.d(TAG,"Finished");
            fout.close();
            return true;
        }catch (Exception e){
            Log.d(TAG," "+e.toString());

            return false;
        }
    }
    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, 44100); // sample rate
            writeInt(output, 48000 * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            output.write(fullyReadFileToBytes(rawFile));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(f);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }
    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }


   /* private void convertToMp3(String path){
        File wavFile = new File(path);
        IConvertCallback callback = new IConvertCallback() {
            @Override
            public void onSuccess(File convertedFile) {
                Toast.makeText(MainActivity.this, "SUCCESS: " + convertedFile.getPath(), Toast.LENGTH_LONG).show();
            }
            @Override
            public void onFailure(Exception error) {
                Toast.makeText(MainActivity.this, "ERROR: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
        Toast.makeText(this, "Converting audio file...", Toast.LENGTH_SHORT).show();
        AndroidAudioConverter.with(this)
                .setFile(wavFile)
                .setFormat(AudioFormat.MP3)
                .setCallback(callback)
                .convert();
     }*/





    @Override
    protected void onPause() {
        super.onPause();

    }

    private void mp3ToPCM(String src, String dest) throws Exception{
        File mp3 = new File(src);
        File pcm = new File(dest);
        final MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(mp3.getAbsolutePath());
        Log.d(TAG,"Track count "+mediaExtractor.getTrackCount());
        MediaFormat format = mediaExtractor.getTrackFormat(0);
        mediaExtractor.selectTrack(0);
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        String decoder_name = mediaCodecList.findDecoderForFormat(format);
        Log.d(TAG,"Decoder Name  :"+decoder_name);
        final MediaCodec mediaCodec = MediaCodec.createByCodecName(decoder_name);
        final FileOutputStream fous = new FileOutputStream(pcm);
        mediaCodec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                ByteBuffer buffer =  codec.getInputBuffer(index);
                int size = mediaExtractor.readSampleData(buffer,0);
                long presentationTime = mediaExtractor.getSampleTime();
                Log.d(TAG, "audio extractor: returned buffer of size"+size);
                Log.d(TAG, "audio extractor: returned buffer for time "+presentationTime);
                if(size<0){
                    codec.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    Log.d(TAG, "audio extractor: EOS");
                }
                else{
                    codec.queueInputBuffer(index,0,size,mediaExtractor.getSampleTime(),0);
                    mediaExtractor.advance();
                }

            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                ByteBuffer buffer = mediaCodec.getOutputBuffer(index);
                byte [] b = new byte[info.size-info.offset];
                int a = buffer.position();
                buffer.get(b);
                buffer.position(a);
                mediaCodec.releaseOutputBuffer(index,true);
                try {
                    fous.write(b,0,info.size-info.offset);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG,e.toString());
                }


            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                Log.d(TAG,"onError");
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                Log.d(TAG, "New format " + mediaCodec.getOutputFormat());
            }
        });

        mediaCodec.configure(format,null,null,0);
        Log.d(TAG,"output format :"+mediaCodec.getOutputFormat());
        int channel = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        Log.d(TAG,"number of channels:"+channel);
        mediaCodec.start();
    }
    private void checkDetails(String path) throws Exception{
        File file = new File(path);
        MediaExtractor mediaExtractor = new MediaExtractor();
        Log.d(TAG,"Track count "+mediaExtractor.getTrackCount());
        mediaExtractor.setDataSource(file.getAbsolutePath());

        MediaFormat format = mediaExtractor.getTrackFormat(0);
        mediaExtractor.selectTrack(0);
        Log.d(TAG,"Channel : "+format.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
        int sample_rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int bit = format.getInteger(MediaFormat.KEY_PCM_ENCODING);
        Log.d(TAG,"Sampling : "+sample_rate);
        Log.d(TAG,"PCM_ENCONDING : "+bit);
    }
}
