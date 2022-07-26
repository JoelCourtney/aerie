package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.mocks.StubMissionModelService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypescriptCodeGenerationServiceTest {

  @Test
  void testCodeGen() throws MissionModelService.NoSuchMissionModelException {
    final var codeGenService = new TypescriptCodeGenerationServiceAdapter(new StubMissionModelService());

    assertEquals(
        """
           /** Start Codegen */
           import * as AST from './constraints-ast.js';
           import { Discrete, Real, Windows } from './constraints-edsl-fluent-api.js';
           export enum ActivityType {
             activity = "activity",
           }
           export type ResourceName = "mode" | "state of charge" | "an integer";
           export type DiscreteResourceSchema<R extends ResourceName> =
             R extends "mode" ? ( | "Option1" | "Option2") :
             R extends "state of charge" ? {initial: number, rate: number, } :
             R extends "an integer" ? number :
             never;
           export type RealResourceName = "state of charge" | "an integer";
           export type ActivityParameters<A extends ActivityType> =
             A extends ActivityType.activity ? {Param: Discrete<string>, AnotherParam: Real, } :
             never;
           export class ActivityInstance<A extends ActivityType> {
             private readonly __activityType: A;
             private readonly __alias: string;
             constructor(activityType: A, alias: string) {
               this.__activityType = activityType;
               this.__alias = alias;
             }
             public get parameters(): ActivityParameters<A> {
               let result = (
                 this.__activityType === ActivityType.activity ? {Param: new Discrete<string>({ kind: AST.NodeKind.DiscreteProfileParameter, activityAlias: this.__alias, parameterName: "Param"}), AnotherParam: new Real({ kind: AST.NodeKind.RealProfileParameter, activityAlias: this.__alias, parameterName: "AnotherParam"}), } :
                 undefined) as ActivityParameters<A>;
               if (result === undefined) {
                 throw new TypeError("Unreachable state. Activity type was unexpected string in ActivityInstance.parameters(): " + this.__activityType);
               } else {
                 return result;
               }
             }
             /**
              * Produces a window for the duration of the activity.
              */
             public window(): Windows {
               return new Windows({
                 kind: AST.NodeKind.WindowsExpressionActivityWindow,
                 activityAlias: this.__alias
               });
             }
             /**
              * Produces an instantaneous window at the start of the activity.
              */
             public start(): Windows {
               return new Windows({
                 kind: AST.NodeKind.WindowsExpressionStartOf,
                 activityAlias: this.__alias
               });
             }
             /**
              * Produces an instantaneous window at the end of the activity.
              */
             public end(): Windows {
               return new Windows({
                 kind: AST.NodeKind.WindowsExpressionEndOf,
                 activityAlias: this.__alias
               });
             }
           }
           declare global {
             enum ActivityType {
               activity = "activity",
             }
           }
           Object.assign(globalThis, {
             ActivityType
           });
           /** End Codegen */""",
        codeGenService.generateTypescriptTypesFromMissionModel("abc")
    );
  }
}
