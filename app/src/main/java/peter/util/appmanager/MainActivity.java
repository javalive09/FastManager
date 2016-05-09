package peter.util.appmanager;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.PopupMenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    AppAdapter appAdapter;
    boolean refresh;
    private static final int NO_SYS = 0;
    private static final int ALL = 1;
    private int showType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView list = (ListView) findViewById(R.id.app_list);
        appAdapter = new AppAdapter(this);
        list.setAdapter(appAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    private void refreshData() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showType = getShowType();
                appAdapter.updataData(getAllAppInfos());
            }
        });
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
    private List<AppAdapter.AppInfo> getAllAppInfos() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> appList = pm.getInstalledApplications(0);
        List<AppAdapter.AppInfo> allNoSystemApps = new ArrayList<>(appList.size());
        if (refresh) {
            for (ApplicationInfo info : appList) {// 非系统APP
                if (info != null && !isSystemApp(info)
                        && !info.packageName.equals(getPackageName())) {
                    AppAdapter.AppInfo inf = new AppAdapter.AppInfo();
                    inf.packageName = info.packageName;
                    allNoSystemApps.add(inf);
                }
            }
        } else {
            String headPackageName = getHeadPackageName();
            for (ApplicationInfo info : appList) {// 非系统APP
                if (info != null && !isSystemApp(info)
                        && !info.packageName.equals(getPackageName())) {
                    AppAdapter.AppInfo inf = new AppAdapter.AppInfo();
                    inf.packageName = info.packageName;
                    if (inf.packageName.equals(headPackageName)) {
                        allNoSystemApps.add(0, inf);
                    } else {
                        allNoSystemApps.add(inf);
                    }
                }
            }
            refresh = true;
        }

        Collections.sort(allNoSystemApps, AppComparator);
        return allNoSystemApps;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                showAlertDialog(getString(R.string.action_help),
                        getString(R.string.action_help_txt));
                break;
            case R.id.action_about:
                showAlertDialog(getString(R.string.action_about),
                        getString(R.string.action_about_txt));
                break;

            case R.id.action_feedback:
                sendMailByIntent();
                break;
            case R.id.action_all_app:
                setShowType(ALL);
                refreshData();
                break;
            case R.id.action_third_app:
                setShowType(NO_SYS);
                refreshData();
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
        if(showType == ALL) {
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
        addClickCount(v);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            popupMenu(v);
        }else {
            popupMenuNormal(v);
        }
    }

    Comparator<AppAdapter.AppInfo> AppComparator = new Comparator<AppAdapter.AppInfo>() {

        @Override
        public int compare(AppAdapter.AppInfo lhs, AppAdapter.AppInfo rhs) {
            if (lhs.count < rhs.count) {
                return 1;
            } else if (lhs.count > rhs.count) {
                return -1;
            }
            return 0;
        }
    };

    private String getHeadPackageName() {
        return getSharedPreferences("head_item", MODE_PRIVATE).getString("package_name", "");
    }

    private void addClickCount(View v) {
        AppAdapter.AppInfo info = (AppAdapter.AppInfo) v.getTag(R.id.appinfo);
        if (info != null) {
            SharedPreferences sp = getSharedPreferences("head_item", MODE_PRIVATE);
            sp.edit().putString("package_name", info.packageName).commit();
        }
    }

    private void showDetailStopView(View v) {
        AppAdapter.AppInfo info = (AppAdapter.AppInfo) v.getTag(R.id.appinfo);
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

    private void uninstallAPP(View v) {
        AppAdapter.AppInfo info = (AppAdapter.AppInfo) v.getTag(R.id.appinfo);
        if (info != null) {
            Uri uri = Uri.parse("package:" + info.packageName);
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(uri);
            startActivity(intent);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void popupMenu(final View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.setGravity(Gravity.END);
        popup.getMenuInflater().inflate(R.menu.operate, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_detail:
                        showDetailStopView(anchor);
                        break;
                    case R.id.action_uninstall:
                        uninstallAPP(anchor);
                        break;
                }

                return true;
            }
        });
        popup.show();
    }

    private void popupMenuNormal(final View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.operate, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_detail:
                        showDetailStopView(anchor);
                        break;
                    case R.id.action_uninstall:
                        uninstallAPP(anchor);
                        break;
                }

                return true;
            }
        });
        popup.show();
    }
}