package hb.xvideoplayer_lib;


import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Surface;

import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;


public class MxMediaManager  implements IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener,
        IMediaPlayer.OnBufferingUpdateListener, IMediaPlayer.OnSeekCompleteListener, IMediaPlayer.OnErrorListener,
        IMediaPlayer.OnVideoSizeChangedListener, IMediaPlayer.OnInfoListener {

    private static String TAG = "MxVideoPlayer";
    public static final int HANDLER_PREPARE = 0;
    public static final int HANDLER_SET_DISPLAY = 1;
    public static final int HANDLER_RELEASE = 2;

    private static MxMediaManager mxMediaManager;
    private IjkMediaPlayer mMediaPlayer;
    private HandlerThread mMediaHandlerThread;
    private MediaHandler mMediaHandler;
    private Handler mainThreadHandler;
    public static MxTextureView mTextureView;

    private int mCurVideoWidth;
    private int mCurVideoHeight;

    private MxMediaManager() {
        mMediaPlayer = new IjkMediaPlayer();
        mMediaHandlerThread = new HandlerThread(TAG);
        mMediaHandlerThread.start();
        mMediaHandler = new MediaHandler(mMediaHandlerThread.getLooper());
        mainThreadHandler = new Handler();
    }

    public static MxMediaManager getInstance() {
        if (mxMediaManager == null) {
            synchronized (MxMediaManager.class) {
                if (mxMediaManager == null) {
                    mxMediaManager = new MxMediaManager();
                }
            }
        }
        return mxMediaManager;
    }

    public void prepare(final String url, final Map<String, String> mapHeapData, boolean loop) {
        if (!TextUtils.isEmpty(url)) {
           Message msg = Message.obtain();
            msg.obj = new DataBean(url, mapHeapData, loop);
            msg.what = HANDLER_PREPARE;
            mMediaHandler.sendMessage(msg);
        }
    }

    public void releaseMediaPlayer() {
        Message msg = Message.obtain();
        msg.what = HANDLER_RELEASE;
        mMediaHandler.sendMessage(msg);
    }

    public void setDisplay(Surface holder) {
        Message msg = Message.obtain();
        msg.what = HANDLER_SET_DISPLAY;
        msg.obj = holder;
        mMediaHandler.sendMessage(msg);
    }

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (MxVideoPlayerManager.getFirst() != null) {
                    MxVideoPlayerManager.getFirst().onPrepared();
                }
            }
        });
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer iMediaPlayer, final int percent) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (MxVideoPlayerManager.getFirst() != null) {
                    MxVideoPlayerManager.getFirst().onBufferingUpdate(percent);
                }
            }
        });
    }

    @Override
    public void onCompletion(IMediaPlayer iMediaPlayer) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (MxVideoPlayerManager.getFirst() != null) {
                    MxVideoPlayerManager.getFirst().onCompletion();
                }
            }
        });
    }

    @Override
    public boolean onError(IMediaPlayer iMediaPlayer, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (MxVideoPlayerManager.getFirst() != null) {
                    MxVideoPlayerManager.getFirst().onError(what, extra);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer iMediaPlayer, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (MxVideoPlayerManager.getFirst() != null) {
                    MxVideoPlayerManager.getFirst().onInfo(what, extra);
                }
            }
        });
        return false;
    }

    @Override
    public void onSeekComplete(IMediaPlayer iMediaPlayer) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (MxVideoPlayerManager.getFirst() != null) {
                    MxVideoPlayerManager.getFirst().onSeekComplete();
                }
            }
        });
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
        mCurVideoWidth = mp.getVideoWidth();
        mCurVideoHeight = mp.getVideoHeight();
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (MxVideoPlayerManager.getFirst() != null) {
                    MxVideoPlayerManager.getFirst().onVideoSizeChanged();
                }
            }
        });
    }

    private class MediaHandler extends Handler {
        public MediaHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_PREPARE:
                    try {
                        mCurVideoWidth = 0;
                        mCurVideoHeight = 0;
                        mMediaPlayer.release();
                        if (mMediaPlayer == null) {
                            mMediaPlayer = new IjkMediaPlayer();
                        }
                        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        DataBean data = (DataBean) msg.obj;
                        mMediaPlayer.setDataSource(data.url);
                        mMediaPlayer.setLooping(data.looping);
                        mMediaPlayer.setOnPreparedListener(MxMediaManager.this);
                        mMediaPlayer.setOnCompletionListener(MxMediaManager.this);
                        mMediaPlayer.setOnBufferingUpdateListener(MxMediaManager.this);
                        mMediaPlayer.setScreenOnWhilePlaying(true);
                        mMediaPlayer.setOnSeekCompleteListener(MxMediaManager.this);
                        mMediaPlayer.setOnErrorListener(MxMediaManager.this);
                        mMediaPlayer.setOnInfoListener(MxMediaManager.this);
                        mMediaPlayer.setOnVideoSizeChangedListener(MxMediaManager.this);
                        mMediaPlayer.prepareAsync();
                        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "reconnect", 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case HANDLER_SET_DISPLAY:
                    if (msg.obj == null) {
                        getInstance().mMediaPlayer.setSurface(null);
                    } else {
                        Surface holder = (Surface) msg.obj;
                        if (holder.isValid()) {
                            mMediaPlayer.setSurface(holder);
                            mainThreadHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mTextureView.requestLayout();
                                }
                            });
                        }
                    }
                    break;
                case HANDLER_RELEASE:
                    mMediaPlayer.reset();
                    mMediaPlayer.release();
                    break;
            }
        }
    }

    private class DataBean {
        String url;
        Map<String, String> mapHeadData;
        boolean looping;

        DataBean(String url, Map<String, String> mapHeadData, boolean loop) {
            this.url = url;
            this.mapHeadData = mapHeadData;
            this.looping = loop;
        }
    }
}