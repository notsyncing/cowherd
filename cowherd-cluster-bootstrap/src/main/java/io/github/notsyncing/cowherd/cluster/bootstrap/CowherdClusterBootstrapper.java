package io.github.notsyncing.cowherd.cluster.bootstrap;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CowherdClusterBootstrapper {
    private Path bootstrapDir;
    private String mainClassName;

    private static Logger log = Logger.getLogger(CowherdClusterBootstrapper.class.getSimpleName());

    public static void main(String[] args) throws IOException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        if (args.length < 2) {
            log.severe("Insufficient parameters!");
            System.exit(-1);
            return;
        }

        Path bootstrapDir = Paths.get(args[0]);
        String mainClassName = args[1];

        log.info("Bootstrap directory: " + bootstrapDir);
        log.info("Main class: " + mainClassName);

        CowherdClusterBootstrapper bootstrapper = new CowherdClusterBootstrapper(bootstrapDir, mainClassName);
        bootstrapper.start();
    }

    private CowherdClusterBootstrapper(Path bootstrapDir, String mainClassName) {
        this.bootstrapDir = bootstrapDir;
        this.mainClassName = mainClassName;
    }

    private List<Path> getItemsToLoad() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();

        if (!(classLoader instanceof URLClassLoader)) {
            log.severe("Unsupported class loader type " + classLoader);
            System.exit(-2);
            return null;
        }

        URLClassLoader cl = (URLClassLoader) classLoader;

        Set<String> loadedJarFilenames = Stream.of(cl.getURLs())
                .map(url -> {
                    try {
                        return Paths.get(url.toURI()).getFileName().toString();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Path> itemsToLoad = Files.list(bootstrapDir)
                .filter(p -> !loadedJarFilenames.contains(p.getFileName().toString()))
                .collect(Collectors.toList());

        String items = itemsToLoad.stream()
                .map(Path::toString)
                .collect(Collectors.joining("\n"));

        log.info("Will load following classpath items:\n" + items);

        return itemsToLoad;
    }

    private void start() throws IOException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        List<Path> itemsToLoad = getItemsToLoad();

        if (itemsToLoad == null) {
            log.severe("Failed to get items to load!");
            System.exit(-3);
        }

        List<URL> urls = itemsToLoad.stream()
                .map(p -> {
                    try {
                        return p.toUri().toURL();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (mainClassName == null) {
            System.exit(-4);
        }

        URLClassLoader appLoader = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
        Class<?> mainClass = appLoader.loadClass(mainClassName);

        Method mainMethod = Stream.of(mainClass.getDeclaredMethods())
                .filter(m -> m.getName().equals("main"))
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> m.getParameterCount() == 1)
                .findFirst()
                .orElse(null);

        if (mainMethod == null) {
            log.severe("Cannot find main method in class " + mainClassName);
            System.exit(-5);
            return;
        }

        log.info("-------- S P I L T - L I N E --------");

        mainMethod.invoke(null, (Object) new String[0]);
    }
}
