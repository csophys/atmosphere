/*
* Copyright 2012 Jeanfrancois Arcand
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy of
* the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations under
* the License.
*/
package org.atmosphere.container.version;

import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.websocket.WebSocketAdapter;
import org.atmosphere.websocket.WebSocketResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GrizzlyWebSocket extends WebSocketAdapter {

    private static final Logger logger = LoggerFactory.getLogger(Jetty8WebSocket.class);
    private final com.sun.grizzly.websockets.WebSocket webSocket;
    private final AtomicBoolean firstWrite = new AtomicBoolean(false);

    public GrizzlyWebSocket(com.sun.grizzly.websockets.WebSocket webSocket, AtmosphereConfig config) {
        super(config);
        this.webSocket = webSocket;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeError(int errorCode, String message) throws IOException {
        if (!firstWrite.get()) {
            logger.debug("The WebSocket handshake succeeded but the dispatched URI failed {}:{}. " +
                    "The WebSocket connection is still open and client can continue sending messages.", message, errorCode);
        } else {
            logger.debug("{} {}", errorCode, message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void redirect(String location) throws IOException {
        logger.error("redirect not supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(String data) throws IOException {
        if (binaryWrite) {
            webSocket.send(webSocketResponseFilter.filter(data).getBytes(resource().getResponse().getCharacterEncoding()));
        } else {
            webSocket.send(webSocketResponseFilter.filter(data));
        }
        lastWrite = System.currentTimeMillis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] data) throws IOException {
        if (binaryWrite) {
            webSocket.send(webSocketResponseFilter.filter(data));
        } else {
            webSocket.send(webSocketResponseFilter.filter(new String(data)));
        }
        lastWrite = System.currentTimeMillis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        if (binaryWrite) {
            if (!WebSocketResponseFilter.NoOpsWebSocketResponseFilter.class.isAssignableFrom(webSocketResponseFilter.getClass())) {
                byte[] b = webSocketResponseFilter.filter(data, offset, length);
                webSocket.send(b);
            } else {
                webSocket.send(Arrays.copyOfRange(data, offset, length));
            }
        } else {
            webSocket.send(webSocketResponseFilter.filter(new String(data, offset, length)));
        }
        lastWrite = System.currentTimeMillis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        webSocket.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
    }

}
