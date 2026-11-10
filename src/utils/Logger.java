package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    public static final String RESET = "\033[0m";

    public static final String RED = "\033[4;31m";
    public static final String GREEN = "\033[0;32m";
    public static final String PURPLE = "\033[0;35m";
    public static final String BLUE = "\033[0;34m";
    public static final String YELLOW = "\u001B[33m";

    public static void log(String text) {
        System.out.print(text);
    }

    public static void logln(String text) {
        System.out.println(text);
    }

    public static void log(String color, String text) {
        System.out.print(color + text + RESET);
    }

    public static void logln(String color, String text) {
        System.out.println(color + text + RESET);
    }

    public static void success(String text) {
        System.out.print(GREEN + text + RESET);
    }

    public static void successln(String text) {
        System.out.println(GREEN + text + RESET);
    }

    public static void warning(String text) {
        System.out.print(YELLOW + text + RESET);
    }

    public static void warningln(String text) {
        System.out.println(YELLOW + text + RESET);
    }

    public static void error(String text) {
        System.out.print(RED + text + RESET);
    }

    public static void errorln(String text) {
        System.out.println(RED + text + RESET);
    }

    public static void primary(String text) {
        System.out.print(BLUE + text + RESET);
    }

    public static void primaryln(String text) {
        System.out.println(BLUE + text + RESET);
    }

    public static void logException(Class<?> clazz, Exception ex, String... log) {
        try {
            if (log != null && log.length > 0) {
                log(PURPLE, log[0] + "\n");
            }

            String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String exceptionDetails = sw.toString();

            Logger.warning("Error in class: ");
            Logger.error(clazz.getName());
            Logger.warning(" - in method: ");
            Logger.error(methodName + "\n");
            Logger.warning("Error details:\n");
            for (String line : exceptionDetails.split("\n")) {
                Logger.error(line + "\n");
            }
            Logger.log("--------------------------------------------------------\n");
        } catch (Exception e) {
            Logger.error("Failed to log exception: " + e.getMessage());
        }
    }

    public static void fileLog(String playerName, String string) {
        new Thread(() -> {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy + HH:mm:ss");
                String timeNow = formatter.format(new Date());
                String logEntry = timeNow + " + " + string;
                writeFile("log/" + playerName + "_log.txt", logEntry);
            } catch (IOException e) {
            }
        }).start();
    }

    private static void writeFile(String filePath, String content) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(file, true); BufferedWriter bw = new BufferedWriter(fw); PrintWriter out = new PrintWriter(bw)) {
            out.println(content);
        }
    }

}
