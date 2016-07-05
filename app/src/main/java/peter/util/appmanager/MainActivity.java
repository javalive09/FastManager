package peter.util.appmanager;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    AppGridAdapter appAdapter;
    private static final int NO_SYS = 0;
    private static final int ALL = 1;
    private int showType;
    ApplicationInfo clickInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.app_list);
        if(recyclerView != null) {
            int itemW = getResources().getDimensionPixelSize(R.dimen.item_width);
            Resources resources = this.getResources();
            DisplayMetrics dm = resources.getDisplayMetrics();
            int width = dm.widthPixels;

            int count = width/ itemW;
            recyclerView.setLayoutManager(new GridLayoutManager(this, count));
            appAdapter = new AppGridAdapter(this);
            recyclerView.setAdapter(appAdapter);
        }

        int type = getShowType();
        refreshData(type);
    }


    private void refreshData(final int type) {
        new AsyncTask<Void, Void, List<ApplicationInfo>>() {

            @Override
            protected void onPreExecute() {
                showType = type;
                findViewById(R.id.loading).setVisibility(View.VISIBLE);
                findViewById(R.id.app_list).setVisibility(View.GONE);
                super.onPreExecute();
            }

            @Override
            protected List<ApplicationInfo> doInBackground(Void... params) {
                setShowType(type);
                return getAllAppInfos();
            }

            @Override
            protected void onPostExecute(List<ApplicationInfo> applicationInfos) {
                findViewById(R.id.loading).setVisibility(View.GONE);
                findViewById(R.id.app_list).setVisibility(View.VISIBLE);
                appAdapter.updataData(applicationInfos);
                setTitle("AppManager(" + applicationInfos.size() + ")");
            }
        }.execute();
    }

    private int getShowType() {
        int type = getSharedPreferences("showType", MODE_PRIVATE).getInt("showType", NO_SYS);
        return type;
    }

    private void setShowType(int type) {
        getSharedPreferences("showType", MODE_PRIVATE).edit().putInt("showType", type).commit();
    }

    /**
     * 获取所有的应用信息
     *
     * @return
     */
    private List<ApplicationInfo> getAllAppInfos() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> appList = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<ApplicationInfo> allNoSystemApps = new ArrayList<>(appList.size());
        String headPackageName = getHeadPackageName();
        Log.i("peter", "headPackageName = " + headPackageName);
        ApplicationInfo headInfo = null;
        for (ApplicationInfo info : appList) {// 非系统APP
            if (info != null && !isSystemApp(info)
                    && !info.packageName.equals(getPackageName())) {
                if (info.packageName.equals(headPackageName)) {
                    headInfo = info;
                } else {
                    allNoSystemApps.add(info);
                }
            }
        }
        Collections.sort(allNoSystemApps, new DisplayNameComparator(pm));
        if(headInfo != null) {
            allNoSystemApps.add(0, headInfo);
        }
        return allNoSystemApps;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.action_help:
//                showAlertDialog(getString(R.string.action_help),
//                        getString(R.string.action_help_txt));
//                break;
            case R.id.action_refresh:
                refreshData(showType);
                break;
            case R.id.action_about:
                showAlertDialog(getString(R.string.action_about),
                        getString(R.string.action_about_txt));
                break;

            case R.id.action_feedback:
                sendMailByIntent();
                break;
            case R.id.action_all_app:
                refreshData(ALL);
                break;
            case R.id.action_third_app:
                refreshData(NO_SYS);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void sendMailByIntent() {
        Intent data = new Intent(Intent.ACTION_SENDTO);
        data.setData(Uri.parse(getString(R.string.setting_feedback_address)));
        data.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.setting_feedback));
        data.putExtra(Intent.EXTRA_TEXT, getString(R.string.setting_feedback_body));
        startActivity(data);
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        if (showType == ALL) {
            return false;
        }
        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {// system apps
            return true;
        } else {
            return false;
        }
    }

    public AlertDialog showAlertDialog(String title, String content) {
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.setTitle(title);
        dialog.setMessage(content);
        dialog.show();
        return dialog;
    }

    @Override
    public void onClick(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            popupMenu(v);
        } else {
            popupMenuNormal(v);
        }
    }

    private String getHeadPackageName() {
        return getSharedPreferences("head_item", MODE_PRIVATE).getString("package_name", "");
    }

    private void addClickCount(ApplicationInfo info ) {
        if (info != null) {
            SharedPreferences headSp = getSharedPreferences("head_item", MODE_PRIVATE);
            headSp.edit().putString("package_name", info.packageName).commit();
        }
    }

    @Override
    protected void onDestroy() {
        addClickCount(clickInfo);
        super.onDestroy();
    }

    private void showDetailStopView(ApplicationInfo info) {
        if (info != null) {
            int version = Build.VERSION.SDK_INT;
            Intent intent = new Intent();
            if (version >= 9) {
                intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                Uri uri = Uri.fromParts("package", info.packageName, null);
                intent.setData(uri);
            } else {
                final String appPkgName = "pkg";
                intent.setAction(Intent.ACTION_VIEW);
                intent.setClassName("com.android.settings",
                        "com.android.settings.InstalledAppDetails");
                intent.putExtra(appPkgName, info.packageName);
            }
            startActivity(intent);
        }
    }

    private void uninstallAPP(ApplicationInfo info) {
        if (info != null) {
            Uri uri = Uri.parse("package:" + info.packageName);
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(uri);
            startActivity(intent);
        }
    }

    private Intent getLaunchIntent(ApplicationInfo info) {
        return getPackageManager().getLaunchIntentForPackage(
                info.packageName);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void popupMenu(final View anchor) {
        final ApplicationInfo info = (ApplicationInfo) anchor.getTag(R.id.appinfo);
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.setGravity(Gravity.END);
        final Intent launchIntent = getLaunchIntent(info);
        if(launchIntent != null) {
            popup.getMenuInflater().inflate(R.menu.operate1, popup.getMenu());
        }else {
            popup.getMenuInflater().inflate(R.menu.operate, popup.getMenu());
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_launch:
                        startActivity(launchIntent);
                        break;
                    case R.id.action_detail:
                        showDetailStopView(info);
                        break;
                    case R.id.action_uninstall:
                        uninstallAPP(info);
                        break;
                }
                clickInfo = info;
                return true;
            }
        });
        popup.show();
    }

    private void popupMenuNormal(final View anchor) {
        final ApplicationInfo info = (ApplicationInfo) anchor.getTag(R.id.appinfo);
        PopupMenu popup = new PopupMenu(this, anchor);
        final Intent launchIntent = getLaunchIntent(info);
        if(launchIntent != null) {
            popup.getMenuInflater().inflate(R.menu.operate1, popup.getMenu());
        }else {
            popup.getMenuInflater().inflate(R.menu.operate, popup.getMenu());
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_launch:
                        startActivity(launchIntent);
                        break;
                    case R.id.action_detail:
                        showDetailStopView(info);
                        break;
                    case R.id.action_uninstall:
                        uninstallAPP(info);
                        break;
                }
                clickInfo = info;
                return true;
            }
        });
        popup.show();
    }

    public static class DisplayNameComparator
            implements Comparator<ApplicationInfo> {
        public DisplayNameComparator(PackageManager pm) {
            mPM = pm;
            mCollator.setStrength(Collator.PRIMARY);
        }

        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            CharSequence  sa = a.loadLabel(mPM);
            if (sa == null) sa = a.packageName;
            CharSequence  sb = b.loadLabel(mPM);
            if (sb == null) sb = b.packageName;

            return mCollator.compare(sa.toString(), sb.toString());
        }

        private final Collator   mCollator = Collator.getInstance();
        private PackageManager   mPM;
    }

}