# QBT - QBT Build Tool

This is the documentation for QBT, a dependency manager and repository stitcher.
QBT helps you build an entire universe of consistent, tested, reproducible,
technology-agnostic software.

# Why QBT?

QBT is a framework you can use to manage a set of consistent versions of many
interconnected software projects, and make changes to those projects while
ensuring none of them break.  QBT is just a tool, but when used correctly it
enables reproducibility, consistency, change management, and trust/auditing.

# Getting Started

To use QBT, first obtain a binary release of QBT from the [QBT
Website](http://qbtbuildtool.com).

Simply place the "qbt" script on your path and you are ready to run QBT
commands.

A "universe" of software is represented by a `qbt-manifest` file.  You can clone
the manifest file of QBT's first author by cloning his meta repository.

    $ git clone https://github.com/AmlingQbt/meta.git

Next, you will need to produce a `qbt-config` file, which typically lives next
to your `qbt-manifest` file (TODO: in a future release this will move to your
homedir probably).

Here is an example `qbt-config` file:


    import com.google.common.collect.ImmutableList;
    import java.nio.file.Paths;
    import qbt.artifactcacher.CompoundArtifactCacher;
    import qbt.artifactcacher.LocalArtifactCacher;
    import qbt.config.CompoundQbtRemoteFinder;
    import qbt.config.FormatLocalRepoFinder;
    import qbt.config.FormatQbtRemoteFinder;
    import qbt.config.MapQbtRemoteFinder;
    import qbt.config.QbtConfig;
    import qbt.pins.SimpleLocalPinsRepo;
    import qbt.remote.FormatQbtRemote;
    import qbt.vcs.VcsRegistry;
    
    def dotQbt = Paths.get(System.getenv("HOME")).resolve(".qbt");
    def gitRemoteVcs = VcsRegistry.getRawRemoteVcs("git");
    def gitLocalVcs = gitRemoteVcs.getLocalVcs();
    
    return new QbtConfig(
    
    // First config argument -- where are my overrides?  My simple choice is
    // "next to meta".  LocalRepoFinder is an interface and it's entirely
    // plausible to implement it in other ways, possibly even in qbt-config
    // itself.
         new FormatLocalRepoFinder(
             gitLocalVcs,
             workspaceRoot.resolve("../%r").toString(),
         ),
    
    // Second config argument -- where are my local pins?  local pins are commits in
    // package repositories that are pointed to by the manifest.  If you push the
    // repository that the manifest file lives in to other people, they can't use it
    // unless they can also get all the commits listed in that manifest, so pins are
    // how QBT accomplishes this.
    // 
    // I put them in my home directory so they're shared between workspaces.  Since
    // this is an immutable, append-only store sharing mostly makes sense.  I notice
    // this is slightly inconsistent specification-wise: most of the rest are
    // formats and this one is root directory that it makes subdirs of.  Should
    // probably go back and change this to format.
         new SimpleLocalPinsRepo(
             gitRemoteVcs,
             dotQbt.resolve("pins/v1"),
         ),
    
    // Third config argument -- where are my remotes?  This is just a
    // programmatic mapping from mere string to full-on QbtRemote platform
    // object.
    // 
    // The first half specifies two fixed ones by name.  "origin" is my QBT
    // universe on GitHub and "amling" is keith amling's universe.
    // This means anywhere that takes a remote can be given "origin" or "amling"
    // and it will pick these guys.
    // 
    // The second half will always hit and treats the string as a format
    // string.  This means I could pass those above format string in place of
    // their short names and get the same effect, just like how you can "git fetch"
    // a git url or a remote name.
         new CompoundQbtRemoteFinder([
             new MapQbtRemoteFinder([
                 amling: new FormatQbtRemote(
                     gitRemoteVcs,
                     "https://github.com/AmlingQbt/%r.git",
                 ),
                 terabyte: new FormatQbtRemote(
                     gitRemoteVcs,
                     "ssh://git@github.com/TerabyteQbt/%r.git",
                 ),
             ]),
             new FormatQbtRemoteFinder(
                 gitRemoteVcs,
             ),
         ]),
    
    // Finally, artifact caching locations and the size of the local cache.  You can
    // probably use this unmodified.  Cache size below is 5G.
         new CompoundArtifactCacher(
             ImmutableList.of(
                 new LocalArtifactCacher(
                     dotQbt.resolve("artifacts/v1"),
                     5L * (1024 * 1024 * 1024)
                 ),
             ),
         ),
    );

You can initially take this file as-is, unmodified, to pull software from one of
the two primary authors of QBT, and you can see how to add your own "universe".

Once you have written this file to `qbt-config` next to `qbt-manifest` in the
meta repo you cloned, you can invoke qbt.

    $ qbt build --package meta_tools.release --output requested,directory,$HOME/qbt-release-%v

This command will build a "release" of QBT itself, which you could then use
instead of the binary distribution you downloaded, if you wish.  "meta_tools" is
the package that includes both QBT's core and the tools written around that core
to manipulate manifest files.

You could now build every package in the universe by doing:

    $ qbt build --all

Notice that if you have already built some packages, they are not rebuilt.  QBT
is incredibly good at only rebuilding what needs to be rebuilt.

As an example, let's fetch the package this documentation lives in, so we can
edit it.  When you want to work on a package, you run `getOverride` to ask QBT
to place the version of that package currently in your manifest on the local
disk.  Either one of these works:

    $ qbt getOverride --package qbt.app.main
    $ qbt getOverride --repo qbt
    
In either case, you can't check out part of a repository, so giving either a
package or repository name creates an override for the entire repository.

In the config file above, I have instructed QBT that overrides live "next
to" the meta repo (in the FormatLocalRepoFinder block).  By changing "../%r" to
"./%r" I could tell it to put them as subdirectories of my meta repo, or I could
insert an absolute path like "$HOME/projects", etc, if I preferred.

If you want to ensure QBT can always find your config and manifest file, in the
directory that contains all of your repos, you can do this:

    echo "$HOME/path/to/meta" > .qbt-meta-location

QBT does a "find up" to find this file, so place this in your home directory or
the root of your "workspace" and you are good to go.  The path can be absolute
*or* relative to the file location.

Now that we have gotten an override for the `qbt` repository, you can find the
docs in $META/../qbt/docs.  let's say you want to make changes to the docs.  You
can edit the files here and test your changes.

    $ vim index.md
    $ qbt build --package qbt.docs
    Actually building qbt.docs@509f1d2717cee5019b5964711102a842c800456a/qbt.docs@509f1d2717cee5019b5964711102a842c800456a...
    Completed request package qbt.docs@509f1d2717cee5019b5964711102a842c800456a

That's it!  It worked.  If you want to examine the results of the build, you can
ask qbt to dump the artifacts somewhere for you:

    $ qbt build --package qbt.docs --output requested,directory,`pwd`/../build/docs
    Completed request package qbt.docs@509f1d2717cee5019b5964711102a842c800456a

Notice that it didn't say "Actually building qbt.docs@...".  This is because it
already found it in your cache (which, according to the config above, is in your
home dir).  If you made any changes, however, it would detect them and rebuild
instead.

So now you are done testing your change, and you want to submit a pull request!
First, you need to update your manifest file.

    $ qbt updateManifest --repo qbt
    qbt is unchanged and dirty!
    Some update(s) failed, not writing manifest!
    $ git commit -m"Documentation fixes"
    $ qbt updateManifest --repo qbt
    Updated repo qbt from 89e0d238542f91d8f950b3375e44ba5382c318fa to 89f4cb174d92f1065a0974f5c0c8ef289b260eff...
    All update(s) successful, writing manifest.
    $ cd ../meta
    $ git status
    On branch feature/update-docs

    Changes not staged for commit:
    (use "git add <file>..." to update what will be committed)
    (use "git checkout -- <file>..." to discard changes in working directory)

            modified:   qbt-manifest

    $ git add qbt-manifest
    $ git commit -m"Updating docs"
    $ qbt pushPins origin --all
    $ git push origin feature/update-docs

THe `qbt pushPins` command pushed the commit you just made to the repo.  Where?
where QBT can find it, and that is all that matters.  Because the manifest file
indicates what commit is authoritative, refs in the package repos "have no
power".

Also, notice that I used the name "origin" above, but "origin" doesn't appear in
my config file example.  "origin" is for you to make - TODO: script to automate
this.  Create a meta repo and a repo for each package you care about somewhere
(any git server, doesn't matter) and push your pins there.  In your pull request
you will need to say where the pins can be fetched from, if it is not obvious.

You are free to push things to normal refs in your package repositories if you
wish, however, the pins are what matter to QBT.

So, now I get your pull request.  I add your pin remote to my config file, and
run `qbt fetchPins` to get your pins.  I can then do a build, examine the
output, look at the diff, etc.  THen I can choose to accept your pull request or
not.

    $ cd /path/to/meta
    $ git fetch cmyers
    $ git checkout feature/update-docs
    $ qbt fetchPins cmyers --all
    $ qbt updateOverrides --all
    All update(s) successful
    $ qbt build --all
    <snip - some output here - remember, it doesn't actually build all, only
    things that changed and the things that depend on the things that changed>

If that `qbt build --all` command succeeds, I know for certain your change has
not broken anything in my universe, and your PR is safe.  I can now merge my
change and push it to the repo, closing the pull request.


    vi: ft=markdown
