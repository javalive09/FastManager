package peter.util.appmanager;

import android.view.ViewGroup;

public class AppListAdapter extends AppAdapter {

    public AppListAdapter(MainActivity act) {
        super(act);
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(factory.inflate(R.layout.listviewitem, parent, false));
    }

}