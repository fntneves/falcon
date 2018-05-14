package pt.haslab.falcon.examples.zookeeper;

import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class ZKCreate
{
    static Logger log = Logger.getLogger( ZKDelete.class.toString() );

    private ZooKeeper zk;

    public ZKCreate( ZooKeeper conn )
    {
        zk = conn;
    }

    // Method to create znode in zookeeper ensemble
    public void create( String path, byte[] data )
                    throws
                    KeeperException, InterruptedException
    {
        zk.create( path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                   CreateMode.PERSISTENT );
    }

    public void main( String[] args )
    {

        String path = "/MyFirstZnode"; // Assign path to znode

        byte[] data = "My first zookeeper app".getBytes(); // Declare data

        try
        {
            create( path, data ); // Create the data to the specified path
            System.out.println( "ZK Create" );
        }
        catch ( Exception e )
        {
            log.info( e.getMessage() ); // catches error messages
        }
    }
}
