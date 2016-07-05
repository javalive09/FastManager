package peter.util.appmanager;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AppGridAdapter extends RecyclerView.Adapter<AppGridAdapter.Holder> {

    private List<ApplicationInfo> mAppInfos = new ArrayList<>();
    LayoutInflater factory;
    private MainActivity mAct;
    private LruCache<String, Bitmap> mMemoryCache;
    private HashMap<String, String> mAppNames = new HashMap<>();
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;
    private Executor thread_pool_executor;
    private BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<>(39);
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    public AppGridAdapter(MainActivity act) {
        mAct = act;
        factory = LayoutInflater.from(act);
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

    public void updataData(List<ApplicationInfo> list) {
        mAppInfos = list;
        notifyDataSetChanged();
    }

    private ApplicationInfo getItem(int position) {
        return mAppInfos != null ? mAppInfos.get(position) : null;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(factory.inflate(R.layout.gridviewitem, parent, false));
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        ApplicationInfo info = getItem(position);
        if(info != null) {
            holder.currentInfoId = info.hashCode();
            holder.itemView.setTag(R.id.appinfo, info);
            holder.itemView.setOnClickListener(mAct);

            //取缓存图片
            Bitmap bmIcon = mMemoryCache.get(info.packageName);
            if (bmIcon == null) {
                holder.app_icon.setImageResource(R.mipmap.ic_launcher);
                holder.app_name.setText("...");
                thread_pool_executor.execute(new ThreadPoolTask(mAct, holder, info, mMemoryCache, mAppNames));
            } else {
                holder.app_icon.setImageBitmap(bmIcon);
                holder.app_name.setText(mAppNames.get(info.packageName));
            }
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mAppInfos.size();
    }

    private static class ThreadPoolTask implements Runnable {

        static int rightIconSize;
        Holder mCache;
        LruCache<String, Bitmap> mMemoryCache;
        PackageManager mPm;
        ApplicationInfo mInfo;
        MainActivity mAct;
        HashMap<String, String> mAppNames;

        public ThreadPoolTask(MainActivity act, Holder cache, ApplicationInfo info, LruCache<String, Bitmap> memoryCache, HashMap<String, String> names) {
            mCache = cache;
            mInfo = info;
            mPm = act.getPackageManager();
            mMemoryCache = memoryCache;
            mAct = act;
            mAppNames = names;
        }

        @Override
        public void run() {
            mAppNames.put(mInfo.packageName, mInfo.loadLabel(mPm).toString());
            if (mCache.currentInfoId == mInfo.hashCode()) {
                mAct.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        mCache.app_name.setText(mAppNames.get(mInfo.packageName));
                    }
                });
            }

            final Drawable drawable = mInfo.loadIcon(mPm);
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                final Bitmap bmIcon = getRightSizeIcon(bitmapDrawable).getBitmap();
                if (bmIcon == null) {
                    Log.i("peter", "==");
                }
                mMemoryCache.put(mInfo.packageName, bmIcon);
                if (mCache.currentInfoId == mInfo.hashCode()) {
                    mAct.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            mCache.app_icon.setImageBitmap(bmIcon);
                        }
                    });
                }
            } else {
                Log.i("peter", "mInfo=" + mInfo);
                if (mCache.currentInfoId == mInfo.hashCode()) {
                    mAct.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            mCache.app_icon.setImageDrawable(drawable);
                        }
                    });
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
            if (rightIconSize == 0) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeResource(mAct.getResources(), R.mipmap.ic_launcher, options);
                rightIconSize = options.outWidth;
            }
            return rightIconSize;
        }

    }


    public static class Holder extends RecyclerView.ViewHolder {
        ImageView app_icon;
        TextView app_name;
        int currentInfoId;

        public Holder(View itemView) {
            super(itemView);
            app_icon = (ImageView) itemView.findViewById(R.id.app_icon);
            app_name = (TextView) itemView.findViewById(R.id.app_name);
        }
    }


}