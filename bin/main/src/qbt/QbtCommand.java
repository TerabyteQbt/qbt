package qbt;

import misc1.commons.options.OptionsResults;

public abstract class QbtCommand<O extends QbtCommandOptions> {
    public abstract Class<O> getOptionsClass();

    public abstract HelpTier getHelpTier();
    public abstract String getDescription();

    public boolean isProgrammaticOutput() {
        return false;
    }

    public abstract int run(OptionsResults<? extends O> options) throws Exception;
}
