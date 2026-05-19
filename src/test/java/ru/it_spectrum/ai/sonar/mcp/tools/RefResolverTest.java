package ru.it_spectrum.ai.sonar.mcp.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefResolverTest {

    @Test
    void explicitBranchTakesPrecedenceOverDefault() {
        var ref = RefResolver.resolve("feature/foo", null, "develop");
        assertThat(ref.branch()).isEqualTo("feature/foo");
        assertThat(ref.pullRequest()).isNull();
    }

    @Test
    void pullRequestSetsPullRequestAndClearsBranch() {
        var ref = RefResolver.resolve(null, "1234", "develop");
        assertThat(ref.branch()).isNull();
        assertThat(ref.pullRequest()).isEqualTo("1234");
    }

    @Test
    void pullRequestIsNotSubjectToDefaultBranchFallback() {
        var ref = RefResolver.resolve(null, "1234", "develop");
        assertThat(ref.branch()).isNull();
    }

    @Test
    void noInputsFallBackToDefaultBranch() {
        var ref = RefResolver.resolve(null, null, "develop");
        assertThat(ref.branch()).isEqualTo("develop");
        assertThat(ref.pullRequest()).isNull();
    }

    @Test
    void noInputsAndNoDefaultProducesEmptyRef() {
        var ref = RefResolver.resolve(null, null, null);
        assertThat(ref.branch()).isNull();
        assertThat(ref.pullRequest()).isNull();
        assertThat(ref.isEmpty()).isTrue();
    }

    @Test
    void blankInputsTreatedAsAbsent() {
        var ref = RefResolver.resolve("  ", "  ", "develop");
        assertThat(ref.branch()).isEqualTo("develop");
        assertThat(ref.pullRequest()).isNull();
    }

    @Test
    void bothBranchAndPullRequestAreRejected() {
        assertThatThrownBy(() -> RefResolver.resolve("feature/foo", "1234", "develop"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mutually exclusive");
    }
}
