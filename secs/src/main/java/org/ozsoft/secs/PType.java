package org.ozsoft.secs;

/**
 * Message protocol (PType).
 * 
 * @author Oscar Stigter
 */
public enum PType {
    
    /** SECS-II protocol. */
    SECS_II(0),
    
    /** Unknown protocol. */
    UNKNOWN(-1),
    
    ;
    
    private int value;
    
    PType(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public static PType parse(int value) {
        for (PType pType : values()) {
            if (pType.value == value) {
                return pType;
            }
        }
        return PType.UNKNOWN;
    }

}