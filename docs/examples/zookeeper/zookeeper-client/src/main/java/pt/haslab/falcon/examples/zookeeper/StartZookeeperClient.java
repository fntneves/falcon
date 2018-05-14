package pt.haslab.falcon.examples.zookeeper;

import org.apache.zookeeper.ZooKeeper;

public class StartZookeeperClient
{
    public static void main( final String[] args )
    {
        try
        {
            final String serverIP = args[0];
            final ZooKeeper conn = new ZooKeeperConnection().connect( serverIP );

            ZKExists zkExists = new ZKExists( conn );
            zkExists.main( new String[] { serverIP } );

            ZKCreate zkCreate = new ZKCreate( conn );
            zkCreate.main( new String[] { serverIP } );

            zkExists.main( new String[] { serverIP } );

            ZKDelete zkDelete = new ZKDelete( conn );
            zkDelete.main( new String[] { serverIP } );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
}
