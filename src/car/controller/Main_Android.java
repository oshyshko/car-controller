package car.controller;

import android.app.Activity;
import android.os.Bundle;

public class Main_Android extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Thread(new Runnable() {
            public void run() {
                try {
                    Main.main(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        setContentView(R.layout.main);
    }
}
