package org.openlumify.core.model.lock;

public interface LeaderListener {
    void isLeader() throws InterruptedException;

    void notLeader() throws InterruptedException;
}
