/**
 * OpenAtlasForAndroid Project
 * The MIT License (MIT) Copyright (OpenAtlasForAndroid) 2015 Bunny Blue,achellies
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author BunnyBlue
 **/
package com.openatlas.hack;

import com.openatlas.hack.Hack.HackDeclaration.HackAssertionException;
import com.openatlas.hack.Interception.InterceptionHandler;
import com.openatlas.runtime.DelegateClassLoader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Hack {
    private static AssertionFailureHandler sFailureHandler;

    public interface AssertionFailureHandler {
        boolean onAssertionFailure(HackAssertionException hackAssertionException);
    }

    public static abstract class HackDeclaration {

        public static class HackAssertionException extends Throwable {
            private static final long serialVersionUID = 1;
            private Class<?> mHackedClass;
            private String mHackedFieldName;
            private String mHackedMethodName;

            public HackAssertionException(String cause) {
                super(cause);
            }

            public HackAssertionException(Exception exception) {
                super(exception);
            }

            @Override
            public String toString() {
                return getCause() != null ? getClass().getName() + ": "
                        + getCause() : super.toString();
            }

            public Class<?> getHackedClass() {
                return this.mHackedClass;
            }

            public void setHackedClass(Class<?> cls) {
                this.mHackedClass = cls;
            }

            public String getHackedMethodName() {
                return this.mHackedMethodName;
            }

            public void setHackedMethodName(String name) {
                this.mHackedMethodName = name;
            }

            public String getHackedFieldName() {
                return this.mHackedFieldName;
            }

            public void setHackedFieldName(String name) {
                this.mHackedFieldName = name;
            }
        }
    }

    public static class HackedClass<C> {
        protected Class<C> mClass;

        public HackedField<C, Object> staticField(String name)
                throws HackAssertionException {
            return new HackedField(this.mClass, name, Modifier.STATIC);
        }

        public HackedField<C, Object> field(String name)
                throws HackAssertionException {
            return new HackedField(this.mClass, name, 0);
        }

        public HackedMethod staticMethod(String name, Class<?>... parameterTypes)
                throws HackAssertionException {
            return new HackedMethod(this.mClass, name, parameterTypes, Modifier.STATIC);
        }

        public HackedMethod method(String name, Class<?>... parameterTypes)
                throws HackAssertionException {
            return new HackedMethod(this.mClass, name, parameterTypes, 0);
        }

        public HackedConstructor constructor(Class<?>... parameterTypes)
                throws HackAssertionException {
            return new HackedConstructor(this.mClass, parameterTypes);
        }

        public HackedClass(Class<C> cls) {
            this.mClass = cls;
        }

        public Class<C> getmClass() {
            return this.mClass;
        }
    }

    public static class HackedConstructor {
        protected Constructor<?> mConstructor;

        HackedConstructor(Class<?> clazz, Class<?>[] parameterTypes) throws HackAssertionException {
            if (clazz != null) {
                try {
                    this.mConstructor = clazz.getDeclaredConstructor(parameterTypes);
                } catch (Exception e) {
                    HackAssertionException hackAssertionException = new HackAssertionException(e);
                    hackAssertionException.setHackedClass(clazz);
                    Hack.fail(hackAssertionException);
                }
            }
        }

        public Object getInstance(Object... args) throws IllegalArgumentException {

            this.mConstructor.setAccessible(true);
            try {
                return this.mConstructor.newInstance(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static class HackedField<C, T> {
        private final Field mField;

        public <T2> com.openatlas.hack.Hack.HackedField<C, T2> ofGenericType(Class<?> cls) throws HackAssertionException {
            if (!(this.mField == null || cls.isAssignableFrom(this.mField.getType()))) {
                Hack.fail(new HackAssertionException(new ClassCastException(
                        this.mField + " is not of type " + cls)));
            }
            return (HackedField<C, T2>) this;
        }

        public <T2> com.openatlas.hack.Hack.HackedField<C, T2> ofType(
                Class<T2> cls) throws HackAssertionException {
            if (!(this.mField == null || cls.isAssignableFrom(this.mField
                    .getType()))) {
                Hack.fail(new HackAssertionException(new ClassCastException(
                        this.mField + " is not of type " + cls)));
            }
            return (HackedField<C, T2>) this;
        }

        public com.openatlas.hack.Hack.HackedField<C, T> ofType(
                String className) throws HackAssertionException {
            com.openatlas.hack.Hack.HackedField<C, T> ofType = null;
            try {
                ofType = (HackedField<C, T>) ofType(Class.forName(className));
            } catch (Exception e) {
                Hack.fail(new HackAssertionException(e));
            }
            return ofType;
        }

        public T get(C object) {
            try {
                return (T) this.mField.get(object);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        }

        public void set(C object, Object value) {
            try {
                this.mField.set(object, value);
            } catch (Throwable e) {
                e.printStackTrace();
                if (value instanceof DelegateClassLoader) {
                    throw new RuntimeException("set DelegateClassLoader fail", e);
                }
            }
        }

        public void hijack(C c, InterceptionHandler<?> interceptionHandler) {
            Object obj = get(c);
            if (obj == null) {
                throw new IllegalStateException("Cannot hijack null");
            }
            set(c, Interception.proxy(obj,
                    interceptionHandler, obj.getClass()
                            .getInterfaces()));
        }

        HackedField(Class<C> cls, String name, int modifier) throws HackAssertionException {
            Field field = null;
            if (cls == null) {
                this.mField = null;
                return;
            }
            try {
                field = cls.getDeclaredField(name);
                if (modifier > 0 && (field.getModifiers() & modifier) != modifier) {
                    Hack.fail(new HackAssertionException(field
                            + " does not match modifiers: " + modifier));
                }
                field.setAccessible(true);
            } catch (Exception e) {
                HackAssertionException hackAssertionException = new HackAssertionException(e);
                hackAssertionException.setHackedClass(cls);
                hackAssertionException.setHackedFieldName(name);
                Hack.fail(hackAssertionException);
            } finally {
                this.mField = field;
            }
        }

        public Field getField() {
            return this.mField;
        }
    }

    public static class HackedMethod {
        protected final Method mMethod;

        /****
         * @param receiver the object on which to call this method (or null for static methods)
         * @param args     the arguments to the method
         * @return the result
         * @throws IllegalArgumentException
         * @throws InvocationTargetException
         */
        public Object invoke(Object receiver, Object... args) throws IllegalArgumentException, InvocationTargetException {
            try {
                return this.mMethod.invoke(receiver, args);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        }

        /***
         * @param name           the requested method's name.
         * @param parameterTypes the parameter types of the requested method.
         */
        HackedMethod(Class<?> cls, String name, Class<?>[] parameterTypes, int modifier) throws HackAssertionException {
            Method method = null;
            if (cls == null) {
                this.mMethod = null;
                return;
            }
            try {
                method = cls.getDeclaredMethod(name, parameterTypes);
                if (modifier > 0 && (method.getModifiers() & modifier) != modifier) {
                    Hack.fail(new HackAssertionException(method
                            + " does not match modifiers: " + modifier));
                }
                method.setAccessible(true);
            } catch (Exception e) {
                HackAssertionException hackAssertionException = new HackAssertionException(e);
                hackAssertionException.setHackedClass(cls);
                hackAssertionException.setHackedMethodName(name);
                Hack.fail(hackAssertionException);
            } finally {
                this.mMethod = method;
            }
        }

        public Method getMethod() {
            return this.mMethod;
        }
    }

    public static <T> HackedClass<T> into(Class<T> cls) {
        return new HackedClass(cls);
    }

    public static <T> HackedClass<T> into(String str)
            throws HackAssertionException {
        try {
            return new HackedClass(Class.forName(str));
        } catch (Exception e) {
            fail(new HackAssertionException(e));
            return new HackedClass(null);
        }
    }

    private static void fail(HackAssertionException hackAssertionException)
            throws HackAssertionException {
        if (sFailureHandler == null
                || !sFailureHandler.onAssertionFailure(hackAssertionException)) {
            throw hackAssertionException;
        }
    }

    public static void setAssertionFailureHandler(
            AssertionFailureHandler assertionFailureHandler) {
        sFailureHandler = assertionFailureHandler;
    }

    private Hack() {
    }
}
