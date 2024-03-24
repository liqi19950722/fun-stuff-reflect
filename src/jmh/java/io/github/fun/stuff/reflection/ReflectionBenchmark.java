package io.github.fun.stuff.reflection;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Warmup(
        iterations = 5,
        time = 1,
        timeUnit = TimeUnit.SECONDS
)
@Measurement(
        iterations = 20,
        time = 1,
        timeUnit = TimeUnit.SECONDS
)
@Fork(3)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(value = Mode.AverageTime)
@State(Scope.Thread)
public class ReflectionBenchmark {

    Person person;

    Method personGetMethod;

    MethodHandle personGetNameMethodHandle;

    static MethodHandle personGetNameStaticMethodHandle;
    static final MethodHandle personGetNameStaticFinalMethodHandle;

    static Map<String, MethodHandle> staticMethodHandleMap;
    static final Map<String, MethodHandle> staticFinalMethodHandleMap;

    static MethodHandleRecord staticMethodHandleRecord;
    static final MethodHandleRecord staticFinalMethodHandleRecord;

    MethodHandleRecordFunction methodHandleRecordFunction;
    Function<Person, String> personGetNameFunction;

    static {
        var lookup = MethodHandles.lookup();
        try {
            personGetNameStaticMethodHandle = lookup.findVirtual(Person.class, "getName", MethodType.methodType(String.class));
            personGetNameStaticFinalMethodHandle = personGetNameStaticMethodHandle;
            staticFinalMethodHandleMap = Map.of("getName", personGetNameStaticMethodHandle);
            staticMethodHandleMap = Map.of("getName", personGetNameStaticMethodHandle);
            staticMethodHandleRecord = new MethodHandleRecord(personGetNameStaticMethodHandle);
            staticFinalMethodHandleRecord = new MethodHandleRecord(personGetNameStaticMethodHandle);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    @Setup
    public void setup() throws Throwable {
        person = new Person("John");
        personGetMethod = Person.class.getMethod("getName");
        var lookup = MethodHandles.lookup();
        personGetNameMethodHandle = lookup.findVirtual(Person.class, "getName", MethodType.methodType(String.class));
        methodHandleRecordFunction = new MethodHandleRecordFunction(personGetNameMethodHandle);
        var callsite = LambdaMetafactory.metafactory(lookup, "apply",
                MethodType.methodType(Function.class),
                MethodType.methodType(Object.class, Object.class),
                lookup.findVirtual(Person.class, "getName", MethodType.methodType(String.class)),
                MethodType.methodType(String.class, Person.class));
        personGetNameFunction = (Function<Person, String>) callsite.getTarget().invokeExact();
    }


    @Benchmark
    public String _100_direct() {
        return person.getName();
    }

    @Benchmark
    public String _200_reflection() throws InvocationTargetException, IllegalAccessException {
        return (String) personGetMethod.invoke(person);
    }

    @Benchmark
    public String _300_methodHandle() throws Throwable {
        return (String) personGetNameMethodHandle.invokeExact(person);
    }

    @Benchmark
    public String _301_staticMethodHandle() throws Throwable {
        return (String) personGetNameStaticMethodHandle.invokeExact(person);
    }

    @Benchmark
    public String _302_staticFinalMethodHandle() throws Throwable {
        return (String) personGetNameStaticFinalMethodHandle.invokeExact(person);
    }

    @Benchmark
    public String _303_methodHandleInStaticFinalMap() throws Throwable {
        return (String) staticFinalMethodHandleMap.get("getName").invokeExact(person);
    }

    @Benchmark
    public String _304_methodHandleInStaticMap() throws Throwable {
        return (String) staticMethodHandleMap.get("getName").invokeExact(person);
    }

    @Benchmark
    public String _305_methodHandleInRecordClass() throws Throwable {
        return (String) staticMethodHandleRecord.methodHandle().invokeExact(person);
    }

    @Benchmark
    public String _306_methodHandleInStaticFinalRecordClass() throws Throwable {
        return (String) staticFinalMethodHandleRecord.methodHandle().invokeExact(person);
    }

    @Benchmark
    public String _307_methodHandleInRecordClassFunction() throws Throwable {
        return (String) methodHandleRecordFunction.apply(person);
    }

    @Benchmark
    public String _400_lambdaMetafactory() throws Throwable {
        return personGetNameFunction.apply(person);
    }

    record MethodHandleRecord(MethodHandle methodHandle) {
    }

    record MethodHandleRecordFunction(MethodHandle methodHandle) implements Function<Person, String> {

        @Override
        public String apply(Person p) {
            try {
                return (String) methodHandle.invokeExact(p);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
