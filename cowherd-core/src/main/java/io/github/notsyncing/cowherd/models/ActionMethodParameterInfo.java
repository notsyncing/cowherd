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

    public ActionMethodParameterInfo() {

    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setParameter(Parameter parameter) {
        this.parameter = parameter;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Annotation[] annotations) {
        this.annotations = annotations;
    }
}
