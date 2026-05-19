package ru.it_spectrum.ai.sonar.mcp.tools;

/**
 * Resolves the (branch, pullRequest) pair passed to branch-aware tools into a normalized form.
 *
 * <p>Rules:
 * <ul>
 *   <li>{@code branch} and {@code pullRequest} are mutually exclusive at the Sonar API level. Passing both
 *       results in an {@link IllegalArgumentException}.</li>
 *   <li>If {@code pullRequest} is given, the resulting {@link Ref} carries it and {@code branch} is null
 *       regardless of any default — pull requests are short-lived and never fall back to a server default.</li>
 *   <li>If only {@code branch} is given, it is returned as is.</li>
 *   <li>If neither is given, {@code defaultBranch} (from {@code SONAR_DEFAULT_BRANCH}) is used; if it is also
 *       missing, the returned {@link Ref} has both fields null and Sonar will use the project's main branch.</li>
 * </ul>
 */
public final class RefResolver {

    private RefResolver() {}

    public record Ref(String branch, String pullRequest) {

        public static final Ref EMPTY = new Ref(null, null);

        public boolean isEmpty() {
            return (branch == null || branch.isBlank())
                    && (pullRequest == null || pullRequest.isBlank());
        }
    }

    public static Ref resolve(String branch, String pullRequest, String defaultBranch) {
        boolean hasBranch = branch != null && !branch.isBlank();
        boolean hasPr = pullRequest != null && !pullRequest.isBlank();
        if (hasBranch && hasPr) {
            throw new IllegalArgumentException(
                    "branch and pullRequest are mutually exclusive; pass only one");
        }
        if (hasPr) {
            return new Ref(null, pullRequest);
        }
        if (hasBranch) {
            return new Ref(branch, null);
        }
        if (defaultBranch != null && !defaultBranch.isBlank()) {
            return new Ref(defaultBranch, null);
        }
        return Ref.EMPTY;
    }
}
