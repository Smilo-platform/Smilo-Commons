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

import java.io.File;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class PeerOutput implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(PeerOutput.class);
    private final Socket socket;

    //Private to mirror InputThread's structure. For OOP model, it makes more sense for a method to simulate 'writing' data (even though it is delayed until the thread writes the data).
    private List<String> outputBuffer;
    private boolean shouldContinue = true;

    private final Object lock = new Object();

    /**
     * Constructor to set class socket variable
     */
    public PeerOutput(Socket socket) {
        this.socket = socket;
        outputBuffer = new ArrayList<>();
    }

    /**
     * Constantly checks outputBuffer for contents, and writes any contents in outputBuffer.
     */
    public void run() {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            while (shouldContinue) {
                LOGGER.trace("LOOP ALIVE FOR " + socket.getInetAddress());
                LOGGER.trace("LOOP SIZE : " + outputBuffer.size() + " for " + socket.getInetAddress());

                List<String> copy;
                synchronized (lock) {
                    while (outputBuffer.isEmpty()) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            LOGGER.trace("Received interruption, quitting PeerOutput loop");
                            return;
                        }
                    }
                    copy = new ArrayList<>(outputBuffer);
                    outputBuffer.clear();
                }
                for (int i = 0; i < copy.size(); i++) {
                    if (copy.get(i).length() > 20) {
                        LOGGER.trace("Sending " + copy.get(i).substring(0, 20) + " to " + socket.getInetAddress() + ":" + socket.getPort());
                    } else {
                        LOGGER.trace("Sending " + copy.get(i) + " to " + socket.getInetAddress() + ":" + socket.getPort());
                    }
                    out.println(copy.get(i));
                }

                Thread.sleep(100);
            }
            LOGGER.error("WHY AM I HERE?");
        } catch (InterruptedException e) {
            LOGGER.info("Peer is interrupted...");
        } catch (Exception e) {
            LOGGER.error("An error has occurred while running the PeerOutput", e);
        }
    }

    /**
     * Technically not writing to the network socket, but instead putting the passed-in data in a buffer to be written to the socket as soon as possible.
     *
     * @param data Data to write
     */
    public void write(String data) {
        File f = new File("writebuffer");
        try {
            PrintWriter out = new PrintWriter(f);
            LOGGER.trace("SENDING: " + data);
            out.close();
        } catch (Exception e) {
            LOGGER.error("Unable to write", e);
        }
        synchronized (lock) {
            outputBuffer.add(data);
            lock.notify();
        }
    }

    /**
     * Stops thread during the next write cycle. I couldn't call it stop() like I wanted to, cause you can't overwrite that method of Thread. :'(
     */
    public void shutdown() {
        shouldContinue = false;
    }
}