package peter.util.appmanager;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    AppAdapter appAdapter;
    boolean refresh;

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
        appAdapter.updataData(getAllAppInfos());
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
        AppAdapter.AppInfo info = (AppAdapter.AppInfo) v.getTag(R.id.appinfo);
        if (info != null) {
            showDetailStopView(info);
            addClickCount(info);
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

    private void addClickCount(AppAdapter.AppInfo info) {
        SharedPreferences sp = getSharedPreferences("head_item", MODE_PRIVATE);
        sp.edit().putString("package_name", info.packageName).commit();
    }

    private void showDetailStopView(AppAdapter.AppInfo info) {
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

    @Override
    public boolean onLongClick(View v) {
        AppAdapter.AppInfo info = (AppAdapter.AppInfo) v.getTag(R.id.appinfo);
        if (info != null) {
            Intent intent = this.getPackageManager().getLaunchIntentForPackage(
                    info.packageName);
            if (intent != null) {
                startActivity(intent);
            }
        }
        return true;
    }
}