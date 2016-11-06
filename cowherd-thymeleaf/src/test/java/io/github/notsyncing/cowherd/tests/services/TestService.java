package io.github.notsyncing.cowherd.tests.services;

import io.github.notsyncing.cowherd.annotations.*;
import io.github.notsyncing.cowherd.annotations.httpmethods.HttpGet;
import io.github.notsyncing.cowherd.thymeleaf.ViewResponse;
import io.github.notsyncing.cowherd.service.CowherdService;

class TestModel
{
    private String text;

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }
}

@Route("/TestService")
public class TestService extends CowherdService
{
    @Exported
    @HttpGet
    @Route(value = "^/te.html$", entry = true, viewPath = "/te.html")
    public ViewResponse<TestModel> testTemplateEngine()
    {
        TestModel m = new TestModel();
        m.setText("Hello, world!");

        return new ViewResponse<>(m);
    }
}
