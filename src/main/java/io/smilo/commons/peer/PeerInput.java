/*
 * Copyright (c) 2018 Smilo Platform B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smilo.commons.peer;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class PeerInput extends Thread
{
    private static final Logger LOGGER = Logger.getLogger(PeerInput.class);
    private final Socket socket;

    //Private instead of public so that object can control calls to receivedData. Acts as a buffer... the same data shouldn't be read more than once.
    private List<String> receivedData = new ArrayList<>();

    private Object lock = new Object();

    /**
     * Constructor to set class socket variable
     */
    public PeerInput(Socket socket) {
        this.socket = socket;
    }

    /**
     * Constantly reads from the input stream of the socket, and saves any received data to the ArrayList<St
     */
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String input;
            while ((input = in.readLine()) != null) {
                // TODO: check if lock works correctly
                synchronized (lock) {
                    receivedData.add(input);
                }
                LOGGER.trace("RUN(): " + input);
                LOGGER.trace("size: " + receivedData.size());
            }
        } catch (Exception e) {
            LOGGER.warn("Peer " + socket.getRemoteSocketAddress().toString() + " disconnected.");
        }
    }

    /**
     * Doesn't actually 'read data' as that's done asynchronously in the threaded run function.
     * However, readData is an easy way to think about it--as receivedData acts as a buffer, holding received data until the daemon is ready to handle it.
     * Generally, the size of receivedData will be small. However, in some instances (like when downloading many blocks), it can grow quickly.
     *
     * @return ArrayList<String> Data pulled from receivedData
     */
    public List<String> readData() {
        LOGGER.trace("readData() called!");
        LOGGER.trace("We have " + receivedData.size() + " pieces!");
        List<String> copy;
        synchronized (lock) {
            copy = new ArrayList<>(receivedData);
            receivedData.clear();
        }
        return copy;
    }
}
