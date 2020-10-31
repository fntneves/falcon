package pt.haslab.taz.causality;

import java.util.ArrayList;
import java.util.List;
import pt.haslab.taz.events.SocketEvent;

/**
 * Represents a causal pair (SND,RCV) for a given message exchanged between two processes. The causal pair is defined
 * as a list of SND events and a list of RCV events to support cases of message partitioning in which
 * the sending / reception procedure is split into multiple smaller-sized operations.
 */
public class MessageCausalPair
{
    private List<SocketEvent> sndList;

    private List<SocketEvent> rcvList;

    private int sndBytes;

    private int rcvBytes;

    public MessageCausalPair()
    {
        sndList = new ArrayList<SocketEvent>();
        rcvList = new ArrayList<SocketEvent>();
        sndBytes = 0;
        rcvBytes = 0;
    }

    public MessageCausalPair( List<SocketEvent> sndList, List<SocketEvent> rcvList )
    {
        this.sndList = sndList;
        this.rcvList = rcvList;
        this.sndBytes = sndList
                .stream()
                .mapToInt(snd -> snd.getSize())
                .sum();
        this.rcvBytes = rcvList
                .stream()
                .mapToInt(rcv -> rcv.getSize())
                .sum();
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

    public void addSnd( SocketEvent snd )
    {
        this.sndList.add( snd );
        this.sndBytes += snd.getSize();
    }

    public void addRcv( SocketEvent rcv )
    {
        this.rcvList.add( rcv );
        this.rcvBytes += rcv.getSize();
    }

    public int getSndBytes() {
        return sndBytes;
    }

    public void setSndBytes(int sndBytes) {
        this.sndBytes = sndBytes;
    }

    public int getRcvBytes() {
        return rcvBytes;
    }

    public void setRcvBytes(int rcvBytes) {
        this.rcvBytes = rcvBytes;
    }

    public SocketEvent getSnd( int index )
    {
        if ( this.sndList.isEmpty() )
            return null;

        return sndList.get( index );
    }

    public SocketEvent getRcv( int index )
    {
        if ( this.rcvList.isEmpty() )
            return null;

        return rcvList.get( index );
    }

    /**
     * Indicates whether the message sending was split across multiple SND events.
     *
     * @return flag indicating whether SND is partitioned.
     */
    public boolean hasSndPartioned()
    {
        return this.sndList.size() > 1;
    }

    /**
     * Indicates whether the message reception was split across multiple RCV events.
     *
     * @return flag indicating whether RCV is partitioned.
     */
    public boolean hasRcvPartioned()
    {
        return this.rcvList.size() > 1;
    }

    /**
     * Combines partitioned SNDs or RCVs into a single coarse-grained event
     */
    public void aggregatePartitionedMessages()
    {
        if ( hasSndPartioned() )
        {
            // increase first SND event's size with the sum of the bytes of the remaining SNDs
            SocketEvent firstSnd = sndList.get( 0 );
            int extraBytes = firstSnd.getSize();
            for ( int i = 1; i < sndList.size(); i++ )
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
            for ( int i = 1; i < rcvList.size(); i++ )
            {
                extraBytes += rcvList.get( i ).getSize();
            }
            firstRcv.setSize( extraBytes );

            //replace partitioned RCVs with a single coarse-grained one
            rcvList.clear();
            rcvList.add( firstRcv );
        }
    }

    public boolean hasRcvBytesToMatch() {
        return rcvBytes > sndBytes;
    }

    public boolean hasSndBytesToMatch() {
        return sndBytes > rcvBytes;
    }

    public boolean isFinished() {
        return !this.sndList.isEmpty() && sndBytes == rcvBytes;
    }

    @Override
    public String toString()
    {
        int sndSize = 0;
        int rcvSize = 0;

        if (sndList.isEmpty() && rcvList.isEmpty()) {
            return "[]";
        }

        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(  "[ " );
        for ( SocketEvent snd : sndList )
        {
            sndSize += snd.getSize();
            strBuilder.append( snd.toString() + " " );
        }
        strBuilder.append( "] -> [ " );
        for ( SocketEvent rcv : rcvList )
        {
            rcvSize += rcv.getSize();
            strBuilder.append( rcv.toString() + " " );
        }
        strBuilder.append( "] (" + sndSize + " / " + rcvSize + ")" );

        return strBuilder.toString();
    }

}
