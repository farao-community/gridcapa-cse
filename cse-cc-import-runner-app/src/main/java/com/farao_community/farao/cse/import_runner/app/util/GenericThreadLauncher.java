/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse.import_runner.app.util;

import com.farao_community.farao.cse.runner.api.exception.CseInternalException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.farao_community.farao.cse.runner.api.resource.ThreadLauncherResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericThreadLauncher<T, U> extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericThreadLauncher.class);
    private T threadable;
    private Method run;
    private Object[] args;

    private ThreadLauncherResult<U> result;

    public GenericThreadLauncher(T threadable, String id, Object... args) {
        super(id);
        this.run = getMethodAnnotatedWith(threadable.getClass());
        this.threadable = threadable;
        this.args = args;
    }

    @Override
    public void run() {
        try {
            U resultat = (U) this.run.invoke(threadable, args);
            this.result = new ThreadLauncherResult<>(Optional.ofNullable(resultat), false, null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("Error occurred during CSE run", e);
            this.result = new ThreadLauncherResult<>(Optional.ofNullable(null), true, e);
        }
    }

    public ThreadLauncherResult<U> getResult() {
        try {
            join();
        } catch (InterruptedException e) {
            interrupt();
        }
        return result;
    }

    private static Method getMethodAnnotatedWith(final Class<?> type) {
        List<Method> methods = getMethodsAnnotatedWith(type);
        if (methods.isEmpty()) {
            throw new CseInternalException("the class " + type.getCanonicalName() + " does not have his running method annoted with @Threadable");
        } else if (methods.size() > 1) {
            throw new CseInternalException("the class " + type.getCanonicalName() + " must have only one method annoted with @Threadable");
        } else {
            return methods.get(0);
        }
    }

    private static List<Method> getMethodsAnnotatedWith(final Class<?> type) {
        final List<Method> methods = new ArrayList<>();
        Class<?> klass = type;
        while (klass != Object.class) { // need to traverse a type hierarchy in order to process methods from super types
            // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
            for (final Method method : klass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Threadable.class)) {
                    methods.add(method);
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }
        return methods;
    }
}
