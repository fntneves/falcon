package pt.haslab.falcon.examples.zookeeper;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.log4j.Logger;

public class ZKDelete
{
    static Logger log = Logger.getLogger( ZKDelete.class.toString() );

    private ZooKeeper zk;

    public ZKDelete( ZooKeeper conn )
    {
        this.zk = conn;
    }

    // Method to check existence of znode and its status, if znode is available.
    public void delete( String path )
                    throws KeeperException, InterruptedException
    {
        zk.delete( path, zk.exists( path, true ).getVersion() );
    }

    public void main( String[] args )
    {
        String path = "/MyFirstZnode"; //Assign path to the znode

        try
        {
            delete( path ); //delete the node with the specified path
            System.out.println( "ZK Delete" );
        }
        catch ( Exception e )
        {
            log.info(e.getMessage()); // catches error messages
        }
    }
}
