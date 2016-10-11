package com.eveningoutpost.dexdrip.G5Model;
import com.eveningoutpost.dexdrip.G5Model.TransmitterMessage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthChallengeTxMessage extends TransmitterMessage {
    byte opcode = 0x4;
    byte[] challengeHash;

    public AuthChallengeTxMessage(byte[] challenge) {
        challengeHash = challenge;

        data = ByteBuffer.allocate(9);
        data.put(opcode);
        data.put(challengeHash);

        byteSequence = data.array();
    }
}
