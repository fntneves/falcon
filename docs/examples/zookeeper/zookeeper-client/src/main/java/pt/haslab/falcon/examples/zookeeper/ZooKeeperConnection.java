package pt.haslab.falcon.examples.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ZooKeeperConnection
{
    final CountDownLatch connectedSignal = new CountDownLatch( 1 );

    private ZooKeeper zoo;

    public ZooKeeper connect( String host )
                    throws IOException, InterruptedException
    {
        zoo = new ZooKeeper( host, 5000, new Watcher()
        {
            public void process( WatchedEvent we )
            {
                if ( we.getState() == KeeperState.SyncConnected )
                {
                    connectedSignal.countDown();
                }
            }
        } );
        connectedSignal.await();
        return zoo;
    }

    public void close()
                    throws InterruptedException
    {
        zoo.close();
    }
}
