package utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FileRunner {

    /** Restart using the same JVM and arguments, after the current process exits. */
    public static void restartServer(String[] arguments) throws IOException {
        String executable = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows")
                ? "java.exe" : "java";
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", executable).toString());
        command.addAll(java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Restart.class.getName());
        command.add(Long.toString(ProcessHandle.current().pid()));
        command.addAll(List.of(arguments));
        new ProcessBuilder(command).inheritIO().start();
    }

    public static class Restart {
        public static void main(String[] arguments) throws Exception {
            long pid = Long.parseLong(arguments[0]);
            ProcessHandle parent = ProcessHandle.of(pid).orElse(null);
            if (parent != null) {
                parent.onExit().get(60, java.util.concurrent.TimeUnit.SECONDS);
            }
            server.ServerManager.main(java.util.Arrays.copyOfRange(arguments, 1, arguments.length));
        }
    }
}
