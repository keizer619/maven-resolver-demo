package resolve;

public class Run {
    public static void main(String[] args) {
        MavenResolver resolver = new MavenResolver("target/local-repo");

        //Pass local .m2 directory as a repository
        resolver.addRepository("local", "file:" + System.getProperty("user.home") + "/.m2/repository");

        //Pass custom maven repository
        resolver.addRepository("wso2-releases", "http://maven.wso2.org/nexus/content/repositories/releases/");


        String username = System.getenv("GIT_USER");
        String password = System.getenv("GIT_PAT");
        resolver.addRepository("ballerina-github",
                "https://maven.pkg.github.com/ballerina-platform/ballerina-update-tool", username, password);

        //This is available in maven central which is the default repository
        resolver.resolve("org.apache.spark", "spark-core_2.12", "2.4.5");

        //This is available in wso2 releases
        resolver.resolve("org.ballerinalang", "ballerina-core", "1.2.2");

        //This is available in local m2 repository
        resolver.resolve("org.tharik", "my-sample-prog", "0.9-SNAPSHOT");

        //This is available in private repostory
        resolver.resolve("org.ballerinalang", "ballerina-command-distribution", "0.8.5");
    }
}
