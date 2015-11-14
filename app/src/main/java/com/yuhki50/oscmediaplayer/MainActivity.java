package com.yuhki50.oscmediaplayer;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


public class MainActivity extends Activity {
    VideoView videoView;
    Handler mHandler;

    OSCPortIn receiver;

    //static final String VIDEO_PATH = "/storage/sdcard0/Movies/";  // Nexus7
    static final String VIDEO_PATH = "/storage/sdcard1/Movies/";  // Xperia

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = (VideoView) findViewById(R.id.videoView);

        // コンテキストメニューを出すためにVideoViewの上にかぶせる
        TextView textView = (TextView) findViewById(R.id.textView);
        registerForContextMenu(textView);


        mHandler = new Handler() {
            public void handleMessage(Message message) {
                List<Object> args = (List<Object>)message.obj;

                if (args.size() == 2) {
                    String command = (String)args.get(0);

                    if (command.equals("play")) {
                        String param = (String)args.get(1);
                        playVideo(VIDEO_PATH + param);
                    }

                    if (command.equals("volume")) {
                        int param = (int)args.get(1);
                        param = (param < 0 ? 0 : param);
                        param = (param > 100 ? 100 : param);
                        setVolume(param);
                    }
                }

                if (args.size() == 1) {
                    String command = (String)args.get(0);

                    if (command.equals("stop")) {
                        stopVideo();
                    } else if (command.equals("pause")) {
                        pauseVideo();
                    } else if (command.equals("resume")) {
                        resumeVideo();
                    }
                }
            }
        };

        try {
            receiver = new OSCPortIn(3000);
            OSCListener listener = new OSCListener() {
                public void acceptMessage(java.util.Date time, OSCMessage message) {
                    List<Object> args = message.getArguments();

                    Message msg = Message.obtain();
                    msg.obj = args;
                    mHandler.sendMessage(msg);
                }
            };
            receiver.addListener("/android", listener);

        } catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        startOscServer();
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopOscServer();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        int viewId = v.getId();

        if (viewId == R.id.textView) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_play) {
            playVideo(VIDEO_PATH + "miku/miku.mp4");
            return true;

        } else if (id == R.id.action_play2) {
            playVideo(VIDEO_PATH + "miku/miku2.mp4");
            return true;

        } else if (id == R.id.action_stop) {
            stopVideo();
            return true;

        } else if (id == R.id.action_ipaddress) {
            String[] addressList = getIPAddress();
            String addressListString = "";

            for (int i = 0; i < addressList.length; i++) {
                if (i > 0) {
                    addressListString += "\n";
                }

                addressListString += addressList[i];
            }

            Toast.makeText(this, addressListString, Toast.LENGTH_LONG).show();
        }

        return super.onContextItemSelected(item);
    }

    public void playVideo(String path) {
        videoView.setVideoPath(path);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                videoView.start();
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                    }
                });
            }
        });
    }

    public void startOscServer() {
        receiver.startListening();
    }

    public void stopOscServer() {
        receiver.stopListening();
    }

    public void setVolume(int value) {
        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        Log.e("max", String.valueOf(am.getStreamMaxVolume(0)));
        float rate = am.getStreamMaxVolume(0) / 100;
        Log.e("rate", String.valueOf(rate));
        Log.e("volume orig", String.valueOf(value));
        Log.e("volume calc", String.valueOf(value * rate));
        //am.setStreamVolume(AudioManager.STREAM_MUSIC, (int)Math.floor(value * rate), 0);

        am.setStreamVolume(AudioManager.STREAM_MUSIC, 5, 0);
    }

    public void pauseVideo() {
        videoView.pause();
    }

    public void resumeVideo() {
        videoView.resume();
    }

    public void stopVideo() {
        videoView.stopPlayback();
    }

    public static String[] getIPAddress() {
        List<String> addressList = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                Enumeration<InetAddress> addresses = network.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    String address = addresses.nextElement().getHostAddress();

                    //127.0.0.1と0.0.0.0以外のアドレスが見つかったらそれを返す
                    if (!"127.0.0.1".equals(address) && !"0.0.0.0".equals(address) && !"::1%1".equals(address)) {
                        addressList.add(address);
                    }
                }
            }
        } catch (Exception ex) {

        }

        return addressList.toArray(new String[addressList.size()]);
    }
}
