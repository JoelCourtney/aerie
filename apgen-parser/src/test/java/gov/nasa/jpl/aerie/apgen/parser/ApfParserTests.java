package gov.nasa.jpl.aerie.apgen.parser;

import gov.nasa.jpl.aerie.apgen.exceptions.APGenAdaptationParsingException;
import gov.nasa.jpl.aerie.apgen.exceptions.PlanParsingException;
import gov.nasa.jpl.aerie.apgen.model.ActivityInstance;
import gov.nasa.jpl.aerie.apgen.model.ActivityInstanceParameter;
import gov.nasa.jpl.aerie.apgen.model.Adaptation;
import gov.nasa.jpl.aerie.apgen.model.Plan;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class ApfParserTests {
    // TODO: Create more plans and tests

    @Test
    public void testParseSimplePlan() throws APGenAdaptationParsingException, PlanParsingException {
        Path adaptationPath = resourcePath("/gov/nasa/jpl/aerie/apgen/simple/simple_adaptation.aaf");
        Path planPath = resourcePath("/gov/nasa/jpl/aerie/apgen/simple/simple_plan.apf");
        Adaptation adaptation = APGenAdaptationParser.parseFile(adaptationPath);
        Plan plan = ApfParser.parseFile(planPath, adaptation);

        assertThat(plan.getActivityInstance("Apple_1")).isNotNull();
        assertThat(plan.getActivityInstance("Banana_1")).isNotNull();

        /* Verify Apple activity instance was loaded properly */
        ActivityInstance apple = plan.getActivityInstance("Apple_1");
        assertThat(apple.getType()).isEqualTo("Apple");
        assertThat(apple.getName()).isEqualTo("Apple1");

        assertThat(apple.getParameter("parameters")).isNotNull();
        ActivityInstanceParameter appleParameters = apple.getParameter("parameters");
        assertThat(appleParameters.getType()).isEqualTo("array");
        assertThat(appleParameters.getValue()).isEqualTo("[\"yellow\", 1.0, 3]");

        /* Verify Banana activity instance was loaded properly */
        ActivityInstance banana = plan.getActivityInstance("Banana_1");
        assertThat(banana.getType()).isEqualTo("Banana");
        assertThat(banana.getName()).isEqualTo("Banana1");

        assertThat(banana.getParameter("duration")).isNotNull();
        ActivityInstanceParameter bananaDuration = banana.getParameter("duration");
        assertThat(bananaDuration.getType()).isEqualTo("Duration");
        assertThat(bananaDuration.getValue()).isEqualTo("01:30:00");
    }

    @Test
    public void testParsePlanTooFewParameters() throws APGenAdaptationParsingException {
        Path adaptationPath = resourcePath("/gov/nasa/jpl/aerie/apgen/simple/simple_adaptation.aaf");
        Path planPath = resourcePath("/gov/nasa/jpl/aerie/apgen/simple/too_few_parameters.apf");
        Adaptation adaptation = APGenAdaptationParser.parseFile(adaptationPath);

        final Throwable thrown = catchThrowable(() -> ApfParser.parseFile(planPath, adaptation));

        assertThat(thrown).isInstanceOf(PlanParsingException.class);
        assertThat(thrown.getMessage()).contains("too few parameters");
    }

    @Test
    public void testParsePlanTooManyParameters() throws APGenAdaptationParsingException {
        Path adaptationPath = resourcePath("/gov/nasa/jpl/aerie/apgen/simple/simple_adaptation.aaf");
        Path planPath = resourcePath("/gov/nasa/jpl/aerie/apgen/simple/too_many_parameters.apf");
        Adaptation adaptation = APGenAdaptationParser.parseFile(adaptationPath);

        final Throwable thrown = catchThrowable(() -> ApfParser.parseFile(planPath, adaptation));

        assertThat(thrown).isInstanceOf(PlanParsingException.class);
        assertThat(thrown.getMessage()).contains("Too many parameters");
    }

    @Test
    public void testParsePlanIncomplete() throws APGenAdaptationParsingException {
        Path adaptationPath = resourcePath("/gov/nasa/jpl/aerie/apgen/simple/simple_adaptation.aaf");
        Path planPath = resourcePath("/gov/nasa/jpl/aerie/apgen/simple/incomplete.apf");
        Adaptation adaptation = APGenAdaptationParser.parseFile(adaptationPath);

        final Throwable thrown = catchThrowable(() -> ApfParser.parseFile(planPath, adaptation));

        assertThat(thrown).isInstanceOf(PlanParsingException.class);
        assertThat(thrown.getMessage()).contains("Unexpected end of file");
    }

    @Test
    public void testParsePlanUnexpectedActivityEnd() throws APGenAdaptationParsingException {
        Path adaptationPath = resourcePath("/gov/nasa/jpl/aerie/apgen/simple/simple_adaptation.aaf");
        Path planPath = resourcePath("/gov/nasa/jpl/aerie/apgen/simple/unexpected_activity_end.apf");
        Adaptation adaptation = APGenAdaptationParser.parseFile(adaptationPath);

        final Throwable thrown = catchThrowable(() -> ApfParser.parseFile(planPath, adaptation));

        assertThat(thrown).isInstanceOf(PlanParsingException.class);
        assertThat(thrown.getMessage()).contains("unexpected activity instance end");
    }

    private static Path resourcePath(final String path) {
        try {
            return Path.of(ApfParserTests.class.getResource(path).toURI());
        } catch (final URISyntaxException e) {
            throw new Error(e);
        }
    }
}
