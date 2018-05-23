package pt.haslab.taz.causality;

import pt.haslab.taz.events.SocketEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a causal pair (SND,RCV). It also supports cases of message partitioning
 * in which either the complete SND or the RCV procedure is split into multiple smaller-sized operations.
 */
public class SocketCausalPair
{
    private List<SocketEvent> sndList;

    private List<SocketEvent> rcvList;

    public SocketCausalPair()
    {
        sndList = new ArrayList<SocketEvent>(  );
        rcvList = new ArrayList<SocketEvent>(  );
    }

    public SocketCausalPair( List<SocketEvent> sndList, List<SocketEvent> rcvList )
    {
        this.sndList = sndList;
        this.rcvList = rcvList;
    }

    public List<SocketEvent> getSndList()
    {
        return sndList;
    }

    public void setSndList( List<SocketEvent> sndList )
    {
        this.sndList = sndList;
    }

    public List<SocketEvent> getRcvList()
    {
        return rcvList;
    }

    public void setRcvList( List<SocketEvent> rcvList )
    {
        this.rcvList = rcvList;
    }

    public void addSnd(SocketEvent snd)
    {
        this.sndList.add( snd );
    }

    public void addRcv(SocketEvent rcv)
    {
        this.rcvList.add( rcv );
    }

    public SocketEvent getSnd()
    {
        if(this.sndList.isEmpty())
            return null;

        return sndList.get( 0 );
    }

    public SocketEvent getSnd(int index)
    {
        if(this.sndList.isEmpty())
            return null;

        return sndList.get( index );
    }

    public SocketEvent getRcv()
    {
        if(this.rcvList.isEmpty())
            return null;

        return rcvList.get( 0 );
    }

    public SocketEvent getRcv(int index)
    {
        if(this.rcvList.isEmpty())
            return null;

        return rcvList.get( index );
    }

    /**
     * Indicates whether the message sending was split across multiple SND events.
     * @return flag indicating whether SND is partitioned.
     */
    public boolean hasSndPartioned()
    {
        return this.sndList.size() > 1;
    }

    /**
     * Indicates whether the message reception was split across multiple RCV events.
     * @return flag indicating whether RCV is partitioned.
     */
    public boolean hasRcvPartioned()
    {
        return this.rcvList.size() > 1;
    }

    /**
     *  For cases in which the message has a partitioned sending or reception, the method
     *  ensures that the overall size (in bytes) of the SND and RCV events is identical.
     */
    public void recomputeSize()
    {
        if( hasSndPartioned() )
        {
            int sndSize = 0;
            for( SocketEvent sendEvent : sndList )
            {
                sndSize += sendEvent.getSize();
            }

            rcvList.get( 0 ).setSize( sndSize );
        }
        else if( hasRcvPartioned() )
        {
            int rcvSize = 0;
            for( SocketEvent rcvEvent : rcvList )
            {
                rcvSize += rcvEvent.getSize();
            }

            sndList.get( 0 ).setSize( rcvSize  );
        }
    }

    /**
     * Combines partitioned SNDs or RCVs into a single coarse-grained event
     */
    public void aggregatePartitionedMessages()
    {
        if( hasSndPartioned() )
        {
            // increase first SND event's size with the sum of the bytes of the remaining SNDs
            SocketEvent firstSnd = sndList.get( 0 );
            int extraBytes = firstSnd.getSize();
            for( int i = 1; i < sndList.size() ; i++ )
            {
                extraBytes += sndList.get( i ).getSize();
            }
            firstSnd.setSize( extraBytes );

            //replace partitioned SNDs with a single coarse-grained one
            sndList.clear();
            sndList.add( firstSnd );
        }
        else if ( hasRcvPartioned() )
        {
            // increase first RCV event's size with the sum of the bytes of the remaining RCVs
            SocketEvent firstRcv = rcvList.get( 0 );
            int extraBytes = firstRcv.getSize();
            for( int i = 1; i < rcvList.size() ; i++ )
            {
                extraBytes += rcvList.get( i ).getSize();
            }
            firstRcv.setSize( extraBytes );

            //replace partitioned RCVs with a single coarse-grained one
            rcvList.clear();
            rcvList.add( firstRcv );
        }
    }

    @Override
    public String toString()
    {
        int sndSize = 0;
        int rcvSize = 0;
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append( sndList.get( 0 ).getMessageId() + " => [ " );
        for(SocketEvent snd : sndList)
        {
            sndSize += snd.getSize();
            strBuilder.append( snd.toString() + " " );
        }
        strBuilder.append( "] -> [" );
        for(SocketEvent rcv : rcvList)
        {
            rcvSize += rcv.getSize();
            strBuilder.append( rcv.toString() + " " );
        }
        strBuilder.append( "] ("+sndSize+" / "+rcvSize+")" );

        return strBuilder.toString();
    }

}
