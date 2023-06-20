package lord225.bxserialsender;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class BxUDPSender extends JavaPlugin {
    private final ConcurrentMap<String, ConcurrentMap<Byte, String>> callbacks = new java.util.concurrent.ConcurrentHashMap<>();
    private final ConcurrentMap<String, DatagramSocket> sockets = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        System.out.println("BxSerialSender is enabled!");

        var plugin = this;

        Runnable listener = new Runnable() {
            private final Server server = getServer();

            @Override
            public void run() {
                while (true) {
                    try {
                        while (true) {
                            // go through all the sockets in callbacks
                            callbacks.forEach(
                                    (address, map) -> {
                                        // go through all the commands in callbacks
                                        DatagramSocket socket = sockets.get(address);

                                        byte[] receiveBuffer = new byte[1024];
                                        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                                        try {
                                            socket.receive(receivePacket);
                                        } catch (IOException e) {
                                            server.getLogger().info("Error receiving packet: " + e.getMessage());
                                        }
                                        byte[] readBuffer = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());

                                        server.getLogger().info("Received bytes: " + Arrays.toString(readBuffer));

                                        map.forEach(
                                                (command, callback) -> {
                                                    for (int i = 0; i < readBuffer.length; i++) {
                                                        if (readBuffer[i] == command) {
                                                            System.out.println("Executing command: " + callback);
                                                            server.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                                                                server.dispatchCommand(server.getConsoleSender(), callback);
                                                            });
                                                        }
                                                    }
                                                }
                                        );
                                    }
                            );
                            Thread.sleep(100);
                        }
                    } catch (Exception e) {
                        System.out.println("Error in listener thread: " + e.getMessage());
                    }
                }
            }
        };

        Thread thread = new Thread(listener);
        System.out.println("Starting reader thread");
        thread.start();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("updsend")) {
            // /updsend <address> <port> <data>

            // get the address from the first argument
            String address = args[0];

            // get the port from the second argument
            int port = Integer.parseInt(args[1]);

            // get data
            String data = Arrays.stream(args).skip(2).reduce("", (a, b) -> a + " " + b);

            // get the socket from the sockets
            DatagramSocket socket = sockets.get(address);

            if (socket == null) {
                // if it doesn't exist, return with an error
                sender.sendMessage("Socket " + address + " is not open");
                return true;
            }

            try {
                // send data
                byte[] sendData = data.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(address), port);
                socket.send(sendPacket);

                sender.sendMessage("Sent " + sendData.length + " bytes");
            } catch (IOException e) {
                sender.sendMessage("An error occurred while sending the UDP packet: " + e.getMessage());
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("udpopen")) {
            // /udpopen <address> <port>

            // get the address from the first argument
            String address = args[0];

            // get the port from the second argument
            int port = Integer.parseInt(args[1]);

            // check if socket already exists
            if (sockets.containsKey(address)) {
                sender.sendMessage("Socket " + address + " is already open");
                return true;
            }

            try {
                // create a new DatagramSocket
                DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName(address));
                sockets.put(address, socket);
                sender.sendMessage("Socket " + address + " opened successfully on port " + port);
            } catch (SocketException | UnknownHostException e) {
                sender.sendMessage("Unable to open socket at " + address + ":" + port + ": " + e.getMessage());
            }

            return true;
        }

        if (command.getName().equalsIgnoreCase("udpclose")) {
            // /udpclose <address>

            // get the address from the first argument
            String address = args[0];

            // check if socket exists
            if (!sockets.containsKey(address)) {
                sender.sendMessage("Socket " + address + " is not open");
                return true;
            }

            // get the socket from the sockets
            DatagramSocket socket = sockets.get(address);

            // close the socket
            socket.close();
            sender.sendMessage("Socket " + address + " closed successfully.");

            // remove the socket from the sockets
            sockets.remove(address);

            return true;
        }

        if (command.getName().equalsIgnoreCase("udpaddcallback")) {
            // /udpaddcallback <address> <byte> <command>

            // get the address from the first argument
            String address = args[0];

            // get byte
            byte byteToCheck = Byte.parseByte(args[1]);

            // get command
            String data = Arrays.stream(args).skip(2).reduce("", (a, b) -> a + " " + b).trim();

            // check if socket exists
            if (!sockets.containsKey(address)) {
                sender.sendMessage("Socket " + address + " is not open");
                return true;
            }

            ConcurrentMap<Byte, String> callbackMap = callbacks.computeIfAbsent(address, k -> new ConcurrentHashMap<>());
            callbackMap.put(byteToCheck, data);

            sender.sendMessage("Added callback " + byteToCheck + " with command " + data);
            return true;
        }

        if (command.getName().equalsIgnoreCase("udpclearcallbacks")) {
            // /udpclearcallbacks <address>

            // get the address from the first argument
            String address = args[0];

            // remove all callbacks for the address
            ConcurrentMap<Byte, String> mappings = callbacks.remove(address);

            sender.sendMessage((mappings != null ? mappings.size() : 0) + " callback(s) cleared");
            return true;
        }

        return false;
    }
}
