package org.ozsoft.secs.message;

import org.ozsoft.secs.SecsException;
import org.ozsoft.secs.SecsMessage;
import org.ozsoft.secs.SecsParseException;
import org.ozsoft.secs.format.Data;

/**
 * S1F1 Are You There (R) request message.
 * 
 * @author Oscar Stigter
 */
public class S1F1New extends SecsMessage {

    public static final int STREAM = 1;

    public static final int FUNCTION = 1;
    
    public static final boolean WITH_REPLY = true;
    
    public static final String DESCRIPTION = "Are You There (R)";
    
    @Override
    public void parseData(Data<?> data) throws SecsParseException {
        if (data != null) {
            throw new SecsParseException("No data expected");
        }
    }

    @Override
    public SecsMessage handle(int sessionId, long transactionId) throws SecsException {
        S1F2New s1f2 = new S1F2New();
        s1f2.setModelName(getEquipment().getModelName());
        s1f2.setSoftRev(getEquipment().getSoftRev());
        return s1f2;
    }

    @Override
    public Data<?> getData() throws SecsParseException {
        // No data.
        return null;
    }

}