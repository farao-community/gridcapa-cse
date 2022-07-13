/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class GenericThreadLauncher<T, U> extends Thread {

    public static final String INTERRUPTED = "The task has been interrupted";
    private static final String NO_RESULT = "No result has been found";
    private T threadable;
    private Method run;
    private Object[] args;

    private final String identifiant;

    private U result;
    private boolean isInterrupt = false;

    public GenericThreadLauncher(T threadable, String identifiant) {
        Class<T> actualTypeArgument = (Class<T>) threadable.getClass();
        List<Method> methods = getMethodsAnnotatedWith(actualTypeArgument, Threadable.class);
        if (methods.isEmpty()) {
            throw new RuntimeException("the class " + actualTypeArgument.getName() + " does not have his running method annoted with @Threadable");
        } else if (methods.size() > 1) {
            throw new RuntimeException("the class " + actualTypeArgument.getName() + " must have only one method annoted with @Threadable");
        } else {
            this.run = methods.get(0);
        }
        this.threadable = threadable;
        this.identifiant = identifiant;
    }

    public void launch(Object... args) {
        this.args = args;
        this.start();
    }

    public void run() {
        setName(this.identifiant);
        try {
            this.result = (U) this.run.invoke(threadable, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            checkInterrupt(e);
        }

    }

    private void checkInterrupt(InvocationTargetException e) {
        Throwable t = e.getTargetException();

        while (t != null) {
            if (t.getMessage().equals("interrupted")) {
                this.isInterrupt = true;
            }
            t = t.getCause();
        }
        if (!this.isInterrupt) {
            throw new RuntimeException(e);
        }

    }

    public static List<Method> getMethodsAnnotatedWith(final Class<?> type, final Class<? extends Annotation> annotation) {
        final List<Method> methods = new ArrayList<Method>();
        Class<?> klass = type;
        while (klass != Object.class) { // need to traverse a type hierarchy in order to process methods from super types
            // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
            for (final Method method : klass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotation)) {
                    Annotation annotInstance = method.getAnnotation(annotation);
                    // TODO process annotInstance
                    methods.add(method);
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }
        return methods;
    }

    public U getResult() {
        if (this.isInterrupt) {
            throw new RuntimeException(INTERRUPTED);
        } else if (result == null) {
            throw new NullPointerException(NO_RESULT);
        }
        return result;
    }
}
