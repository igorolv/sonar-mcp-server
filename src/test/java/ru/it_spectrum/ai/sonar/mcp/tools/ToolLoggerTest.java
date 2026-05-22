package ru.it_spectrum.ai.sonar.mcp.tools;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import ru.it_spectrum.ai.sonar.mcp.service.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolLoggerTest {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ToolLoggerTest.class);

    @Test
    void returnsResultOnHappyPath() {
        assertThat(ToolLogger.run(LOG, "x", () -> 42)).isEqualTo(42);
    }

    @Test
    void passesThroughResourceNotFound() {
        ResourceNotFoundException original = new ResourceNotFoundException("issue X not found");
        assertThatThrownBy(() -> ToolLogger.run(LOG, "x", () -> {
            throw original;
        })).isSameAs(original);
    }

    @Test
    void passesThroughIllegalArgument() {
        IllegalArgumentException original = new IllegalArgumentException("bad input");
        assertThatThrownBy(() -> ToolLogger.run(LOG, "x", () -> {
            throw original;
        })).isSameAs(original);
    }

    @Test
    void wrapsNpeWithNullMessageIntoMeaningfulException() {
        NullPointerException npe = new NullPointerException();
        assertThat(npe.getMessage()).isNull();

        assertThatThrownBy(() -> ToolLogger.run(LOG, "myTool", () -> {
            throw npe;
        }))
                .isInstanceOf(RuntimeException.class)
                .hasCause(npe)
                .hasMessageContaining("myTool failed")
                .hasMessageContaining("NullPointerException");
    }

    @Test
    void unwrapsNestedRootCause() {
        IllegalStateException root = new IllegalStateException("Sonar returned 503");
        RuntimeException wrapper = new RuntimeException("wrapped", root);

        assertThatThrownBy(() -> ToolLogger.run(LOG, "myTool", () -> {
            throw wrapper;
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("myTool failed")
                .hasMessageContaining("IllegalStateException")
                .hasMessageContaining("Sonar returned 503");
    }

    @Test
    void wrappedMessageIsNonNull() {
        NullPointerException npe = new NullPointerException();
        Throwable thrown = catchThrowable(() -> ToolLogger.run(LOG, "t", () -> {
            throw npe;
        }));
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).isNotNull();
    }

    private static Throwable catchThrowable(Runnable r) {
        try {
            r.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }
}
