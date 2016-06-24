package io.github.notsyncing.cowherd.files;

import io.github.notsyncing.cowherd.annotations.Exported;
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpGet;
import io.github.notsyncing.cowherd.responses.FileResponse;
import io.github.notsyncing.cowherd.service.CowherdService;

import java.nio.file.Path;

public class CowherdFileStorageService extends CowherdService
{
    @Exported
    @HttpGet
    public FileResponse getFile(Enum tag, String path)
    {
        Path store = getFileStorage().getStoragePath(tag);
        Path p = store.resolve(path);

        return new FileResponse(p);
    }
}
