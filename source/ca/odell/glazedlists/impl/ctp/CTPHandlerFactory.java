/**
 * Glazed Lists
 * http://glazedlists.dev.java.net/
 *
 * COPYRIGHT 2003 O'DELL ENGINEERING LTD.
 */
package ca.odell.glazedlists.impl.ctp;

import java.util.*;
import java.nio.*;

/**
 * The CTPHandlerFactory provides a factory to handle incoming connections.
 *
 * @author <a href="mailto:jesse@odel.on.ca">Jesse Wilson</a>
 */
public interface CTPHandlerFactory {
    
    /**
     * Upon a connect, a CTPHandler is required to handle the data of this connection.
     * The returned CTPHandler will be delegated to handle the connection's data.
     */
    public CTPHandler constructHandler();
}
