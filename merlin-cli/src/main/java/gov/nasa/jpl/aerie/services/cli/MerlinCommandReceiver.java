package gov.nasa.jpl.aerie.services.cli;

import gov.nasa.jpl.aerie.services.cli.models.Adaptation;

import java.nio.file.Path;

/**
 * A receiver for Merlin commands.
 *
 * A Merlin command is a command regarding plans, mission models, and simulation thereof.
 */
public interface MerlinCommandReceiver {
    void createPlan(String path);
    void updatePlanFromFile(String planId, String path);
    void updatePlanFromTokens(String planId, String[] tokens);
    void deletePlan(String planId);
    void downloadPlan(String planId, String outName);
    void appendActivityInstances(String planId, String path);
    void displayActivityInstance(String planId, String activityId);
    void updateActivityInstance(String planId, String activityId, String[] tokens);
    void deleteActivityInstance(String planId, String activityId);
    void listPlans();
    String createAdaptation(Path path, Adaptation adaptation);
    void deleteAdaptation(String adaptationId);
    void displayAdaptation(String adaptationId);
    void listAdaptations();
    void listActivityTypes(String adaptationId);
    void displayActivityType(String adaptationId, String activityType);
    void convertApfFile(String input, String output, String dir, String[] tokens);
    void performSimulation(String planId, long samplingPeriod, String outName);
}
