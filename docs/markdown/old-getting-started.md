# QBT - Getting Started

## Obtain QBT

To use QBT, first obtain a binary release of QBT from the [QBT
Website](http://qbtbuildtool.com).

Simply place the "qbt" script on your path and you are ready to run some QBT
commands, but most will require a "universe" to operate in.

## Obtain a manifest file

A "universe" of software is represented by a `qbt-manifest` file.  You can clone
the manifest file of QBT's first author by cloning his meta repository.

>     $ git clone https://github.com/AmlingQbt/meta.git

By convention, a universe is usually represented by a `meta` repository with
multiple package repositories "next to it".  To avoid confusion, you may wish
to create an organization in github to hold your universe so it does not spill
in to your other repositories.  More details on this later.

## Create a configuration file

Once you have a manifest file, you will need to produce a `qbt-config` file,
which typically lives next to your `qbt-manifest` file but is not checked in.

> Note: in a future release this will probably move to your home directory

Here is an example `qbt-config` file, along with many extra comments to explain
what everything does:

>     import com.google.common.collect.ImmutableList;
>     import java.nio.file.Paths;
>     import qbt.artifactcacher.CompoundArtifactCacher;
>     import qbt.artifactcacher.LocalArtifactCacher;
>     import qbt.config.CompoundQbtRemoteFinder;
>     import qbt.config.FormatLocalRepoFinder;
>     import qbt.config.FormatQbtRemoteFinder;
>     import qbt.config.MapQbtRemoteFinder;
>     import qbt.config.QbtConfig;
>     import qbt.pins.SimpleLocalPinsRepo;
>     import qbt.remote.FormatQbtRemote;
>     import qbt.remote.GithubQbtRemote;
>     import qbt.vcs.VcsRegistry;
>     
>     def dotQbt = Paths.get(System.getenv("HOME")).resolve(".qbt");
>     def gitRemoteVcs = VcsRegistry.getRawRemoteVcs("git");
>     def gitLocalVcs = gitRemoteVcs.getLocalVcs();
>     // uncomment this and edit to include a github token if desired.
>     //def github_api_token = new File(System.getProperty('user.home') + '/.github-api-token').text.trim();
>     
>     return new QbtConfig(
>     
>     // First config argument -- where are my overrides?  My simple choice is
>     // "next to meta".  LocalRepoFinder is an interface and it's entirely
>     // plausible to implement it in other ways, possibly even in qbt-config
>     // itself.
>          new FormatLocalRepoFinder(
>              gitLocalVcs,
>              workspaceRoot.resolve("../%r").toString(),
>          ),
>     
>     // Second config argument -- where are my local pins?  local pins are commits in
>     // package repositories that are pointed to by the manifest.  If you push the
>     // repository that the manifest file lives in to other people, they can't use it
>     // unless they can also get all the commits listed in that manifest, so pins are
>     // how QBT accomplishes this.
>     // 
>     // I put them in my home directory so they're shared between workspaces.  Since
>     // this is an immutable, append-only store sharing mostly makes sense.  I notice
>     // this is slightly inconsistent specification-wise: most of the rest are
>     // formats and this one is root directory that it makes subdirs of.  Should
>     // probably go back and change this to format.
>          new SimpleLocalPinsRepo(
>              gitRemoteVcs,
>              dotQbt.resolve("pins/v1"),
>          ),
>     
>     // Third config argument -- where are my remotes?  This is just a
>     // programmatic mapping from mere string to full-on QbtRemote platform
>     // object.
>     // 
>     // The first half specifies two fixed ones by name.  "origin" is my QBT
>     // universe on GitHub and "amling" is keith amling's universe.
>     // This means anywhere that takes a remote can be given "origin" or "amling"
>     // and it will pick these guys.
>     // 
>     // The second half will always hit and treats the string as a format
>     // string.  This means I could pass those above format string in place of
>     // their short names and get the same effect, just like how you can "git fetch"
>     // a git url or a remote name.
>          new CompoundQbtRemoteFinder([
>              new MapQbtRemoteFinder([
>
>                  // Here is an example "github aware" remote
>                  // This will automatically create package repos if they don't exist
>                  amling: new GithubQbtRemote(
>                     gitRemoteVcs,
>                     null, // change to github_api_token as defined above if desired
>                     "AmlingQbt", // username or organization name, can contain %r and/or %h
>                     // optional 4th argument is the remote repository name, defaults to "%r"
>                  ),
>                  // The FormatQbtRemote works with any format string git understands as a remote
>                  terabyte: new FormatQbtRemote(
>                      gitRemoteVcs,
>                      "https://github.com/TerabyteQbt/%r.git",
>                  ),
>                  // GithubQbtRemote and FormatQBtRemote both implement the QbtRemote interface.  You can add your
>                  // own implementations to handle different servers, autovivification of repositories, auth, etc.
>                  // custom: new ClassThatImplementsQbtRemote(
>                  //     gitRemoteVcs, // <-- you probably need one of these in your ctor
>                  //     "ssh://git@github.com/TerabyteQbt/%r.git", <-- whatever other args you need
>                  // ),
>              ]),
>              // Having this at the bottom of the chain lets you pass git remotes in directly, i.e. `qbt pushPins git@github.com/TerabyteQbt/pins.git`
>              new FormatQbtRemoteFinder(
>                  gitRemoteVcs,
>              ),
>          ]),
>     
>     // Finally, artifact caching locations and the size of the local cache.  You can
>     // probably use this unmodified.  Cache size below is 5G.
>          new CompoundArtifactCacher(
>              ImmutableList.of(
>                  new LocalArtifactCacher(
>                      dotQbt.resolve("artifacts/v1"),
>                      5L * (1024 * 1024 * 1024)
>                  ),
>              ),
>          ),
>     );

As may be apparently, a qbt-config file is just a standard groovy file, so you
can write arbitrary groovy and java in it to accomplish pretty much anything.
For the same reason, it can become an attack vector, so you should never check
in a qbt-config file or run against a config file you don't have reason to
trust.

You can initially take this file as-is, unmodified, to pull software from one of
the two primary authors of QBT, and you can see how to add your own "universe".

## My First Build

Once you have written this file to `qbt-config` next to `qbt-manifest` in the
meta repo you cloned, you can invoke qbt.

>     # qbt fetchPins amling
>     $ qbt build --package meta_tools.release --output requested,directory,$HOME/qbt-release-%v

The first command fetches "pins" - this grabs all commits of the "package
repositories" that are mentioned in your manifest file.

The second command will build a "release" of QBT itself, which you could then
use instead of the binary distribution you downloaded, if you wish.
`meta_tools.release` is the package that includes both QBT's core and the tools written
around that core to manipulate manifest files.

You could now build every package in the universe by doing:

>     $ qbt build --all

Notice that if you have already built some packages, they are not rebuilt.  QBT
is incredibly good at only rebuilding what needs to be rebuilt.

You could examine any package or repository's source by creating an "override".

>     $ qbt getOverride --package meta_tools.release
>     $ qbt getOverride --repo qbt

If you use the default config file above, the `meta_tools` or `qbt` repository
will be placed next to your meta directory.

## Fork the Planet

In a normal git repository, in order to make a pull request, you first create
your own personal "fork" of the repo, so you can push your changes there and
request they be pulled.  Since QBT is all about cross-package and
cross-repository consistency, you must instead fork the entire universe.

First, you will need to create a fork of meta, and a place to store your
"pins".  The easiest way to do this is to create an organization in github, so
the package repositories don't make a "mess" in your github account.

Log into Github and select "new organization" from the menu in the top right.
Let's say you created a Github organization called "NewUserQbt".  You will
probably want to create a single repo in there called "meta", and if you want
to submit pull requests back to others, you should probably fork this repo from
someone else such as [Amling](http://github.com/AmlingQbt/meta).

If you haven't already, you need to also generate a Github API token.  You can
use standard SSH authentication for pushing and pulling, but to create a
repository you need an API token.  Go to [Create new
Token](https://github.com/settings/tokens/new) and ensure your token has the
`public_repo` permission.  Store it in a file in your home directory (like
`.qbt-github-token`) and add it to your config file like the example above
describes (see: `gitub_api_token`).  Alternatively, you could manually create a
repository in your target prefix for each repository in the qbt-manifest file.

> NOTE: it has been observed that if you create your API token before you
> create the organization, it might not work (pushPins may get 401s).  If you
> experience problems, try generating a new token after creating your
> organization.

Either way, you will need to create a "pin remote" entry in your config file,
following the pattern above (see the `terabyte` and `amling` entries above).

Because the `meta` repository is the metadata only, to store your code you need
to store both a branch which contains meta, and the "package pins".  You can
take the package pins you fetched earlier and push them now.

>     $ git push new_git_remote HEAD:master
>     $ qbt pushPins new_qbt_remote

Where `new_git_remote` is the name you gave the remote for your meta repo, and
`new_qbt_remote` is the name you gave the package pin repos in your
`qbt-config` file (and of course, they could be the same thing for
convenience).  The first line pushes the `qbt-manifest` itself, and the second
line pushes all your pins (and if your remote is a GithubQbtRemote, it will
automatically create any repos that are missing).

You now have a complete fork of both metadata and package pins ready for use.

## Hack the Planet

Let's work through a complete example of how to submit a change to qbt and ask that we pull it.

First, we must override the packages we wish to edit.  To do this, you run
`getOverride` to ask QBT to place the version of that package currently in your
manifest on the local disk.  Either one of these works:

>     $ qbt getOverride --package qbt.app.main
>     $ qbt getOverride --repo qbt
    
In either case, you can't check out part of a repository, so giving either a
package or repository name creates an override for the entire repository.

In the config file above, we instructed QBT that overrides live "next to" the
meta repo (in the FormatLocalRepoFinder block).  By changing "../%r" to
"/some/absolute/path/%r" we could tell it to put them in whatever location we
desire.

If you want to ensure QBT can always find your config and manifest file, in the
directory that contains all of your repos, you can do this:

>     echo "$HOME/path/to/meta" > .qbt-meta-location

QBT does a "find up" to find this file, so place this in your home directory or
the root of your "workspace" and you are good to go.  The path can be absolute
*or* relative to the file location.  Setting this up allows us to run the `qbt`
command in any directory and it can easily find our config and manifest files

Now that we have gotten an override for the `qbt` repository, you can find the
docs in `$META/../qbt/docs`.  let's say you want to make changes to the docs.
You can edit the files here and test your changes.

>     $ vim index.md
>     $ qbt build --package qbt.docs
>     Actually building qbt.docs@509f1d2717cee5019b5964711102a842c800456a/qbt.docs@509f1d2717cee5019b5964711102a842c800456a...
>     Completed request package qbt.docs@509f1d2717cee5019b5964711102a842c800456a

That's it!  It worked.  If you want to examine the results of the build, you can
ask qbt to dump the artifacts somewhere for you:

>     $ qbt build --package qbt.docs --output requested,directory,/tmp/my-docs
>     Completed request package qbt.docs@509f1d2717cee5019b5964711102a842c800456a

Notice that it didn't say "Actually building qbt.docs@...".  This is because it
already found it in your cache (which, according to the config above, is in your
home dir).  If you made any changes, however, it would detect them and rebuild
instead.

> Note: qbt will overwrite files, but it will not remove files first, so make
> sure you clean the directory if you want that behavior.

So now you are done testing your change, and you want to submit a pull request!
The easiest way is by using `qbt commit`.  What does `qbt commit` actually do?
To make a change in QBT, you need to create a commit in the package repository,
then you need to update your manifest file to point to that commit, then you
need to commit that, then you need to ensure that the "pin" for that package
lives in your local pins.  `qbt commit` does all of that for you.  Be careful,
however, because `qbt commit` also commits *everything* that is tracked or
untracked in your repository, and ignores the index.  `qbt commit` does not
commit ignored files, however.

>     $ qbt commit -m"Updating docs"
>     [qbt] Committed 4b90f2a4ac389182b7aa52dd4b65e99cf64598d1
>     [meta] Committed 246e679ad07b09fa27ae5977611ee43406421c6b

Note that if you have multiple packages overridden, you can specify to only
include certain ones by using `--repo`.

Just like in git, we've made our commit but still need to push it.  To push your commit and pins, you run these commands:

>     $ qbt pushPins new_qbt_remote
>     $ git push new_git_remote HEAD:feature/update-docs

THe `qbt pushPins` command pushed the commit you just made to the repo.  Where?
where QBT can find it, and that is all that matters.  Because the manifest file
indicates what commit is authoritative, refs in the package repos "have no
power".

You are free to push things to normal refs in your package repositories if you
wish, however, the pins are what matter to QBT.

Now you can make a pull request.  Great work!

## Receiving Pull Requests

So, now I get your pull request.  I add your pin remote to my config file, and
run `qbt fetchPins` to get your pins.  I can then do a build, examine the
output, look at the diff, etc.  Then I can choose to accept your pull request or
not.

>     $ cd /path/to/meta
>     $ git fetch cmyers
>     $ git checkout feature/update-docs
>     $ qbt fetchPins cmyers --all
>     $ qbt updateOverrides --all
>     All update(s) successful
>     $ qbt build --all
>     <snip - some output here - remember, it doesn't actually build all, only
>     things that changed and the things that depend on the things that changed>

If that `qbt build --all` command succeeds, I know for certain your change has
not broken anything in my universe.  After examining the code, I decide your
change is safe.  I can now merge your change and push it to the repo, closing
the pull request.

## Plumbing:  updateManifest

Sometimes `qbt commit` is not granular enough.  Maybe you need to use the index
and make several commits, or maybe you end up in a bad state because you
accidentally committed things and you need to fix it.

You can make whatever commits you want directly in your overrides, then run
`updateManifest` to update your manifest and create the pins necessary.

The example above, with `updateManifest`, would have looked like this:

>     $ qbt updateManifest --repo qbt
>     qbt is unchanged and dirty!
>     Some update(s) failed, not writing manifest!
>     $ git commit -m"Documentation fixes"
>     $ qbt updateManifest --repo qbt
>     Updated repo qbt from 89e0d238542f91d8f950b3375e44ba5382c318fa to 89f4cb174d92f1065a0974f5c0c8ef289b260eff...
>     All update(s) successful, writing manifest.
>     $ cd ../meta
>     $ git status
>     On branch feature/update-docs
> 
>     Changes not staged for commit:
>     (use "git add <file>..." to update what will be committed)
>     (use "git checkout -- <file>..." to discard changes in working directory)
> 
>             modified:   qbt-manifest
> 
>     $ git add qbt-manifest
>     $ git commit -m"Updating docs"
>     $ qbt pushPins origin --all
>     $ git push origin feature/update-docs

Like before, we end with pushing our meta and our pins.

    vi: ft=markdown
