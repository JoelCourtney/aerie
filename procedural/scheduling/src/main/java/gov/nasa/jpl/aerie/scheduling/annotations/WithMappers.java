package gov.nasa.jpl.aerie.scheduling.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PACKAGE)
@Repeatable(AllMappers.class)
public @interface WithMappers {
  Class<?> value();
}
