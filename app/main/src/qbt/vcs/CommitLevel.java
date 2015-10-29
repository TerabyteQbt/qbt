package qbt.vcs;

// Somewhat of a crummy leak of git specifics.  If we add a VCS with less
// levels it can ignore some and if we add one with more we can give up and
// just make this a bunch of numbers...
public enum CommitLevel {
    STAGED,
    MODIFIED,
    UNTRACKED,
    ;
}
