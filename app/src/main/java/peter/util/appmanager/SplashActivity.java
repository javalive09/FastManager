package peter.util.appmanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.os.MessageQueue;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {

            @Override
            public boolean queueIdle() {
                Manager manager = (Manager) getApplication();
                manager.init();
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
                return false;
            }
        });
    }
}