package peter.util.appmanager;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by peter on 16/7/12.
 */
public class Manager extends Application {

    private List<ApplicationInfo> list;

    public void init() {
        long start = System.currentTimeMillis();
        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> appList = packageManager.getInstalledApplications(0);
        list = new ArrayList<>(appList.size());
        for (ApplicationInfo info : appList) {
            if (isUserApp(info)) {
                list.add(info);
            }
        }
        Collections.sort(list, new ApplicationInfo.DisplayNameComparator(packageManager));
        Log.i("peter", "time = " + (System.currentTimeMillis() - start));
    }

    public List<ApplicationInfo> getData() {
        return list;
    }

    boolean isUserApp(ApplicationInfo info) {
        int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        return (info.flags & mask) == 0;
    }

}