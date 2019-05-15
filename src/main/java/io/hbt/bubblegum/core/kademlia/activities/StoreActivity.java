package io.hbt.bubblegum.core.kademlia.activities;

import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;

import java.util.Set;


/**
 * Implementation of the compound store operation.
 */
public class StoreActivity extends SystemActivity {

    final private String key;
    final private byte[] value;

    /**
     * Constructor.
     * @param localNode The owning BubblegumNode.
     * @param key The key being stored against.
     * @param value The value being stored.
     */
    public StoreActivity(BubblegumNode localNode, String key, byte[] value) {
        super(localNode);
        this.key = key;
        this.value = value;
    }

    /**
     * Run the operation's logic.
     */
    @Override
    public void run() {
        super.run();
        if(this.aborted) {
            this.onFail();
            return;
        }

        try {
            NodeID storeIdentifier = new NodeID(this.key);
            LookupActivity lookupActivity = new LookupActivity(this.localNode, storeIdentifier, 5, false);
            lookupActivity.run();

            if(lookupActivity.getComplete() && lookupActivity.getSuccess()) {

                Set<RouterNode> results = lookupActivity.getClosestNodes();
                if(!results.isEmpty()) {

                    int numAccepted = 0;
                    for(RouterNode node : results) {
                        PrimitiveStoreActivity storeActivity = new PrimitiveStoreActivity(this.localNode, node, this.key, this.value);
                        storeActivity.run();
                        if(storeActivity.getComplete() && storeActivity.getSuccess()) {
                            numAccepted++;
                        }
                    }

                    if(numAccepted > 0) this.onSuccess();
                    else this.onFail();
                }
                else {
                    this.onFail("STORE: LookupActivity returned no results");
                }
            }

            else {
                this.onFail("STORE: LookupActivity failed");
            }

        } catch (MalformedKeyException e) {
            this.onFail("STORE: MalformedKey");
        }
    }

} // end StoreActivity class
