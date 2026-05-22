package ru.it_spectrum.ai.sonar.mcp.tools;

import org.slf4j.Logger;
import ru.it_spectrum.ai.sonar.mcp.service.ResourceNotFoundException;

import java.util.function.Supplier;

public final class ToolLogger {

    private ToolLogger() {}

    public static void completed(Logger log, String toolName, long startNanos) {
        long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
        log.info("Tool completed: {} (elapsed: {}ms)", toolName, elapsed);
    }

    public static void failed(Logger log, String toolName, long startNanos, String error) {
        long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
        log.warn("Tool failed: {} (elapsed: {}ms): {}", toolName, elapsed, error);
    }

    /**
     * Runs the tool body, times it, and guarantees that any exception thrown to the caller
     * carries a non-null message. Spring AI MCP's tool callback surfaces the root cause's
     * {@code getMessage()} verbatim to the client; a null message there shows up as a literal
     * "null" line, which is useless for diagnosis.
     *
     * <p>Pass-through cases (already specific and useful — left unwrapped):
     * <ul>
     *   <li>{@link ResourceNotFoundException} and subclasses — domain "not found" errors</li>
     *   <li>{@link IllegalArgumentException} — caller-mistake validation</li>
     * </ul>
     * Anything else is rethrown as a {@link RuntimeException} whose message includes the tool
     * name and a description of the root cause (class simple name + message, or just the class
     * simple name when the root cause has no message).
     */
    public static <T> T run(Logger log, String toolName, Supplier<T> action) {
        long start = System.nanoTime();
        try {
            T result = action.get();
            completed(log, toolName, start);
            return result;
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            failed(log, toolName, start, describe(e));
            throw e;
        } catch (RuntimeException e) {
            Throwable root = rootCause(e);
            String description = describe(root);
            failed(log, toolName, start, description);
            throw new RuntimeException(toolName + " failed: " + description, e);
        }
    }

    private static Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }

    private static String describe(Throwable t) {
        String msg = t.getMessage();
        String type = t.getClass().getSimpleName();
        if (msg == null || msg.isBlank()) {
            return type;
        }
        return type + ": " + msg;
    }
}
