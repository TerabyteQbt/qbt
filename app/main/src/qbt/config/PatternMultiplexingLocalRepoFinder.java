package qbt.config;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import qbt.repo.LocalRepoAccessor;
import qbt.tip.RepoTip;

public class PatternMultiplexingLocalRepoFinder implements LocalRepoFinder {
    private final List<Pair<Pattern, LocalRepoFinder>> delegates;

    public PatternMultiplexingLocalRepoFinder(List<Pair<String, LocalRepoFinder>> delegates) {
        ImmutableList.Builder<Pair<Pattern, LocalRepoFinder>> b = ImmutableList.builder();
        for(Pair<String, LocalRepoFinder> e : delegates) {
            b.add(Pair.of(Pattern.compile(e.getLeft()), e.getRight()));
        }
        this.delegates = b.build();
    }

    private Pair<LocalRepoFinder, RepoTip> match(RepoTip repo) {
        for(Pair<Pattern, LocalRepoFinder> e : delegates) {
            String repoString = repo.toString();
            Matcher m = e.getLeft().matcher(repoString);
            if(!m.matches()) {
                continue;
            }

            String replacementString;
            int groupCount = m.groupCount();
            switch(m.groupCount()) {
                case 0:
                    replacementString = repoString;
                    break;

                case 1:
                    replacementString = m.group(1);
                    break;

                default:
                    throw new IllegalArgumentException("Pattern " + e.getLeft() + " has too many capture groups (" + groupCount + ")");
            }
            RepoTip replacement = RepoTip.TYPE.parseRequire(replacementString);

            return Pair.of(e.getRight(), replacement);
        }

        return null;
    }

    @Override
    public LocalRepoAccessor findLocalRepo(RepoTip repo) {
        Pair<LocalRepoFinder, RepoTip> match = match(repo);
        if(match == null) {
            return null;
        }
        return match.getLeft().findLocalRepo(match.getRight());
    }

    @Override
    public LocalRepoAccessor createLocalRepo(RepoTip repo) {
        Pair<LocalRepoFinder, RepoTip> match = match(repo);
        if(match == null) {
            throw new IllegalArgumentException("Repo " + repo + " matched no configured patterns");
        }
        return match.getLeft().createLocalRepo(match.getRight());
    }
}
