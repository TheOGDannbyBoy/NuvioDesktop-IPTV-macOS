import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Builds this fork using only the publishable client configuration already bundled
 * in the user's official Nuvio.app. It never reads account credentials or prints
 * the client configuration. The official app must be installed in /Applications.
 */
public final class BuildMacIptvFromInstalledNuvio {
    public static void main(String[] args) throws Exception {
        Path appDir = Path.of("/Applications/Nuvio.app/Contents/app");
        Path officialJar;
        try (var files = Files.list(appDir)) {
            officialJar = files.filter(p -> p.getFileName().toString().matches("composeApp-desktop-.*\\.jar"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Official Nuvio app JAR not found"));
        }
        String backendUrl;
        String anonKey;
        String fallbackUrl;
        String tmdbKey;
        try (var loader = new URLClassLoader(new java.net.URL[]{officialJar.toUri().toURL()}, null)) {
            Class<?> supabase = loader.loadClass("com.nuvio.app.core.network.SupabaseConfig");
            Class<?> tmdb = loader.loadClass("com.nuvio.app.features.tmdb.TmdbConfig");
            backendUrl = (String) supabase.getField("URL").get(null);
            anonKey = (String) supabase.getField("ANON_KEY").get(null);
            fallbackUrl = (String) supabase.getField("FALLBACK_URL").get(null);
            tmdbKey = (String) tmdb.getField("API_KEY").get(null);
        }
        if (!backendUrl.startsWith("https://") || anonKey.isBlank()) {
            throw new IllegalStateException("Official app does not contain usable public client configuration");
        }
        String[] gradleArgs = args.length == 0
            ? new String[]{":composeApp:packageDmg", "--no-daemon", "--no-configuration-cache", "-Pcompose.desktop.packaging.checkJdkVendor=false", "--console=plain"}
            : args;
        var command = new java.util.ArrayList<String>();
        command.add("./gradlew");
        command.addAll(Arrays.asList(gradleArgs));
        var process = new ProcessBuilder(command).directory(Path.of(".").toFile()).inheritIO();
        process.environment().put("NUVIO_SUPABASE_URL", backendUrl);
        process.environment().put("NUVIO_SUPABASE_ANON_KEY", anonKey);
        process.environment().put("NUVIO_SUPABASE_FALLBACK_URL", fallbackUrl);
        process.environment().put("TMDB_API_KEY", tmdbKey);
        process.environment().remove("SENTRY_DSN");
        process.environment().remove("SENTRY_DESKTOP_DSN");
        System.exit(process.start().waitFor());
    }
}
