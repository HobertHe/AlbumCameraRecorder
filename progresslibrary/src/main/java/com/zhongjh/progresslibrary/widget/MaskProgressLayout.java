package com.zhongjh.progresslibrary.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.zhongjh.progresslibrary.R;
import com.zhongjh.progresslibrary.engine.ImageEngine;
import com.zhongjh.progresslibrary.entity.MultiMediaView;
import com.zhongjh.progresslibrary.entity.RecordingItem;
import com.zhongjh.progresslibrary.listener.MaskProgressLayoutListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gaode.zhongjh.com.common.entity.MultimediaTypes;
import gaode.zhongjh.com.common.entity.SaveStrategy;
import gaode.zhongjh.com.common.utils.MediaStoreCompat;

/**
 * 这是返回（图片、视频、录音）等文件后，显示的Layout
 * Created by zhongjh on 2018/10/17.
 * https://www.jianshu.com/p/191c41f63dc7
 */
public class MaskProgressLayout extends FrameLayout {

    private MediaStoreCompat mMediaStoreCompat; // 文件配置路径
    private boolean isOperation = true;            // 是否允许操作

    public ViewHolder mViewHolder;          // 控件集合
    private ImageEngine mImageEngine;       // 图片加载方式

    public List<MultiMediaView> audioList = new ArrayList<>();     // 音频数据
    private int audioProgressColor;                 // 音频 文件的进度条颜色
    private MaskProgressLayoutListener listener;   // 点击事件(这里只针对音频)

    public void setMaskProgressLayoutListener(MaskProgressLayoutListener listener) {
        mViewHolder.alfMedia.setListener(listener);
        mViewHolder.playView.setListener(listener);
        this.listener = listener;
    }

    public MaskProgressLayout(@NonNull Context context) {
        this(context, null);
    }

    public MaskProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaskProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(attrs);
    }

    /**
     * 初始化view
     */
    private void initView(AttributeSet attrs) {
        // 自定义View中如果重写了onDraw()即自定义了绘制，那么就应该在构造函数中调用view的setWillNotDraw(false).
        setWillNotDraw(false);

        mMediaStoreCompat = new MediaStoreCompat(getContext());
        mViewHolder = new ViewHolder(View.inflate(getContext(), R.layout.layout_mask_progress, this));

        // 获取系统颜色
        int defaultColor = 0xFF000000;
        int[] attrsArray = {R.attr.colorPrimary, R.attr.colorPrimaryDark, R.attr.colorAccent};
        TypedArray typedArray = getContext().obtainStyledAttributes(attrsArray);
        int colorPrimary = typedArray.getColor(0, defaultColor);

        // 获取自定义属性。
        TypedArray maskProgressLayoutStyle = getContext().obtainStyledAttributes(attrs, R.styleable.MaskProgressLayoutStyle);
        // 是否允许操作
        isOperation = maskProgressLayoutStyle.getBoolean(R.styleable.MaskProgressLayoutStyle_isOperation, true);
        // 获取默认图片
        Drawable drawable = maskProgressLayoutStyle.getDrawable(R.styleable.MaskProgressLayoutStyle_album_thumbnail_placeholder);
        // 获取添加图片
        Drawable imageAddDrawable = maskProgressLayoutStyle.getDrawable(R.styleable.MaskProgressLayoutStyle_imageAddDrawable);
        // 获取显示图片的类
        String imageEngineStr = maskProgressLayoutStyle.getString(R.styleable.MaskProgressLayoutStyle_imageEngine);
        // provider的authorities,用于提供给外部的file
        String authority = maskProgressLayoutStyle.getString(R.styleable.MaskProgressLayoutStyle_authority);
        // 获取最多显示多少个方框
        int imageCount = maskProgressLayoutStyle.getInteger(R.styleable.MaskProgressLayoutStyle_maxCount, 5);
        int imageDeleteColor = maskProgressLayoutStyle.getColor(R.styleable.MaskProgressLayoutStyle_imageDeleteColor, colorPrimary);
        Drawable imageDeleteDrawable = maskProgressLayoutStyle.getDrawable(R.styleable.MaskProgressLayoutStyle_imageDeleteDrawable);

        // region 音频
        // 音频，删除按钮的颜色
        int audioDeleteColor = maskProgressLayoutStyle.getColor(R.styleable.MaskProgressLayoutStyle_audioDeleteColor, colorPrimary);
        // 音频 文件的进度条颜色
        audioProgressColor = maskProgressLayoutStyle.getColor(R.styleable.MaskProgressLayoutStyle_audioProgressColor, colorPrimary);
        // 音频 播放按钮的颜色
        int audioPlayColor = maskProgressLayoutStyle.getColor(R.styleable.MaskProgressLayoutStyle_audioPlayColor, colorPrimary);
        // endregion 音频

        // region 遮罩层相关属性

        int maskingColor = maskProgressLayoutStyle.getColor(R.styleable.MaskProgressLayoutStyle_maskingColor, colorPrimary);
        int maskingTextSize = maskProgressLayoutStyle.getInteger(R.styleable.MaskProgressLayoutStyle_maskingTextSize, 12);
        int maskingTextColor = maskProgressLayoutStyle.getColor(R.styleable.MaskProgressLayoutStyle_maskingTextColor, getContext().getResources().getColor(R.color.thumbnail_placeholder));
        String maskingTextContent = maskProgressLayoutStyle.getString(R.styleable.MaskProgressLayoutStyle_maskingTextContent);

        // endregion 遮罩层相关属性

        if (imageEngineStr == null) {
            throw new RuntimeException("必须定义image_engine属性，指定某个显示图片类");
        } else {
            Class<?> imageEngineClass;//完整类名
            try {
                imageEngineClass = Class.forName(imageEngineStr);
                mImageEngine = (ImageEngine) imageEngineClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mImageEngine == null) {
                throw new RuntimeException("image_engine找不到相关类");
            }
        }

        if (authority == null){
            throw new RuntimeException("必须定义authority属性，指定provider的authorities,用于提供给外部的file,否则Android7.0以上报错");
        }else {
            SaveStrategy saveStrategy = new SaveStrategy(true,authority,"");
            mMediaStoreCompat.setSaveStrategy(saveStrategy);
        }

        if (drawable == null) {
            drawable = getResources().getDrawable(R.color.thumbnail_placeholder);
        }
        // 初始化九宫格的控件
        mViewHolder.alfMedia.initConfig(this, mImageEngine, isOperation, drawable, imageCount, maskingColor, maskingTextSize, maskingTextColor, maskingTextContent, imageDeleteColor, imageDeleteDrawable, imageAddDrawable);
        // 设置上传音频等属性
        mViewHolder.imgRemoveRecorder.setColorFilter(audioDeleteColor);
        isShowRemovceRecorder();
        mViewHolder.numberProgressBar.setProgressTextColor(audioProgressColor);
        mViewHolder.numberProgressBar.setReachedBarColor(audioProgressColor);
        mViewHolder.tvRecorderTip.setTextColor(audioProgressColor);

        // 设置播放控件里面的播放按钮的颜色
        mViewHolder.playView.mViewHolder.imgPlay.setColorFilter(audioPlayColor);
        mViewHolder.playView.mViewHolder.tvCurrentProgress.setTextColor(audioProgressColor);
        mViewHolder.playView.mViewHolder.tvTotalProgress.setTextColor(audioProgressColor);

        maskProgressLayoutStyle.recycle();
        typedArray.recycle();

        initListener();
    }

    /**
     * 设置图片同时更新表格
     *
     * @param imagePaths 图片数据源
     */
    public void addImages(List<String> imagePaths) {
        ArrayList<MultiMediaView> multiMediaViews = new ArrayList<>();
        for (String string : imagePaths) {
            MultiMediaView multiMediaView = new MultiMediaView(MultimediaTypes.PICTURE);
            multiMediaView.setPath(string);
            multiMediaView.setUri(mMediaStoreCompat.getUri(string));
            multiMediaViews.add(multiMediaView);
        }
        mViewHolder.alfMedia.addImageData(multiMediaViews);
    }

    /**
     * 添加图片网址数据
     *
     * @param imagesUrls 图片网址
     */
    public void addImageUrls(List<String> imagesUrls) {
        ArrayList<MultiMediaView> multiMediaViews = new ArrayList<>();
        for (String string : imagesUrls) {
            MultiMediaView multiMediaView = new MultiMediaView(MultimediaTypes.PICTURE);
            multiMediaView.setUrl(string);
            multiMediaViews.add(multiMediaView);
        }
        mViewHolder.alfMedia.addImageData(multiMediaViews);
    }

    /**
     * 设置视频地址
     */
    public void addVideo(List<String> videoPath,boolean icClean,boolean isUploading) {
        ArrayList<MultiMediaView> multiMediaViews = new ArrayList<>();
        for (String string : videoPath) {
            MultiMediaView multiMediaView = new MultiMediaView(MultimediaTypes.VIDEO);
            multiMediaView.setPath(string);
            multiMediaView.setUri(mMediaStoreCompat.getUri(string));
            multiMediaViews.add(multiMediaView);
        }
        mViewHolder.alfMedia.addVideoData(multiMediaViews, icClean, isUploading);
    }

    /**
     * 添加视频网址数据
     *
     * @param videoUrl 视频网址
     */
    public void addVideoUrl(String videoUrl) {
        ArrayList<MultiMediaView> multiMediaViews = new ArrayList<>();
        MultiMediaView multiMediaView = new MultiMediaView(MultimediaTypes.VIDEO);
        multiMediaView.setUrl(videoUrl);
        multiMediaViews.add(multiMediaView);
        mViewHolder.alfMedia.addVideoData(multiMediaViews, false, false);
    }

    /**
     * 设置音频数据
     *
     * @param filePath 音频文件地址
     */
    public void addAudio(String filePath, int length) {
        MultiMediaView multiMediaView = new MultiMediaView( MultimediaTypes.AUDIO);
        multiMediaView.setPath(filePath);
        multiMediaView.setUri(mMediaStoreCompat.getUri(filePath));
        multiMediaView.setViewHolder(this);
        addAudioData(multiMediaView);

        // 显示上传中的音频
        mViewHolder.groupRecorderProgress.setVisibility(View.VISIBLE);
        mViewHolder.playView.setVisibility(View.GONE);
        isShowRemovceRecorder();

        // 初始化播放控件
        RecordingItem recordingItem = new RecordingItem();
        recordingItem.setFilePath(filePath);
        recordingItem.setLength(length);
        mViewHolder.playView.setData(recordingItem, audioProgressColor);

        // 检测添加多媒体上限
        mViewHolder.alfMedia.checkLastImages();
    }

    /**
     * 添加音频网址数据
     *
     * @param audioUrl 音频网址
     */
    public void addAudioUrl(String audioUrl) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.prepare();
            int duration = mediaPlayer.getDuration();
            if (0 != duration) {
                MultiMediaView multiMediaView = new MultiMediaView(MultimediaTypes.AUDIO);
                multiMediaView.setUrl(audioUrl);
                multiMediaView.setViewHolder(this);

                if (this.audioList == null) {
                    this.audioList = new ArrayList<>();
                }
                audioList.add(multiMediaView);

                // 显示音频播放控件，当点击播放的时候，才正式下载并且进行播放
                mViewHolder.playView.setVisibility(View.VISIBLE);
                isShowRemovceRecorder();
                RecordingItem recordingItem = new RecordingItem();
                recordingItem.setUrl(audioUrl);
                recordingItem.setLength(duration);
                mViewHolder.playView.setData(recordingItem, audioProgressColor);
                //记得释放资源
                mediaPlayer.release();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 直接添加音频实际的文件
     *
     * @param file 文件路径
     */
    public void addAudioFile(String file) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(file);

        String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION); // ms,时长


        MultiMediaView multiMediaView = new MultiMediaView(MultimediaTypes.AUDIO);
        multiMediaView.setPath(file);
        multiMediaView.setViewHolder(this);

        // 显示音频播放控件，当点击播放的时候，才正式下载并且进行播放
        mViewHolder.playView.setVisibility(View.VISIBLE);
        isShowRemovceRecorder();
        RecordingItem recordingItem = new RecordingItem();
        recordingItem.setFilePath(file);
        recordingItem.setLength(Integer.valueOf(duration));
        mViewHolder.playView.setData(recordingItem, audioProgressColor);
    }


    /**
     * 添加音频数据
     *
     * @param multiMediaView 数据
     */
    public void addAudioData(MultiMediaView multiMediaView) {
        if (this.audioList == null) {
            this.audioList = new ArrayList<>();
        }
        this.audioList.add(multiMediaView);
        if (audioList != null && audioList.size() > 0) {
            // 显示音频的进度条
            this.listener.onItemStartUploading(multiMediaView);
        }
    }

    /**
     * 音频上传完成后
     */
    public void audioUploadCompleted() {
        // 显示完成后的音频
        mViewHolder.groupRecorderProgress.setVisibility(View.GONE);
        mViewHolder.playView.setVisibility(View.VISIBLE);
        isShowRemovceRecorder();
    }

    /**
     * 语音点击
     */
    public void onAudioClick() {
        mViewHolder.playView.mViewHolder.imgPlay.performClick();
    }

    /**
     * 视频点击
     */
    public void onVideoClick() {
        mViewHolder.alfMedia.getChildAt(0).performClick();
    }

    /**
     * 删除单个图片
     * @param position 图片的索引，该索引列表不包含视频等
     */
    public void onRemoveItemImage(int position){
        mViewHolder.alfMedia.onRemoveItemImage(position);
    }

    /**
     * @return 返回当前包含url的图片数据
     */
    public List<MultiMediaView> getImages() {
        return mViewHolder.alfMedia.imageList;
    }

    /**
     * @return 返回当前包含url的视频数据
     */
    public List<MultiMediaView> getVideos() {
        return mViewHolder.alfMedia.videoList;
    }

    /**
     * @return 返回当前包含url的音频数据
     */
    public List<MultiMediaView> getAudios() {
        return this.audioList;
    }

    /**
     * 设置是否操作
     *
     * @param isOperation
     */
    public void setOperation(boolean isOperation) {
        this.isOperation = isOperation;
        mViewHolder.alfMedia.setOperation(isOperation);
        isShowRemovceRecorder();
    }

    /**
     * 销毁所有相关正在执行的东西
     */
    public void destroy() {
        mViewHolder.playView.deStory();
    }

    /**
     * 初始化所有事件
     */
    private void initListener() {
        // 音频删除事件
        this.mViewHolder.imgRemoveRecorder.setOnClickListener(v -> {
            if (audioList.size() > 0)
                // 需要判断，防止是网址状态未提供实体数据的
                listener.onItemClose(MaskProgressLayout.this, audioList.get(0));
            // 隐藏音频相关控件
            mViewHolder.groupRecorderProgress.setVisibility(View.GONE);
            mViewHolder.playView.setVisibility(View.GONE);
            audioList.clear();
            mViewHolder.imgRemoveRecorder.setVisibility(View.GONE);
            mViewHolder.alfMedia.checkLastImages();
            isShowRemovceRecorder();
            mViewHolder.playView.reset();
        });
    }

    /**
     * 设置是否显示删除音频按钮
     */
    private void isShowRemovceRecorder() {
        if (isOperation) {
            // 如果是可操作的，就判断是否有音频数据
            if (this.mViewHolder.playView.getVisibility() == View.VISIBLE || this.mViewHolder.groupRecorderProgress.getVisibility() == View.VISIBLE)
                mViewHolder.imgRemoveRecorder.setVisibility(View.VISIBLE);
            else
                mViewHolder.imgRemoveRecorder.setVisibility(View.GONE);
        } else {
            mViewHolder.imgRemoveRecorder.setVisibility(View.GONE);
        }
    }

    public static class ViewHolder {
        View rootView;
        AutoLineFeedLayout alfMedia;
        public NumberProgressBar numberProgressBar;
        public ImageView imgRemoveRecorder;
        public Group groupRecorderProgress;
        public PlayView playView;
        public TextView tvRecorderTip;

        public ViewHolder(View rootView) {
            this.rootView = rootView;
            this.alfMedia = rootView.findViewById(R.id.alfMedia);
            this.numberProgressBar = rootView.findViewById(R.id.numberProgressBar);
            this.imgRemoveRecorder = rootView.findViewById(R.id.imgRemoveRecorder);
            this.playView = rootView.findViewById(R.id.playView);
            this.groupRecorderProgress = rootView.findViewById(R.id.groupRecorderProgress);
            this.tvRecorderTip = rootView.findViewById(R.id.tvRecorderTip);
        }
    }
}
