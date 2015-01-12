package car.controller;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import shared.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static shared.IO.println;

public class MainActivity extends Activity {

    private Net net;

    private final AtomicInteger idSeq       = new AtomicInteger();
    private final AtomicInteger speedVar    = new AtomicInteger();
    private final AtomicInteger steeringVar = new AtomicInteger();
    private final AtomicInteger pingVar     = new AtomicInteger();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Udp.DEBUG = true;

        Map<InetAddress, InetAddress> addresses2broadcasts = Udp.address2broadcast();
        println("My addresses + broadcasts are: " + addresses2broadcasts);
        println();

        if (addresses2broadcasts.size() != 1)
            Errors.die("addresses2broadcasts expected to have exactly one pair, buts was: " +
                    addresses2broadcasts.size() + " (TODO implement selection)");

        InetAddress self      = addresses2broadcasts.keySet().iterator().next();
        InetAddress broadcast = addresses2broadcasts.values().iterator().next();

        try {
            net = new Net(Net.KIND_CONTROLLER, self, broadcast) {
                protected void all_onShout(InetAddress from) {
                    // do nothing
                }

                protected void controller_onUpdateConfirm(InetAddress from, byte id, long ping) {
                    println("~ " + ping);
                    pingVar.set((int) ping);
                }
            };
        } catch (IOException e) {
            Errors.die(e);
        }


        setContentView(R.layout.main);

        final TextView status     = (TextView) findViewById(R.id.status);
        final SeekBar steeringBar = (SeekBar) findViewById(R.id.steering);
        final SeekBar speedBar    = (SeekBar) findViewById(R.id.speed);

        bindSeekBarToAtomic(steeringBar, steeringVar);
        bindSeekBarToAtomic(speedBar, speedVar);

        findViewById(R.id.pair).setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                v.setEnabled(false);
                steeringBar.setEnabled(false);
                speedBar.setEnabled(false);

                net.controller_pair(new Net.OnPair() {
                    public void onPair(InetAddress peerOrNull) {
                        v.setEnabled(true);
                        steeringBar.setEnabled(true);
                        speedBar.setEnabled(true);
                    }
                });

                Threads.fork(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        runOnUiThread(new Runnable() {
                            public void run() {
                            }
                        });
                    }
                });
            }
        });

        Threads.fork(new Runnable() {
            public void run() {

                Sleeper s = new Sleeper();
                while (true) {
                    final byte steering = (byte) steeringVar.get();
                    final byte speed =    (byte) speedVar.get();

                    List<InetAddress> cars = net.peers(Net.KIND_CAR);


                    if (net.getPair() != null) {
                        byte id = (byte) idSeq.getAndIncrement();
                        net.controller_update(
                                net.getPair(),
                                id, steering, speed);
                    }

                    runOnUiThread(new Runnable() {
                        public void run() {
                            status.setText("[" + steering + ", " + speed + "] ~ " + pingVar.get() + "\n" + net.getPair());
                        }
                    });

                    s.sleep(Net.MS_BETWEEN_UPDATES);
                }
            }
        });
    }

    private void bindSeekBarToAtomic(final SeekBar bar, final AtomicInteger atomic) {
        SeekBar.OnSeekBarChangeListener l = new SeekBar.OnSeekBarChangeListener() {
            public static final int ADJUSTMENT = 127;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                atomic.set(progress - ADJUSTMENT);
            }
            public void onStartTrackingTouch(SeekBar seekBar) { }
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(ADJUSTMENT);
            }
        };
        bar.setOnSeekBarChangeListener(l);
        bar.setMax(254);
        l.onProgressChanged(bar, bar.getProgress(), false);
        l.onStopTrackingTouch(bar);
    }
}
