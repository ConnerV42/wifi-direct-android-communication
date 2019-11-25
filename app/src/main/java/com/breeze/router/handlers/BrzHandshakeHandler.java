package com.breeze.router.handlers;

import android.content.Context;

import com.breeze.MainActivity;
import com.breeze.packets.BrzPacket;
import com.breeze.router.BrzRouter;
import com.breeze.application.BreezeAPI;

public class BrzHandshakeHandler implements BrzRouterHandler {

    private BrzRouter brzRouter;

    public BrzHandshakeHandler(BrzRouter brzRouter) {
        this.brzRouter = brzRouter;
    }
    @Override
    public void handle(BrzPacket packet, String fromEndpointId) {

        //TODO Determine what's coming in on the packet. I think it'd need to be a profile and a node object,
            //...From there we can get any data that we actually need

        //TODO CHECK if user name from is in store already or database (check with the user ID)
            //...isNodeInStore then isNodeInDatabase

        //TODO if they're in the db/store, compare the public key passed in w/ whatever public key is associated w/ the id
            //...compareKeys

        //TODO If its a new user id and new public, add them, send an ack that tells them they're added
            //...addNewUser, addNewChat, sendAddedAck, sendChatInitializedAck

        //TODO If other issues, like public keys not matching, etc TODO figure out later
            //...sendMismatchAck, wait for TODO: MismatchHandler to handle next packet

    }
    @Override
    public boolean handles(BrzPacket.BrzPacketType type) {
        return type == BrzPacket.BrzPacketType.HANDSHAKE_PACKET;
    }
    private boolean isNodeInStore(BrzPacket packet, String endpointId)
    {
        BreezeAPI api = BreezeAPI.getInstance();
        //TODO checks the api for the node being in the store. Checks by endpoint ID (?)
        //OR nodeId passed in from the profile inside the message?
        return false;
    }
    private boolean isNodeInDatabase(BrzPacket packet, String endpointId)
    {
        BreezeAPI api = BreezeAPI.getInstance();
        //TODO checks the api for the node being in the database. Checks by endpoint ID (?)
        //OR nodeId passed in from the profile inside the message?

        //TODO determine if isNodeInDatabase and isNodeInStore should be one method
        return false;
    }
    private boolean compareKeys(BrzPacket packet, String endpointId)
    {
        BreezeAPI api = BreezeAPI.getInstance();
        //TODO compare two keys, one coming from the node in the message

        return false;
    }
    private void addNewUser(BrzPacket packet, String endpointId)
    {
        BreezeAPI api = BreezeAPI.getInstance();

        
    }
    private void addNewBrzChat(BrzPacket packet, String endpointId)
    {
        BreezeAPI api = BreezeAPI.getInstance();
    }
    private void sendAddedAcknowledgement(BrzPacket packet, String endpointId)
    {
        BreezeAPI api = BreezeAPI.getInstance();
    }
    private void sendKeyMismatchAcknowledgement(BrzPacket packet, String endpointId)
    {
        BreezeAPI api = BreezeAPI.getInstance();
    }
    private void sendInitializeChatAcknowledgement(BrzPacket packet, String endpointId)
    {
        BreezeAPI api = BreezeAPI.getInstance();
    }

}
