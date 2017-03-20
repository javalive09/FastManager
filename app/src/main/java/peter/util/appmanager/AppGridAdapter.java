package peter.util.appmanager;

import android.view.ViewGroup;

public class AppGridAdapter extends AppAdapter {

    public AppGridAdapter(MainActivity act) {
        super(act);
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(factory.inflate(R.layout.gridviewitem, parent, false));
    }

}