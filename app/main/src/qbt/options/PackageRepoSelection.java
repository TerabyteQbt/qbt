package qbt.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import misc1.commons.ds.LazyCollector;
import org.apache.commons.lang3.tuple.Pair;
import qbt.NormalDependencyType;
import qbt.config.QbtConfig;
import qbt.manifest.current.QbtManifest;
import qbt.manifest.current.RepoManifest;
import qbt.map.DependencyComputer;
import qbt.map.PackageTipDependenciesMapper;
import qbt.recursive.srpd.SimpleRecursivePackageData;
import qbt.recursive.srpd.SimpleRecursivePackageDataMapper;
import qbt.script.QbtScriptEngine;
import qbt.tip.AbstractTip;
import qbt.tip.PackageTip;
import qbt.tip.RepoTip;

public final class PackageRepoSelection {
    private PackageRepoSelection() {
        // no
    }

    public static ImmutableSet<PackageTip> evalPackages(QbtConfig config, QbtManifest manifest, String script) {
        return new PackageCoercer(manifest).coerce(eval(config, manifest, script));
    }

    public static ImmutableSet<RepoTip> evalRepos(QbtConfig config, QbtManifest manifest, String script) {
        return new RepoCoercer(manifest).coerce(eval(config, manifest, script));
    }

    private static Object eval(final QbtConfig config, final QbtManifest manifest, String script) {
        final PackageCoercer packageCoercer = new PackageCoercer(manifest);
        QbtScriptEngine.Builder s = QbtScriptEngine.TYPE.builder();
        s = s.addClosure("all", (args) -> manifest.repos.keySet());
        s = s.addClosure("overrides", (args) -> overrides(config, manifest));
        s = s.addClosure("inward", (args) -> inwardsClosure(manifest, packageCoercer.coerce(args)));
        s = s.addClosure("outward", (args) -> outwardsClosure(manifest, packageCoercer.coerce(args)));
        s = s.addClosure("r", (args) -> RepoTip.TYPE.parseRequire(String.valueOf(args[0])));
        s = s.addClosure("p", (args) -> PackageTip.TYPE.parseRequire(String.valueOf(args[0])));
        return s.build().eval(script);
    }

    private static abstract class AbstractCoercer<T extends AbstractTip<T>> {
        protected final QbtManifest manifest;
        private final AbstractTip.Type<T> type;

        public AbstractCoercer(QbtManifest manifest, AbstractTip.Type<T> type) {
            this.manifest = manifest;
            this.type = type;
        }

        public ImmutableSet<T> coerce(Object o) {
            ImmutableSet.Builder<T> b = ImmutableSet.builder();
            coerce(b, o);
            return b.build();
        }

        protected void coerce(ImmutableSet.Builder<T> b, Object o) {
            if(type.clazz.isInstance(o)) {
                b.add(type.clazz.cast(o));
                return;
            }

            if(o instanceof Iterable) {
                for(Object o2 : (Iterable)o) {
                    coerce(b, o2);
                }
                return;
            }

            if(o instanceof String) {
                b.add(type.parseRequire((String)o));
                return;
            }

            if(o instanceof Object[]) {
                for(Object o2 : (Object[])o) {
                    coerce(b, o2);
                }
                return;
            }

            throw new IllegalArgumentException("Cannot coerce to " + type.clazz.getSimpleName() + ": " + o);
        }
    }

    private static final class PackageCoercer extends AbstractCoercer<PackageTip> {
        public PackageCoercer(QbtManifest manifest) {
            super(manifest, PackageTip.TYPE);
        }

        @Override
        protected void coerce(ImmutableSet.Builder<PackageTip> b, Object o) {
            if(o instanceof RepoTip) {
                b.addAll(reposToPackages(manifest, ImmutableSet.of((RepoTip)o)));
                return;
            }

            super.coerce(b, o);
        }
    }

    private static final class RepoCoercer extends AbstractCoercer<RepoTip> {
        public RepoCoercer(QbtManifest manifest) {
            super(manifest, RepoTip.TYPE);
        }

        @Override
        protected void coerce(ImmutableSet.Builder<RepoTip> b, Object o) {
            if(o instanceof PackageTip) {
                b.addAll(packagesToRepos(manifest, ImmutableSet.of((PackageTip)o)));
                return;
            }

            super.coerce(b, o);
        }
    }

    public static ImmutableSet<PackageTip> inwardsClosure(QbtManifest manifest, ImmutableSet<PackageTip> packages) {
        DependencyComputer dependencyComputer = new DependencyComputer(manifest);
        PackageTipDependenciesMapper dependenciesMapper = new PackageTipDependenciesMapper();
        ImmutableList.Builder<LazyCollector<PackageTip>> b = ImmutableList.builder();
        for(PackageTip pkg : packages) {
            b.add(dependenciesMapper.transform(dependencyComputer.compute(pkg)));
        }
        return LazyCollector.unionIterable(b.build()).forceSet();
    }

    public static ImmutableSet<PackageTip> outwardsClosure(QbtManifest manifest, final ImmutableSet<PackageTip> packages) {
        DependencyComputer dependencyComputer = new DependencyComputer(manifest);
        SimpleRecursivePackageDataMapper<DependencyComputer.Result, Boolean> usesOutwardsMapper = new SimpleRecursivePackageDataMapper<DependencyComputer.Result, Boolean>() {
            @Override
            protected Boolean map(SimpleRecursivePackageData<DependencyComputer.Result> r) {
                if(packages.contains(r.result.packageTip)) {
                    return true;
                }
                for(Pair<NormalDependencyType, SimpleRecursivePackageData<DependencyComputer.Result>> dependencyResult : r.children.values()) {
                    if(transform(dependencyResult.getRight())) {
                        return true;
                    }
                }
                return false;
            }
        };
        ImmutableSet.Builder<PackageTip> b = ImmutableSet.builder();
        for(PackageTip pkg : manifest.packageToRepo.keySet()) {
            if(usesOutwardsMapper.transform(dependencyComputer.compute(pkg))) {
                b.add(pkg);
            }
        }
        return b.build();
    }

    public static ImmutableSet<RepoTip> overrides(QbtConfig config, QbtManifest manifest) {
        ImmutableSet.Builder<RepoTip> b = ImmutableSet.builder();
        for(RepoTip repo : manifest.repos.keySet()) {
            if(config.localRepoFinder.findLocalRepo(repo) != null) {
                b.add(repo);
            }
        }
        return b.build();
    }

    public static ImmutableSet<PackageTip> reposToPackages(QbtManifest manifest, ImmutableSet<RepoTip> repos) {
        ImmutableSet.Builder<PackageTip> b = ImmutableSet.builder();
        for(RepoTip repo : repos) {
            RepoManifest repoManifest = manifest.repos.get(repo);
            if(repoManifest == null) {
                throw new IllegalArgumentException("No such repo: " + repo);
            }
            for(String name : repoManifest.packages.keySet()) {
                b.add(repo.toPackage(name));
            }
        }
        return b.build();
    }

    public static ImmutableSet<RepoTip> packagesToRepos(QbtManifest manifest, ImmutableSet<PackageTip> packages) {
        ImmutableSet.Builder<RepoTip> b = ImmutableSet.builder();
        for(PackageTip pkg : packages) {
            RepoTip repo = manifest.packageToRepo.get(pkg);
            if(repo == null) {
                throw new IllegalArgumentException("No such package: " + pkg);
            }
            b.add(repo);
        }
        return b.build();
    }
}
