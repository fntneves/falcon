package pt.haslab.falcon.examples.zookeeper;

import org.apache.zookeeper.ZooKeeper;
import org.apache.log4j.Logger;

public class ZKExists
{
    static Logger log = Logger.getLogger( ZKDelete.class.toString() );

    private ZooKeeper zk;

    public ZKExists( ZooKeeper conn )
    {
        this.zk = conn;
    }

    public void main( String[] args )
    {

        String path = "/MyFirstZnode"; // Assign znode to the specified path

        try
        {
            zk.exists( path, true );
            System.out.println( "ZK Exists" );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
}