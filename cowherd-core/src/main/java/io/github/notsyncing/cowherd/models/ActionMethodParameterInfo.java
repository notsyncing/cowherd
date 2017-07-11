package io.github.notsyncing.cowherd.models;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

public class ActionMethodParameterInfo {
    private Parameter parameter;
    private Annotation[] annotations;

    public ActionMethodParameterInfo(Parameter parameter) {
        this.parameter = parameter;
        this.annotations = parameter.getAnnotations();
    }

    public Parameter getParameter() {
        return parameter;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }
}
