package peter.util.appmanager;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AppAdapter extends BaseAdapter {

    private List<AppInfo> mAppInfos;
    private MainActivity mAct;
    private LruCache<String, Bitmap> mMemoryCache;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT;
    private static final int KEEP_ALIVE = 1;
    private Executor thread_pool_executor;
    private BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<>(15);
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    public AppAdapter(MainActivity act, List<AppAdapter.AppInfo> list) {
        mAct = act;
        mAppInfos = list;
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int mCacheSize = maxMemory / 5;
        //给LruCache分配
        mMemoryCache = new LruCache<String, Bitmap>(mCacheSize) {

            //必须重写此方法，来测量Bitmap的大小
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }

        };
        thread_pool_executor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
                sPoolWorkQueue, sThreadFactory, new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    @Override
    public int getCount() {
        return mAppInfos.size();
    }

    @Override
    public AppInfo getItem(int position) {
        return mAppInfos != null ? mAppInfos.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 获取数据
        AppInfo info = getItem(position);

        // 获取View
        ViewCache cache;
        if (convertView == null) {
            convertView = View.inflate(mAct, R.layout.listviewitem, null);
            cache = new ViewCache();
            cache.app_icon = (ImageView) convertView.findViewById(R.id.app_icon);
            cache.app_name = (TextView) convertView.findViewById(R.id.app_name);
            convertView.setTag(cache);
        } else {
            cache = (ViewCache) convertView.getTag();
        }

        // 绑定数据
        cache.currentInfoId = info.hashCode();
        convertView.setTag(R.id.appinfo, info);
        convertView.setOnClickListener(mAct);
        convertView.setOnLongClickListener(mAct);

        //取缓存图片
        Bitmap bmIcon = mMemoryCache.get(info.packageName);
        if (bmIcon == null) {
            cache.app_icon.setImageResource(R.mipmap.ic_launcher);
            cache.app_name.setText("...");
            thread_pool_executor.execute(new ThreadPoolTask(mAct, cache, info, mMemoryCache));
        } else {
            cache.app_icon.setImageBitmap(bmIcon);
            cache.app_name.setText(info.appName);
        }

        return convertView;
    }

    private static class ThreadPoolTask implements Runnable {

        static int rightIconSize;
        ViewCache mCache;
        LruCache<String, Bitmap> mMemoryCache;
        PackageManager mPm;
        AppInfo mInfo;
        MainActivity mAct;

        public ThreadPoolTask(MainActivity act, ViewCache cache, AppInfo info, LruCache<String, Bitmap> memoryCache) {
            mCache = cache;
            mInfo = info;
            mPm = act.getPackageManager();
            mMemoryCache = memoryCache;
            mAct = act;
        }

        @Override
        public void run() {
            ApplicationInfo info = null;
            try {
                info = mPm.getApplicationInfo(mInfo.packageName, PackageManager.GET_META_DATA);
                mInfo.appName = info.loadLabel(mPm).toString();
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            if (info != null) {
                Drawable drawable = info.loadIcon(mPm);
                if(drawable instanceof BitmapDrawable) {
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                    final Bitmap bmIcon = getRightSizeIcon(bitmapDrawable).getBitmap();
                    mMemoryCache.put(mInfo.packageName, bmIcon);
                    if (mCache.currentInfoId == mInfo.hashCode()) {
                        mAct.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                mCache.app_icon.setImageBitmap(bmIcon);
                                mCache.app_name.setText(mInfo.appName);
                            }
                        });
                    }
                }
            }
        }

        private BitmapDrawable getRightSizeIcon(BitmapDrawable drawable) {
            int size = getIconSize();
            Log.i("peter", "rightSize" + size);
            Bitmap bitmap = drawable.getBitmap();
            float scale = (size * 1.f) / (bitmap.getWidth() * 1.f);
            Matrix matrix = new Matrix();
            matrix.setScale(scale, scale);
            Bitmap bm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            return new BitmapDrawable(mAct.getResources(), bm);
        }

        private int getIconSize() {
            if(rightIconSize == 0) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeResource(mAct.getResources(), R.mipmap.ic_launcher, options);
                rightIconSize = options.outWidth;
            }
            return rightIconSize;
        }

    }


    public static class ViewCache {
        ImageView app_icon;
        TextView app_name;
        int currentInfoId;
    }

    public static class AppInfo {
        public String appName;
        public String packageName;
        public int count;
    }

}