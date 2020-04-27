package resolve;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.util.ArrayList;
import java.util.List;

public class MavenResolver {

    private List<RemoteRepository> repositories;
    RepositorySystem system;
    RepositorySystemSession session;

    public MavenResolver(String targetLocation) {

        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                exception.printStackTrace();
            }
        });

        system = locator.getService(RepositorySystem.class);
        DefaultRepositorySystemSession defaultSession = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(targetLocation);
        defaultSession.setLocalRepositoryManager(system.newLocalRepositoryManager(defaultSession, localRepo));
        session = defaultSession;

        repositories = new ArrayList<>();
        repositories.add(new RemoteRepository.Builder(
                "central", "default", "https://repo.maven.apache.org/maven2/").build());
    }

    public void addRepository(String id, String location) {
        repositories.add(new RemoteRepository.Builder(id, "default", location).build());
    }

    public void resolve(String groupId, String artifactId, String version) {
        Artifact artifact = new DefaultArtifact(groupId + ":" + artifactId + ":" + version);
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
