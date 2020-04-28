package resolve;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import java.util.ArrayList;
import java.util.List;

public class MavenResolver {

    private List<RemoteRepository> repositories;
    RepositorySystem system;
    DefaultRepositorySystemSession session;

    /**
     * Resolver will be initialized to specified to location.
     *
     * @param targetLocation file path the target
     */
    public MavenResolver(String targetLocation) {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        system = locator.getService(RepositorySystem.class);
        session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(targetLocation);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        repositories = new ArrayList<>();
        repositories.add(new RemoteRepository.Builder(
                "central", "default", "https://repo.maven.apache.org/maven2/").build());
    }

    /**
     * Sepcified repository will be added to remote repositories.
     *
     * @param id  identifier of the repository
     * @param url url of the repository
     */
    public void addRepository(String id, String url) {
        repositories.add(new RemoteRepository.Builder(id, "default", url).build());
    }

    /**
     * Sepcified repository will be added to remote repositories.
     *
     * @param id  identifier of the repository
     * @param url url of the repository
     */
    public void addRepository(String id, String url, String username, String password) {
        Authentication authentication =
                new AuthenticationBuilder()
                        .addUsername(username)
                        .addPassword(password)
                        .build();
        repositories.add(new RemoteRepository.Builder(id, "default", url)
                .setAuthentication(authentication).build());
    }

    /**
     * Resolves provided artifact into resolver location.
     *
     * @param groupId                       group ID of the dependency
     * @param artifactId                    artifact ID of the dependency
     * @param version                       version of the dependency
     * @param resolveTransitiveDependencies should resolve resolve transitive dependencies
     */
    public void resolve(String groupId, String artifactId, String version, boolean resolveTransitiveDependencies) {
        Artifact artifact = new DefaultArtifact(groupId + ":" + artifactId + ":" + version);
        if (resolveTransitiveDependencies) {
            DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));
            collectRequest.setRepositories(repositories);

            DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFlter);

            try {
                List<ArtifactResult> artifactResults =
                        system.resolveDependencies(session, dependencyRequest).getArtifactResults();

                for (ArtifactResult artifactResult : artifactResults) {
                    System.out.println(artifactResult.getArtifact() + " resolved to "
                            + artifactResult.getArtifact().getFile());

                    for (DependencyNode dependency : artifactResult.getRequest().getDependencyNode().getChildren()) {
                        resolve(dependency.getArtifact().getGroupId(), dependency.getArtifact().getArtifactId(),
                                dependency.getArtifact().getVersion(), false);
                    }
                }
            } catch (DependencyResolutionException e) {
                System.out.println("Cannot resolve " + artifact);
            }
        } else {
            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);
            artifactRequest.setRepositories(repositories);

            try {
                ArtifactResult artifactResult = system.resolveArtifact(session, artifactRequest);
                artifact = artifactResult.getArtifact();

                System.out.println(artifact + " resolved to  " + artifact.getFile());
            } catch (ArtifactResolutionException e) {
                System.out.println("Cannot resolve " + artifact);
            }
        }
    }
}
