import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.function.Consumer;

/**
 * CommandExecutor
 * ---------------
 * A small utility that runs native Windows commands through
 * {@code cmd.exe /c <command>} and returns their combined stdout/stderr.
 *
 * All async work is delegated to {@link SwingWorker} so the Swing EDT
 * is never blocked. Blocking calls are only made from
 * {@code doInBackground()}.
 */
public final class CommandExecutor {

    /** No instances — this is a static utility class. */
    private CommandExecutor() { }

    /**
     * Runs a command synchronously and returns its combined output.
     * <p><b>Blocking.</b> Call only from a background thread
     * (e.g. inside {@link SwingWorker#doInBackground()}).
     *
     * @param command the Windows command to execute (without {@code cmd.exe /c})
     * @return combined stdout + stderr as a single string
     * @throws Exception if the process fails to start or is interrupted
     */
    public static String run(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
        pb.redirectErrorStream(true);   // merge stderr into stdout
        Process process = pb.start();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }

        process.waitFor();
        return sb.toString();
    }

    /**
     * Fire-and-forget asynchronous execution.
     * <p>The {@code onSuccess} callback is invoked on the Swing EDT with the
     * command's output. If the command fails, {@code onError} is invoked
     * on the EDT with the underlying cause.
     *
     * @param command   the Windows command to execute
     * @param onSuccess callback receiving the combined output (EDT)
     * @param onError   optional callback receiving the failure cause (EDT);
     *                  may be {@code null}
     * @return the {@link SwingWorker} running the command, so the caller
     *         can track or cancel it if desired
     */
    public static SwingWorker<String, Void> runAsync(
            String command,
            Consumer<String> onSuccess,
            Consumer<Throwable> onError) {

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                // NOTE: fully qualify the outer static method.
                // Writing "run(command)" here would resolve to
                // SwingWorker.run() (no-arg) and fail to compile.
                return CommandExecutor.run(command);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    if (onSuccess != null) onSuccess.accept(result);
                } catch (Exception ex) {
                    Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                    if (onError != null) onError.accept(cause);
                }
            }
        };

        worker.execute();
        return worker;
    }
}