package io.github.notsyncing.cowherd.thymeleaf;

import io.github.notsyncing.cowherd.CowherdPart;
import io.github.notsyncing.cowherd.commons.CowherdConfiguration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.nio.file.Path;

public class CowherdThymeleafPart implements CowherdPart
{
    public static TemplateEngine templateEngine;

    private void initTemplateEngine()
    {
        templateEngine = new TemplateEngine();
        templateEngine.addDialect(new Java8TimeDialect());

        for (Path r : CowherdConfiguration.getContextRoots()) {
            if (r.getName(r.getNameCount() - 1).toString().equals("$")) {
                ClassLoaderTemplateResolver clr = new ClassLoaderTemplateResolver();
                clr.setCharacterEncoding("utf-8");
                clr.setPrefix("APP_ROOT/");
                clr.setSuffix(".html");
                clr.setTemplateMode(TemplateMode.HTML);
                clr.setCacheable(!CowherdConfiguration.isEveryHtmlIsTemplate());
                templateEngine.addTemplateResolver(clr);
                continue;
            }

            FileTemplateResolver fr = new FileTemplateResolver();
            fr.setCharacterEncoding("utf-8");
            fr.setPrefix(r.toString() + "/");
            fr.setSuffix(".html");
            fr.setTemplateMode(TemplateMode.HTML);
            fr.setCacheable(!CowherdConfiguration.isEveryHtmlIsTemplate());
            templateEngine.addTemplateResolver(fr);
        }
    }

    @Override
    public void init()
    {
        initTemplateEngine();
    }

    @Override
    public void destroy()
    {
    }
}
