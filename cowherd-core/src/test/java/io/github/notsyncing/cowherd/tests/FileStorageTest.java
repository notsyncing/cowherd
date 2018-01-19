package io.github.notsyncing.cowherd.tests;

import io.github.notsyncing.cowherd.Cowherd;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import io.github.notsyncing.cowherd.files.FileStorage;
import io.github.notsyncing.cowherd.service.CowherdDependencyInjector;
import io.github.notsyncing.cowherd.tests.services.TestStorageEnum;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class FileStorageTest
{
    private Path tempDir;
    private Vertx vertx;
    private FileStorage fs;

    @Before
    public void setUp() throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException {
        vertx = Vertx.vertx();

        CowherdConfiguration.setStoreFilesByDate(false);

        Cowherd.dependencyInjector = new CowherdDependencyInjector();
        Cowherd.dependencyInjector.registerComponent(Vertx.class, vertx);

        fs = new FileStorage();

        tempDir = Files.createTempDirectory("cowherd-test");
    }

    @After
    public void tearDown() throws IOException
    {
        FileUtils.deleteDirectory(tempDir.toFile());

        vertx.close();
    }

    @Test
    public void testRegisterStoragePath() throws IOException
    {
        Path dir = tempDir.resolve("test_storage");
        assertTrue(Files.notExists(dir));

        fs.registerStoragePath(TestStorageEnum.TestStorage, dir);
        assertTrue(Files.exists(dir));

        assertEquals(dir, fs.getStoragePath(TestStorageEnum.TestStorage));
    }

    @Test
    public void testStoreFile(TestContext context) throws IOException
    {
        Path tempFile = Files.createTempFile("cowherd-test-fs", ".txt");
        Async async = context.async();

        Path dir = tempDir.resolve("test_storage");
        fs.registerStoragePath(TestStorageEnum.TestStorage, dir);

        assertTrue(Files.notExists(dir.resolve(tempFile.getFileName())));

        fs.storeFile(tempFile, TestStorageEnum.TestStorage, null, true)
                .thenAccept(p -> {
                    context.assertEquals(tempFile.getFileName(), p);
                    context.assertTrue(Files.exists(dir.resolve(tempFile.getFileName())));
                    context.assertTrue(Files.exists(tempFile));
                    async.complete();

                    try {
                        Files.delete(tempFile);
                    } catch (IOException e) {
                        context.fail(e);
                    }
                })
                .exceptionally(ex -> {
                    async.complete();
                    context.fail(ex);

                    try {
                        Files.delete(tempFile);
                    } catch (IOException e) {
                        context.fail(e);
                    }

                    return null;
                });
    }

    @Test
    public void testStoreFileWithStoreByDate(TestContext context) throws IOException
    {
        CowherdConfiguration.setStoreFilesByDate(true);

        Path tempFile = Files.createTempFile("cowherd-test-fs", ".txt");
        Async async = context.async();

        Path dir = tempDir.resolve("test_storage");
        fs.registerStoragePath(TestStorageEnum.TestStorage, dir);

        assertTrue(Files.notExists(dir.resolve(tempFile.getFileName())));

        fs.storeFile(tempFile, TestStorageEnum.TestStorage, null, true)
                .thenAccept(p -> {
                    LocalDate d = LocalDate.now();
                    Path tempFileFullPath = Paths.get(String.valueOf(d.getYear()), String.valueOf(d.getMonthValue()),
                            String.valueOf(d.getDayOfMonth()), tempFile.getFileName().toString());

                    context.assertEquals(tempFileFullPath, p);
                    context.assertTrue(Files.exists(dir.resolve(tempFileFullPath)));
                    context.assertTrue(Files.exists(tempFile));
                    async.complete();

                    try {
                        Files.delete(tempFile);
                    } catch (IOException e) {
                        context.fail(e);
                    }
                })
                .exceptionally(ex -> {
                    async.complete();
                    context.fail(ex);

                    try {
                        Files.delete(tempFile);
                    } catch (IOException e) {
                        context.fail(e);
                    }

                    return null;
                });
    }

    @Test
    public void testStoreFileAndRemoveOld(TestContext context) throws IOException
    {
        Path tempFile = Files.createTempFile("cowherd-test-fs", ".txt");
        Async async = context.async();

        Path dir = tempDir.resolve("test_storage");
        fs.registerStoragePath(TestStorageEnum.TestStorage, dir);

        assertTrue(Files.notExists(dir.resolve(tempFile.getFileName())));

        fs.storeFile(tempFile, TestStorageEnum.TestStorage, null, false)
                .thenAccept(r -> {
                    context.assertTrue(Files.exists(dir.resolve(tempFile.getFileName())));
                    context.assertTrue(Files.notExists(tempFile));
                    async.complete();
                })
                .exceptionally(ex -> {
                    async.complete();
                    context.fail(ex);

                    try {
                        Files.delete(tempFile);
                    } catch (IOException e) {
                        context.fail(e);
                    }

                    return null;
                });
    }

    @Test
    public void testStoreFileWithAlternativeName(TestContext context) throws IOException
    {
        Path tempFile = Files.createTempFile("cowherd-test-fs", ".txt");
        Async async = context.async();

        Path dir = tempDir.resolve("test_storage");
        fs.registerStoragePath(TestStorageEnum.TestStorage, dir);

        assertTrue(Files.notExists(dir.resolve(tempFile.getFileName())));

        fs.storeFile(tempFile, TestStorageEnum.TestStorage, "aaa.txt", false)
                .thenAccept(r -> {
                    context.assertTrue(Files.notExists(dir.resolve(tempFile.getFileName())));
                    context.assertTrue(Files.exists(dir.resolve("aaa.txt")));

                    async.complete();
                })
                .exceptionally(ex -> {
                    async.complete();
                    context.fail(ex);

                    try {
                        Files.delete(tempFile);
                    } catch (IOException e) {
                        context.fail(e);
                    }

                    return null;
                });
    }
}
