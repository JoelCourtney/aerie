/**
 * Definitions for constructing time-varying resources.
 *
 * <p>
 *   A <b>mission resource</b> is an entity whose details may vary over the course of the mission,
 *   and which influences the success of the mission in some way.
 * </p>
 *
 * <p>
 *   At any given time, a resource is in a given <b>state</b>, and its time-varying behavior -- its <b>dynamics</b> --
 *   depends only upon that state. The {@link gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver} interface
 *   defines this relationship between state and dynamics.
 * </p>
 *
 * <p>
 *   Multiple resources may be determined by the same shared state.
 *   Such resources are defined by a <b>model</b> which determines the dynamics of each of the coupled resources.
 *   Thus, a <i>resource</i> is defined by a <i>model</i>.
 *   (We include singlet resources -- resources whose state is not shared by others -- as a limiting case.)
 * </p>
 *
 * <p>
 *   Pragmatically, a dynamics at one time may not be a reasonable approximation at a later time. A resource, then,
 *   also gives the current extent of validity for its current dynamics.
 *   The {@link gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics} class describes this mechanism.
 * </p>
 *
 * <p>
 *   We may analyze a resource's dynamics in one of two ways:
 * </p>
 * <ul>
 *   <li>
 *     Determining the value of a resource at a given time, under its current dynamics; or
 *   <li>
 *     Determining the next time at which some condition is held true.
 * </ul>
 * <p>
 *   The {@link gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver} interface captures these capabilities
 *   for a given choice of types for resource values, dynamics, and conditions.
 * </p>
 */
package gov.nasa.jpl.aerie.merlin.framework.resources;
