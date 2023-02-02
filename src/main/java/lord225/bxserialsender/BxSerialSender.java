package lord225.bxserialsender;

import com.fazecast.jSerialComm.SerialPort;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public final class BxSerialSender extends JavaPlugin {
    private final ConcurrentMap<String, ConcurrentMap<Byte, String>> callbacks = new java.util.concurrent.ConcurrentHashMap<>();
    private final ConcurrentMap<String, SerialPort> ports = new java.util.concurrent.ConcurrentHashMap<>();

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
                            // go through all the serial ports in callbacks
                            callbacks.forEach(
                                    (port, map) -> {
                                        // go through all the commands in callbacks
                                        SerialPort comPort = ports.get(port);

                                        if (comPort.isOpen()) {
                                            if (comPort.bytesAvailable() != 0) {

                                                byte[] readBuffer = new byte[comPort.bytesAvailable()];
                                                int numRead = comPort.readBytes(readBuffer, readBuffer.length);

                                                server.getLogger().info("Red bytes " + numRead + " " + Arrays.toString(readBuffer));


                                                map.forEach(
                                                        (command, callback) -> {
                                                            for (int i = 0; i < numRead; i++) {
                                                                if (readBuffer[i] == command) {
                                                                    System.out.println("Executing command " + callback);
                                                                    server.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                                                                        server.dispatchCommand(server.getConsoleSender(), callback);
                                                                    });

                                                                }
                                                            }
                                                        }
                                                );
                                            }
                                        }
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
        if (command.getName().equalsIgnoreCase("serialsend")) {
            // /serialsend <COM6> <data>

            // get the port from the first argument
            String port = args[0];

            // get data
            String data = Arrays.stream(args).skip(1).reduce("", (a, b) -> a + " " + b);

            // get the comPort from the ports
            SerialPort comPort = ports.get(port);

            if (comPort == null) {
                // if it doesn't exist, return with an error
                sender.sendMessage("Port " + port + " is not open");
                return true;
            }

            // send data
            int sendAmount = comPort.writeBytes(data.getBytes(), data.length());

            sender.sendMessage("Sent " + sendAmount + " bytes");

            return true;
        }

        if (command.getName().equalsIgnoreCase("serialopen")) {
            // /serialopen <COM6> <9600>

            // get COM port
            String comPort = args[0];

            // get baud rate
            int baudRate = Integer.parseInt(args[1]);

            // check if port is in ports
            if (ports.containsKey(comPort)) {
                sender.sendMessage("Proszę spierdalaj");
                return true;
            }

            SerialPort comPortObject = SerialPort.getCommPort(comPort);

            comPortObject.setComPortParameters(baudRate, 8, 1, 0);

            if (comPortObject.isOpen()) {
                sender.sendMessage("Proszę spierdalać");
                return true;
            }

            try {
                if (comPortObject.openPort()) {
                    sender.sendMessage("Port " + comPort + " opened successfully.");
                    ports.put(comPort, comPortObject);
                } else {
                    sender.sendMessage("Unable to open port " + comPort + ".");
                }
            } catch (Exception e) {
                sender.sendMessage("Unable to open port " + comPort + ".");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("serialclose")) {
            // /serialclose <COM6>

            // get COM port
            String comPort = args[0];

            // check if port is in ports
            if (!ports.containsKey(comPort)) {
                sender.sendMessage("Port " + comPort + " is not open");
                return true;
            }

            // get the comPort from the ports
            SerialPort comPortObject = ports.get(comPort);

            // close the port
            if (comPortObject.closePort()) {
                sender.sendMessage("Port " + comPort + " closed successfully.");
            } else {
                sender.sendMessage("Unable to close port " + comPort + ".");
            }

            // remove the port from the ports
            ports.remove(comPort);

            return true;
        }

        if (command.getName().equalsIgnoreCase("serialaddcallback")) {
            // /serialaddcallback <COM6> <byte> <command>

            // get COM port
            String comPort = args[0];

            // get byte
            Byte byteToCheck = Byte.parseByte(args[1]);

            // get command
            String data = Arrays.stream(args).skip(2).reduce("", (a, b) -> a + " " + b).trim();

            // get port from ports
            var comPortObject = ports.get(comPort);

            if (comPortObject == null) {
                sender.sendMessage("Port " + comPort + " is not open");
                return true;
            }

            callbacks.computeIfAbsent(comPort, k -> new java.util.concurrent.ConcurrentHashMap<>()).put(byteToCheck, data);

            sender.sendMessage("Added callback " + byteToCheck + " with command " + data);
        }

        if (command.getName().equalsIgnoreCase("serialclearcallbacks")) {
            // /serialclearcallbacks <COM6>

            // get COM port
            String comPort = args[0];

            // remove all callbacks from comPort

            ConcurrentMap<Byte, String> mappings = callbacks.remove(comPort);

            sender.sendMessage(mappings.size() + " callback cleared");

            return true;
        }

        return false;
    }
}
