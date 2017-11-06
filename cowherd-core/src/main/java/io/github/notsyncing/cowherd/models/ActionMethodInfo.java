package io.github.notsyncing.cowherd.models;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ActionMethodInfo {
    private Method method;
    private List<ActionMethodParameterInfo> parameters;

    public ActionMethodInfo(Method method) {
        this.method = method;

        this.parameters = Stream.of(method.getParameters())
                .map(ActionMethodParameterInfo::new)
                .collect(Collectors.toList());
    }

    public Method getMethod() {
        return method;
    }

    public List<ActionMethodParameterInfo> getParameters() {
        return parameters;
    }

    public void setParameters(List<ActionMethodParameterInfo> parameters) {
        this.parameters = parameters;
    }
}
